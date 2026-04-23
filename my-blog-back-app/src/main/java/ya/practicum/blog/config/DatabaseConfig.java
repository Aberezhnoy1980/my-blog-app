package ya.practicum.blog.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ya.practicum.blog.db.migration.V4__Normalize_tags;

@Configuration
public class DatabaseConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> configuration
                .locations("classpath:db/migration")
                .javaMigrations(new V4__Normalize_tags())
                .baselineOnMigrate(true);
    }
}
