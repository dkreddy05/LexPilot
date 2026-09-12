package com.lexpilot.common.tenant;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Intercepts SQL statements executed by Hibernate to inject the current
 * tenant ID into the PostgreSQL connection parameters.
 * <p>
 * This allows PostgreSQL Row-Level Security (RLS) policies to filter rows
 * automatically based on the `app.current_tenant` session variable.
 */
@Component
public class TenantInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        UUID tenantId = TenantContext.getTenantId();
        
        if (tenantId != null) {
            // Set the tenant ID for the current transaction/statement
            // Note: In a real high-throughput system, setting this per-statement
            // is expensive. A better approach is setting it once per connection
            // acquisition in the connection pool or using a custom Hibernate Aspect.
            // For LexPilot's current scale, prepending the SET command works reliably.
            
            // We only prepend if it's a DML statement (SELECT, INSERT, UPDATE, DELETE)
            String upperSql = sql.trim().toUpperCase();
            if (upperSql.startsWith("SELECT") || upperSql.startsWith("INSERT") || 
                upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")) {
                
                return "SET LOCAL app.current_tenant = '" + tenantId.toString() + "'; " + sql;
            }
        }
        
        return sql;
    }
}
