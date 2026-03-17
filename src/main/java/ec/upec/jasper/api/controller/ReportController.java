package ec.upec.jasper.api.controller;

import ec.upec.jasper.api.dto.ReportRequest;
import ec.upec.jasper.api.dto.ReportResponse;
import ec.upec.jasper.api.service.ReportGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    public ReportController(ReportGenerationService reportGenerationService) {
        this.reportGenerationService = reportGenerationService;
    }

    @GetMapping("/health")
    public ResponseEntity<ReportResponse> health() {
        return ResponseEntity.ok(new ReportResponse("UP", "Servicio de reportes disponible"));
    }

    @PostMapping(value = "/{reportName}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(
            @PathVariable String reportName,
            @Valid @RequestBody ReportRequest request
    ) {
        byte[] reportBytes = reportGenerationService.generatePdf(reportName, request);
        String fileName = reportName + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportBytes);
    }
}
