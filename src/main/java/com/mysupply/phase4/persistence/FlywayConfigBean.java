package com.mysupply.phase4.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
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

    @Autowired
    private ApplicationArguments args;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayConfigBean.class);

    @Bean
    public Flyway flyway() {
        String dbUrl = this.env.getProperty("spring.datasource.url");
        String dbUsername = this.env.getProperty("spring.datasource.username");
        String password = this.env.getProperty("spring.datasource.password");
        String locations = this.env.getProperty("spring.flyway.locations");

        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, dbUsername, password)
                .locations(locations)
                .load();

        String[] args = this.args.getSourceArgs();
        for(String arg : args) {
            if(arg.equalsIgnoreCase("migrate")) {
                this.printMigrationInfo(flyway);
                flyway.migrate();
                LOGGER.info("Migrations applied (if any), exiting application.");
                System.exit(0);
            }
        }

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
