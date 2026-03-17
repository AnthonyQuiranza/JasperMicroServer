package ec.upec.jasper.api;

import ec.upec.jasper.api.config.JasperApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JasperApiProperties.class)
public class JasperReportApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JasperReportApiApplication.class, args);
    }
}
