package ec.upec.jasper.api.service;

import ec.upec.jasper.api.config.JasperApiProperties;
import ec.upec.jasper.api.dto.ReportRequest;
import ec.upec.jasper.api.exception.ReportGenerationException;
import ec.upec.jasper.api.exception.ReportTemplateNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.util.JRLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationService.class);
    private static final String REPORT_LOGO_PARAMETER = "P_LOGO_PATH";
    private static final String DEFAULT_LOGO_CLASSPATH = "classpath:/images/Upec_s.png";
    private static final String JASPER_JAVA_COMPILER_PROPERTY = "net.sf.jasperreports.compiler.java";
    private static final String JASPER_JDT_COMPILER_CLASS = "net.sf.jasperreports.jdt.JRJdtCompiler";

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final JasperApiProperties properties;

    public ReportGenerationService(DataSource dataSource, ResourceLoader resourceLoader, JasperApiProperties properties) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
        configureJasperCompiler();
    }

    public byte[] generatePdf(String reportName, ReportRequest request) {
        Map<String, Object> reportParams = new HashMap<>();
        reportParams.putAll(request.parameters());
        setDefaultAssets(reportParams);

        try {
            JasperReport report = resolveReport(reportName);

            if (request.useDatabase()) {
                try (Connection connection = dataSource.getConnection()) {
                    JasperPrint print = JasperFillManager.fillReport(report, reportParams, connection);
                    return JasperExportManager.exportReportToPdf(print);
                }
            }

            JasperPrint print = JasperFillManager.fillReport(report, reportParams, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException ex) {
            log.error("Error Jasper al generar el reporte '{}' con parametros: {}", reportName, reportParams, ex);
            throw new ReportGenerationException("Error generando el reporte '%s'".formatted(reportName), ex);
        } catch (Exception ex) {
            log.error("Error inesperado al generar el reporte '{}' con parametros: {}", reportName, reportParams, ex);
            throw new ReportGenerationException("Error inesperado al generar el reporte '%s'".formatted(reportName), ex);
        }
    }

    private JasperReport resolveReport(String reportName) {
        Resource jasperResource = resourceLoader.getResource(properties.getReportsPath() + reportName + ".jasper");
        if (jasperResource.exists()) {
            try (InputStream in = jasperResource.getInputStream()) {
                return (JasperReport) JRLoader.loadObject(in);
            } catch (Exception ex) {
                throw new ReportGenerationException("No se pudo cargar el archivo .jasper para '%s'".formatted(reportName), ex);
            }
        }

        Resource jrxmlResource = resourceLoader.getResource(properties.getReportsPath() + reportName + ".jrxml");
        if (jrxmlResource.exists()) {
            try (InputStream in = jrxmlResource.getInputStream()) {
                return JasperCompileManager.compileReport(in);
            } catch (Exception ex) {
                throw new ReportGenerationException("No se pudo compilar el archivo .jrxml para '%s'".formatted(reportName), ex);
            }
        }

        throw new ReportTemplateNotFoundException("No existe plantilla para reporte '%s'".formatted(reportName));
    }

    private void setDefaultAssets(Map<String, Object> reportParams) {
        if (reportParams.containsKey(REPORT_LOGO_PARAMETER)) {
            return;
        }

        Resource logoResource = resourceLoader.getResource(DEFAULT_LOGO_CLASSPATH);
        if (!logoResource.exists()) {
            log.warn("No se encontró el logo por defecto en {}", DEFAULT_LOGO_CLASSPATH);
            return;
        }

        try {
            reportParams.put(REPORT_LOGO_PARAMETER, logoResource.getURL().toExternalForm());
        } catch (Exception ex) {
            log.warn("No se pudo resolver la URL del logo por defecto en {}", DEFAULT_LOGO_CLASSPATH, ex);
        }
    }

    private void configureJasperCompiler() {
        JRPropertiesUtil.getInstance(DefaultJasperReportsContext.getInstance())
                .setProperty(JASPER_JAVA_COMPILER_PROPERTY, JASPER_JDT_COMPILER_CLASS);
        log.info("Compilador Jasper Java configurado: {}", JASPER_JDT_COMPILER_CLASS);
    }
}
