package com.mysupply.phase4.persistence.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringDatabaseConfiguration {

    private final PeppolDocumentsJdbcConfiguration peppolDocumentsJdbcConfiguration;

    @Autowired
    public SpringDatabaseConfiguration(PeppolDocumentsJdbcConfiguration peppolJdbcProperties) {
        this.peppolDocumentsJdbcConfiguration = peppolJdbcProperties;
    }

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl(peppolDocumentsJdbcConfiguration.getUrl());
        dataSourceProperties.setUsername(peppolDocumentsJdbcConfiguration.getUsername());
        dataSourceProperties.setPassword(peppolDocumentsJdbcConfiguration.getPassword());
        dataSourceProperties.setDriverClassName(peppolDocumentsJdbcConfiguration.getDriver());
        return dataSourceProperties;
    }
}