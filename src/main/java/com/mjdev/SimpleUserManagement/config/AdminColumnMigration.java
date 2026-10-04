package com.mjdev.SimpleUserManagement.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * CREATE TABLE IF NOT EXISTS never alters an existing table, so the `admin`
 * column is added here for databases created before it existed.
 */
@Component
public class AdminColumnMigration implements ApplicationRunner {

    private final JdbcClient jdbc;

    public AdminColumnMigration(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.sql("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'users'
                  AND COLUMN_NAME = 'admin'
                """)
                .query(Integer.class)
                .single();
        if (count == 0) {
            jdbc.sql("ALTER TABLE users ADD COLUMN `admin` TINYINT(1) NOT NULL DEFAULT 0 AFTER enabled")
                    .update();
        }
    }
}
