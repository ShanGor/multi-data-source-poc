package com.example.demo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * This class is the magic to handle multiple data sources in flight.
 *
 * @author Samuel Chan at 10 Dec 2021
 */
@Slf4j
public class DatasourceAwareRoutingSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        log.info("AbstractRoutingDataSource: {}", ThreadLocalStorage.getDBName());
        return ThreadLocalStorage.getDBName();
    }
}
