package com.a9.arbor.grc.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence unit for the shared <b>catalog</b> database (tenant registry). Uses a fixed datasource
 * and is migrated once at startup.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.a9.arbor.grc.catalog.repo",
        entityManagerFactoryRef = "catalogEntityManagerFactory",
        transactionManagerRef = "catalogTransactionManager")
public class CatalogPersistenceConfig {

    @Value("${arbor.jpa.dialect}")
    private String hibernateDialect;

    @Bean
    public DataSource catalogDataSource(ArborTenancyProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.catalogJdbcUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setPoolName("catalog-pool");
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    @Bean
    public CatalogDatabaseInitializer catalogDatabaseInitializer(ArborTenancyProperties properties) {
        return new CatalogDatabaseInitializer(properties);
    }

    @Bean(initMethod = "migrate")
    public Flyway catalogFlyway(DataSource catalogDataSource,
                                ArborTenancyProperties properties,
                                @SuppressWarnings("unused") CatalogDatabaseInitializer initializer) {
        return Flyway.configure()
                .dataSource(catalogDataSource)
                .locations("classpath:db/migration/catalog/" + properties.getVendor())
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean catalogEntityManagerFactory(
            DataSource catalogDataSource,
            @SuppressWarnings("unused") Flyway catalogFlyway) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(catalogDataSource);
        emf.setPackagesToScan("com.a9.arbor.grc.catalog.domain");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setPersistenceUnitName("catalog");
        emf.setJpaPropertyMap(jpaProperties());
        return emf;
    }

    @Bean
    public PlatformTransactionManager catalogTransactionManager(
            @Qualifier("catalogEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", hibernateDialect);
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.format_sql", true);
        props.put(
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        props.put("hibernate.boot.allow_jdbc_metadata_access", false);
        return props;
    }
}
