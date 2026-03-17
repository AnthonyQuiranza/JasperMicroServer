package ec.upec.jasper.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jasper")
public class JasperApiProperties {

    private String reportsPath = "classpath:/reports/";
    private boolean compileOnStartup = false;

    public String getReportsPath() {
        return reportsPath;
    }

    public void setReportsPath(String reportsPath) {
        this.reportsPath = reportsPath;
    }

    public boolean isCompileOnStartup() {
        return compileOnStartup;
    }

    public void setCompileOnStartup(boolean compileOnStartup) {
        this.compileOnStartup = compileOnStartup;
    }
}
