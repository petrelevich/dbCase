package ru.usermng.dao;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgreSQLContainerShared extends PostgreSQLContainer<PostgreSQLContainerShared> {
    private static final String IMAGE_VERSION = "postgres:12";
    private static PostgreSQLContainerShared container;

    private PostgreSQLContainerShared() {
        super(IMAGE_VERSION);
    }

    public static PostgreSQLContainerShared getInstance() {
        if (container == null) {
            container = new PostgreSQLContainerShared();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        var flyway = Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .locations("classpath:/db/migration")
                .load();
        flyway.migrate();
    }

    @Override
    public void stop() {
    }
}