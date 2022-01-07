package com.example.demo.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.*;

import static com.example.demo.configuration.DataSourceConfig.Entity.*;

@Configuration
public class DataSourceConfig {
    public static final Set<String> supportedEntities = new HashSet<>();
    public static class Entity {
        public static final String E1 = "E1";
        public static final String E2 = "E2";
        public static final String E3 = "E3";
    }

    static {
        supportedEntities.add(E1);
        supportedEntities.add(E2);
        supportedEntities.add(E3);
    }

    @Primary
    @Bean
    public DataSource dataSourceForAll() {
        AbstractRoutingDataSource dataSource = new DatasourceAwareRoutingSource();
        Map<Object,Object> targetDataSources = new HashMap<>();
        targetDataSources.put(E1, dataSourceForE1());
        targetDataSources.put(E2, dataSourceForE2());
        targetDataSources.put(E3, dataSourceForE3());

        dataSource.setTargetDataSources(targetDataSources);
        /**
         * You have to specify a default entity, otherwise the program cannot boot.
         */
        dataSource.setDefaultTargetDataSource(targetDataSources.get(E1));
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    @Bean(E1)
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource dataSourceForE1() {
        return DataSourceBuilder.create().build();
    }

    @Bean(E2)
    @ConfigurationProperties(prefix="spring.datasource-e2")
    public DataSource dataSourceForE2() {
        return DataSourceBuilder.create().build();
    }

    @Bean(E3)
    @ConfigurationProperties(prefix="spring.datasource-e3")
    public DataSource dataSourceForE3() {
        return DataSourceBuilder.create().build();
    }
}
