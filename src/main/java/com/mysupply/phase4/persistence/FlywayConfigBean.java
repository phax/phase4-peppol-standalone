package com.mysupply.phase4.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FlywayConfigBean {
    @Autowired
    private Environment env;

//    @Autowired
//    private ApplicationArguments args;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayConfigBean.class);
    private static final String baseConfigurationPath = "peppol.documents.jdbc.";

    @Bean
    public Flyway flyway() {
        String dbUrl = this.env.getRequiredProperty(baseConfigurationPath + "url");
        String dbUsername = this.env.getRequiredProperty(baseConfigurationPath + "username");
        String dbPassword = this.env.getRequiredProperty(baseConfigurationPath + "password");
        String locations = this.env.getRequiredProperty(baseConfigurationPath + "locations");
        String driver = this.env.getRequiredProperty(baseConfigurationPath + "driver");

        FluentConfiguration flywayConfiguration = Flyway.configure();
        flywayConfiguration
                .dataSource(dbUrl, dbUsername, dbPassword)
                .driver(driver)
                .locations(locations)
                .schemas(DocumentConstants.DOCUMENT_SCHEMA_NAME)
                .createSchemas(true); //we have to create the custom schema or the DocumentRepository entities will break

        Flyway flyway = flywayConfiguration.load();
                this.printMigrationInfo(flyway);
                flyway.migrate();
                LOGGER.info("Migrations applied (if any), exiting application.");

        return flyway;
    }

    private void printMigrationInfo(Flyway flyway) {
        MigrationInfoService migrationInfoService = flyway.info();
        MigrationInfo[] migrationInfos = migrationInfoService.pending();
        LOGGER.info("Pending migrations: {}.", migrationInfos.length);
        for (MigrationInfo migrationInfo : migrationInfos) {
            LOGGER.info("Scheduling Version: {} Description: {}.", migrationInfo.getVersion(), migrationInfo.getDescription());
        }
    }
}
