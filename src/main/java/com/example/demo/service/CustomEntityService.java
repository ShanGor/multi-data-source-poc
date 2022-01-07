package com.example.demo.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is some customized utilities to help to improve the performance for database operation. For example..
 * Spring Data JPA, the saveAll works for below prerequisites:
 *   - Your Entity has no @GeneratedValue for @Id column, and the value is assigned in the list for saveAll(List<Entity>).
 *     Otherwise, it will save one by one because it needs to ensure the id is generated as unique.
 *   - application.properties: spring.jpa.properties.hibernate.batch_size is configured
 *   - Your Entity should have implemented the `Persistable`, and the `isNew()` always return true, otherwise it will call a lot of `select to check duplicate`
 * To solve the pain for saveAll, this service provided a "batchInsertWithoutId(List<T>)" to save in a bulk.
 * For PostgreSQL, please add `?reWriteBatchedInserts=true` to the jdbc url. Which might save half of the time per tested result.
 * For MySQL, `rewriteBatchedStatements=true`
 * For Oracle, no need to rewrite.
 *
 * @author Samuel Chan
 * at 7th-Jan-2021
 */
@Slf4j
@Service
public class CustomEntityService {

    @Value("${spring.jpa.properties.hibernate.batch_size:500}")
    private int batchSize;

    private static final HashMap<Class, ObjectRelationMapInfo> ormInfoWithoutId = new HashMap<>();
    private static final ConcurrentHashMap<Class, Object> converters = new ConcurrentHashMap<>();

    @PersistenceContext
    EntityManager em;

    @PostConstruct
    public void init() {
        log.info("Listing the ORM tables..");
        MetadataExtractorIntegrator.INSTANCE.getMetadata().getEntityBindings().stream().forEach(clazz -> {
            ObjectRelationMapInfo objectRelationMapInfo = prepareSqlForBatchInsertsWithoutId(clazz);
            ormInfoWithoutId.put(clazz.getMappedClass(), objectRelationMapInfo);
        });
    }

