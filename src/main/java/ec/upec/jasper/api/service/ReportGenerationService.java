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
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final JasperApiProperties properties;

    public ReportGenerationService(DataSource dataSource, ResourceLoader resourceLoader, JasperApiProperties properties) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public byte[] generatePdf(String reportName, ReportRequest request) {
        Map<String, Object> reportParams = new HashMap<>();
        reportParams.putAll(request.parameters());

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
            throw new ReportGenerationException("Error generando el reporte '%s'".formatted(reportName), ex);
        } catch (Exception ex) {
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
}
