param(
    [string]$EnvFile = ".env",
    [string]$Mode = "jar"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    throw "No se encontro el archivo de entorno: $EnvFile"
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    $idx = $line.IndexOf("=")
    if ($idx -lt 1) {
        return
    }

    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()

    [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
}

Write-Host "Variables cargadas desde $EnvFile"

if ($Mode -eq "mvn") {
    mvn spring-boot:run
} else {
    $jarPath = "target/jasper-report-api-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jarPath)) {
        throw "No se encontro el jar en $jarPath. Ejecuta primero: mvn clean package -DskipTests"
    }

    java -jar $jarPath
}
