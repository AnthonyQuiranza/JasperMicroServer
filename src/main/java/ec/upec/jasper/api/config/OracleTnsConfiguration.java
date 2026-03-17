package ec.upec.jasper.api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OracleTnsConfiguration {

    @Value("${ORACLE_NET_TNS_ADMIN:${TNS_ADMIN:}}")
    private String tnsAdmin;

    @PostConstruct
    void configureTnsAdmin() {
        if (StringUtils.hasText(tnsAdmin)) {
            System.setProperty("oracle.net.tns_admin", tnsAdmin);
            System.setProperty("TNS_ADMIN", tnsAdmin);
        }
    }
}
