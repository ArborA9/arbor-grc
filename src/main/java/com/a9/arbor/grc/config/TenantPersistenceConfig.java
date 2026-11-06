package com.a9.arbor.grc.config;

import com.a9.arbor.grc.tenancy.NoTenantDataSource;
import com.a9.arbor.grc.tenancy.TenantRoutingDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistence unit for tenant-scoped modules. Backed by the {@link TenantRoutingDataSource}, which
 * selects the correct per-tenant database from the current {@code TenantContext}. Marked
 * {@code @Primary} so feature modules inject this unit by default.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.a9.arbor.grc.modules",
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager")
public class TenantPersistenceConfig {

    @Value("${arbor.jpa.dialect}")
    private String hibernateDialect;

    @Bean
    public TenantRoutingDataSource tenantRoutingDataSource() {
        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        routing.setTargetDataSources(new HashMap<>());
        routing.setDefaultTargetDataSource(new NoTenantDataSource());
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            TenantRoutingDataSource tenantRoutingDataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(tenantRoutingDataSource);
        emf.setPackagesToScan("com.a9.arbor.grc.modules");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setPersistenceUnitName("tenant");
        emf.setJpaPropertyMap(jpaProperties());
        return emf;
    }

    @Bean
    @Primary
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    private Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", hibernateDialect);
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put("hibernate.format_sql", true);
        // Match Flyway snake_case columns (created_at, module_code, …). Without this, the custom
        // EMF leaves camelCase physical names and tenant queries 500 at runtime.
        props.put(
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        // The routing datasource has no connection at boot (no tenant yet); the explicit dialect
        // above lets Hibernate build the EMF without probing JDBC metadata.
        props.put("hibernate.boot.allow_jdbc_metadata_access", false);
        return props;
    }
}
