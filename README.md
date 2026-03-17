# Jasper Report API (Spring Boot)

Microservicio para generar reportes PDF con JasperReports, conectado a Oracle mediante alias TNS definidos en `tnsnames.ora`.

## Autor

- Anthony Quiranza
- anthony.quiranza@cloudsofts.net

## Que hace este servicio

- Expone endpoints REST para generar PDFs desde plantillas Jasper.
- Soporta plantillas precompiladas (`.jasper`) o compilacion runtime (`.jrxml`).
- Se conecta a Oracle usando alias TNS (ideal para ambientes empresariales).
- Incluye observabilidad y manejo global de errores.

## Stack

- Java 17
- Spring Boot 3.4.x
- JasperReports
- Oracle JDBC (ojdbc11)
- Actuator + HikariCP

## Ejecutar en local

### 1) Configurar `tnsnames.ora`

Ejemplo:

```ora
ORCLPDB1 =
  (DESCRIPTION =
    (ADDRESS = (PROTOCOL = TCP)(HOST = 10.10.10.20)(PORT = 1521))
    (CONNECT_DATA =
      (SERVER = DEDICATED)
      (SERVICE_NAME = ORCLPDB1)
    )
  )
```

### 2) Variables de entorno (PowerShell)

```powershell
$env:ORACLE_NET_TNS_ADMIN = "D:\oracle\network\admin"
$env:ORACLE_TNS_ALIAS = "ORCLPDB1"
$env:ORACLE_DB_USER = "app_user"
$env:ORACLE_DB_PASSWORD = "app_password"
```

### 3) Ejecutar

```powershell
mvn spring-boot:run
```

## Endpoints principales

- `GET /api/v1/reports/health`
- `GET /actuator/health`
- `POST /api/v1/reports/{reportName}/pdf`

Body de ejemplo:

```json
{
  "parameters": {
    "P_CLIENT_ID": 1001,
    "P_START_DATE": "2026-01-01",
    "P_END_DATE": "2026-12-31"
  },
  "useDatabase": true
}
```

## Plantillas Jasper

- `src/main/resources/reports/*.jasper`
- `src/main/resources/reports/*.jrxml`

Ejemplo: `POST /api/v1/reports/ventas_mensuales/pdf`

## Despliegue en Debian/Linux

Guia completa, paso a paso y lista para servidor:

- [docs/DEPLOY_DEBIAN_LINUX.md](docs/DEPLOY_DEBIAN_LINUX.md)

El flujo inicia desde clonar este repositorio en el servidor:

- https://github.com/AnthonyQuiranza/JasperMicroServer

Incluye:

- Instalacion de Java y preparacion del servidor
- Configuracion segura de variables de entorno
- Servicio `systemd` para arranque automatico
- Verificacion operativa y troubleshooting
- Opcion de publicar por Nginx

## Preparado para GitHub

Este repositorio ya incluye `.gitignore` para evitar subir:

- artefactos de build (`target/`)
- logs
- archivos sensibles y temporales

### Pasos recomendados para publicar

```bash
git init
git add .
git commit -m "feat: initial JasperReports microservice with Oracle TNS support"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/<tu-repo>.git
git push -u origin main
```

## Recomendaciones productivas

- Agregar autenticacion (JWT o API Key)
- Implementar rate limiting
- Centralizar logs (ELK / OpenSearch)
- Versionar plantillas de reportes por entorno o tenant
