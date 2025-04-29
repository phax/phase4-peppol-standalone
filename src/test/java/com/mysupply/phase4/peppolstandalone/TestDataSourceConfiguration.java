package com.mysupply.phase4.peppolstandalone;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TestDataSourceConfiguration {

    @Bean
    public DataSourceProperties testDataSourceProperties(Environment environment) {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(environment.getProperty("spring.datasource.url"));
        properties.setUsername(environment.getProperty("spring.datasource.username"));
        properties.setPassword(environment.getProperty("spring.datasource.password"));
        properties.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name")); // Optional, but good practice
        return properties;
    }
}