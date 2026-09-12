package com.lexpilot.common.config;

import com.lexpilot.common.tenant.TenantInterceptor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(TenantInterceptor tenantInterceptor) {
        return (Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put("hibernate.session_factory.statement_inspector", tenantInterceptor);
        };
    }
}
