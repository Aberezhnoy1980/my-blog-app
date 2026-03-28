package ya.practicum.blog.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import ya.practicum.blog.db.migration.V4__Normalize_tags;

@Configuration
@EnableTransactionManagement
@PropertySources({
        @PropertySource("classpath:application.properties"),
        @PropertySource(value = "classpath:application-${spring.profiles.active:dev}.properties")
})
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getRequiredProperty("db.driver"));
        dataSource.setUrl(environment.getRequiredProperty("db.url"));
        dataSource.setUsername(environment.getRequiredProperty("db.username"));
        dataSource.setPassword(environment.getRequiredProperty("db.password"));
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .javaMigrations(new V4__Normalize_tags())
                .load();
    }
}
