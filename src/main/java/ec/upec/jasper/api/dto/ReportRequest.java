package ec.upec.jasper.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ReportRequest(
        @NotNull Map<String, Object> parameters,
        boolean useDatabase
) {
}
