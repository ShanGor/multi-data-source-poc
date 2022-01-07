package com.example.demo;

import com.example.demo.entity.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

@Slf4j
public class SimpleTests {
    @Test
    public void test() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class clazz = AppUser.class;
//
//        BatchInsertService.PrepareSqlForBatchInserts prepareSqlForBatchInserts = BatchInsertService.prepareSqlForBatchInserts(clazz);
//        log.info(prepareSqlForBatchInserts.toString());
    }


}