    public static class MetadataExtractorIntegrator
            implements org.hibernate.integrator.spi.Integrator {

        public static final MetadataExtractorIntegrator INSTANCE =
                new MetadataExtractorIntegrator();

        private Metadata metadata;

        public Metadata getMetadata() {
            return metadata;
        }

        @Override
        public void integrate(
                Metadata metadata,
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

            this.metadata = metadata;

        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }

    /**
     * You need to add the @Transactional otherwise you cannot get the entityManager.
     * This is for Batch Insert Without ID specified, which means the database will generate the ID column.
     * Spring Data JPA, the saveAll works for below prerequisites:
     *   - Your Entity has no @GeneratedValue for @Id column, and the value is assigned in the list for saveAll(List<Entity>).
     *     Otherwise, it will save one by one because it needs to ensure the id is generated as unique.
     *   - application.properties: spring.jpa.properties.hibernate.batch_size is configured
     *   - Your Entity should have implemented the `Persistable`, and the `isNew()` always return true, otherwise it will call a lot of `select to check duplicate`
     * To solve the pain for saveAll, this service provided a "batchInsertWithoutId(List<T>)" to save in a bulk.
     *    For PostgreSQL, please add `?reWriteBatchedInserts=true` to the jdbc url. Which might save half of the time per tested result.
     *    For MySQL, `rewriteBatchedStatements=true`
     *    For Oracle, no need to rewrite.
     * @param objects
     */
    @Transactional
    public <T> void batchInsertWithoutId(List<T> objects) {
        if (objects==null || objects.isEmpty()) {
            return;
        }
        Class clazz = objects.get(0).getClass();

        Session session = em.unwrap(Session.class);
        SessionFactoryImpl mapping = (SessionFactoryImpl)session.getSessionFactory();
        session.doWork(connection -> {
            try {
                ObjectRelationMapInfo objectRelationMapInfo = ormInfoWithoutId.get(clazz);
                try(PreparedStatement stmt = connection.prepareStatement(objectRelationMapInfo.toBatchInsertString())) {
                    AtomicInteger pending = new AtomicInteger(0);
                    for (T obj: objects) {
                        for(ObjectRelationMapInfo.FieldMap fieldMap : objectRelationMapInfo.getFields()){
                            Field field = clazz.getDeclaredField(fieldMap.getNameInJava());
                            statementSetValue(stmt, field, obj, fieldMap, mapping);
                        }

                        stmt.addBatch();
                        if (pending.incrementAndGet() >= batchSize) {
                            stmt.executeBatch();
                            pending.set(0);
                        }
                    }

                    if (pending.get() > 0) {
                        stmt.executeBatch();
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void statementSetValue(PreparedStatement stmt,
                                  Field field,
                                  Object obj,
                                  ObjectRelationMapInfo.FieldMap fieldMap,
                                  SessionFactoryImpl sessionFactory) throws IllegalAccessException, SQLException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        field.setAccessible(true);
        Object value = field.get(obj);
        int columnIndex = fieldMap.getColumnIndex();
        Column column = fieldMap.getColumn();
        if (value == null) {
            stmt.setNull(columnIndex, column.getValue().getType().sqlTypes(sessionFactory)[0]);
            return;
        }

        /**
         * Handler converters, usually it is for DateTime, especially timestamp.
         */
        Annotation convert = field.getAnnotation(Convert.class);
        if (convert!=null) {
            Class converterClass = (Class) Convert.class.getDeclaredMethod("converter").invoke(convert);
            if(converterClass != void.class) {
                converters.putIfAbsent(converterClass, converterClass.newInstance());
                Object converter = converters.get(converterClass);
                if (!(converter instanceof AttributeConverter)) {
                    throw new RuntimeException("Your converter " + converterClass + " is not an implementation of AttributeConverter!");
                }
                Method convertMethod = converterClass.getDeclaredMethod("convertToDatabaseColumn", field.getType());
                Class returnClass = convertMethod.getReturnType();
                if (returnClass == Timestamp.class) {
                    Timestamp ts = (Timestamp) convertMethod.invoke(converter, value);
                    stmt.setTimestamp(columnIndex, ts);
                    return;
                }
            }
        }

        Class clazz = field.getType();
        if (clazz == String.class) {
            stmt.setString(columnIndex, value.toString());
        } else if (clazz == Long.class) {
            stmt.setLong(columnIndex, (Long) value);
        } else if (clazz == Integer.class) {
            stmt.setInt(columnIndex, (Integer) value);
        } else if (clazz == Double.class) {
            stmt.setDouble(columnIndex, (Double) value);
        } else {
            stmt.setObject(columnIndex, field.get(obj));
        }
    }

    public static final ObjectRelationMapInfo prepareSqlForBatchInsertsWithoutId(final PersistentClass clazz) {
        ObjectRelationMapInfo objectRelationMapInfo = new ObjectRelationMapInfo();
        org.hibernate.mapping.Table table = clazz.getTable();
        objectRelationMapInfo.setTableName(table.getName());
        AtomicInteger inx = new AtomicInteger(1);
        clazz.getPropertyIterator().forEachRemaining(prop -> {
            org.hibernate.mapping.Property p = (Property)prop;
            String nameInJava = p.getName();
            Column nameInTable = (org.hibernate.mapping.Column)(p.getColumnIterator().next());

            log.info("  {} - {}", nameInJava, nameInTable.getName());
            objectRelationMapInfo.addField(nameInJava, nameInTable, inx.getAndIncrement());
        });
        return objectRelationMapInfo;
    }

    @Data
    public static class ObjectRelationMapInfo {
        private String tableName;
        private java.util.List<FieldMap> fields;
        public ObjectRelationMapInfo() {
            fields = new LinkedList<>();
        }

        public ObjectRelationMapInfo addField(String nameInJava, Column nameInTable, int columnIndex) {
            this.fields.add(new FieldMap(nameInJava, nameInTable, columnIndex));
            return this;
        }
        @Data
        public static class FieldMap {
            private String nameInJava;
            private Column column;
            private int columnIndex;
            public FieldMap(String nameInJava, Column column, int columnIndex) {
                setNameInJava(nameInJava);
                setColumn(column);
                setColumnIndex(columnIndex);
            }
        }

        public String toBatchInsertString() {
            if (fields.size() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder("INSERT INTO ");
            sb.append(this.tableName).append('(');
            boolean first = true;
            for(FieldMap field: fields) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(field.column.getName());
            }
            sb.append(") VALUES(");
            first = true;
            for(int i=0; i<fields.size(); i++) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append('?');
            }
            sb.append(')');

            return sb.toString();
        }
    }

}
