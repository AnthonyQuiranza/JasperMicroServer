# Guia de Instalacion y Despliegue en Windows

Esta guia te lleva desde cero hasta dejar el microservicio funcionando en Windows.

Repositorio oficial:

- https://github.com/AnthonyQuiranza/JasperMicroServer

## 1. Requisitos previos

Instala estos componentes:

- Git for Windows
- Java JDK 17
- Maven 3.9+
- Acceso de red hacia Oracle (host y puerto)
- Archivo `tnsnames.ora` con tu alias TNS

Verifica en PowerShell:

```powershell
git --version
java -version
mvn -version
```

## 2. Crear carpeta de trabajo y clonar el repositorio

En PowerShell:

```powershell
New-Item -Path "C:\apps" -ItemType Directory -Force | Out-Null
Set-Location "C:\apps"
git clone https://github.com/AnthonyQuiranza/JasperMicroServer.git
Set-Location "C:\apps\JasperMicroServer"
```

## 3. Compilar el proyecto

```powershell
mvn clean package -DskipTests
```

Al finalizar, el jar se genera en:

- `target\jasper-report-api-0.0.1-SNAPSHOT.jar`

## 4. Preparar `tnsnames.ora`

Crea una carpeta para la configuracion Oracle:

```powershell
New-Item -Path "C:\oracle\network\admin" -ItemType Directory -Force | Out-Null
```

Copia tu `tnsnames.ora` en esta ruta:

- `C:\oracle\network\admin\tnsnames.ora`

Ejemplo de alias en el archivo:

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

## 5. Configurar variables de entorno (explicadas)

Puedes configurarlas por sesion (temporal) o de forma permanente en Windows.

### 5.1 Variables temporales (solo la sesion actual de PowerShell)

```powershell
$env:ORACLE_NET_TNS_ADMIN = "C:\oracle\network\admin"
$env:ORACLE_TNS_ALIAS = "ORCLPDB1"
$env:ORACLE_DB_USER = "app_user"
$env:ORACLE_DB_PASSWORD = "app_password"
$env:DB_POOL_MAX_SIZE = "10"
$env:DB_POOL_MIN_IDLE = "2"
$env:PORT = "8080"
```

### 5.2 Variables permanentes (a nivel de maquina)

Ejecuta PowerShell como Administrador:

```powershell
[Environment]::SetEnvironmentVariable("ORACLE_NET_TNS_ADMIN", "C:\oracle\network\admin", "Machine")
[Environment]::SetEnvironmentVariable("ORACLE_TNS_ALIAS", "ORCLPDB1", "Machine")
[Environment]::SetEnvironmentVariable("ORACLE_DB_USER", "app_user", "Machine")
[Environment]::SetEnvironmentVariable("ORACLE_DB_PASSWORD", "app_password", "Machine")
[Environment]::SetEnvironmentVariable("DB_POOL_MAX_SIZE", "10", "Machine")
[Environment]::SetEnvironmentVariable("DB_POOL_MIN_IDLE", "2", "Machine")
[Environment]::SetEnvironmentVariable("PORT", "8080", "Machine")
```

Cierra y abre una nueva consola para que tomen efecto.

## 6. Que significa cada variable

- `ORACLE_NET_TNS_ADMIN`:
  - Ruta donde esta `tnsnames.ora`.
  - El driver Oracle busca ahi los alias TNS.

- `ORACLE_TNS_ALIAS`:
  - Nombre del alias definido en `tnsnames.ora`.
  - Ejemplo: `ORCLPDB1`.

- `ORACLE_DB_USER`:
  - Usuario de base de datos Oracle que usara la API.

- `ORACLE_DB_PASSWORD`:
  - Clave del usuario Oracle.

- `DB_POOL_MAX_SIZE`:
  - Maximo de conexiones simultaneas en el pool Hikari.
  - Si sube la concurrencia, este valor puede subir.

- `DB_POOL_MIN_IDLE`:
  - Conexiones que el pool mantiene listas en reposo.

- `PORT`:
  - Puerto HTTP donde se publica la API.

## 7. Ejecutar el microservicio

En PowerShell, dentro del proyecto:

```powershell
mvn spring-boot:run
```

O ejecutando el jar compilado:

```powershell
java -jar target\jasper-report-api-0.0.1-SNAPSHOT.jar
```

## 8. Verificar que funciona

Health checks:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/reports/health"
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health"
```

Si responde `200 OK`, esta operativo.

## 9. Ejecutarlo como servicio de Windows (opcional, recomendado)

En Windows no existe `systemd`, por eso se recomienda NSSM para convertir el jar en servicio.

### 9.1 Instalar NSSM

- Descarga NSSM: https://nssm.cc/download
- Descomprime en una ruta, por ejemplo: `C:\tools\nssm`

### 9.2 Crear servicio

PowerShell como Administrador:

```powershell
C:\tools\nssm\win64\nssm.exe install JasperReportApi
```

En la ventana de NSSM configura:

- Path: `C:\Program Files\Java\jdk-17\bin\java.exe`
- Startup directory: `C:\apps\JasperMicroServer`
- Arguments: `-jar target\jasper-report-api-0.0.1-SNAPSHOT.jar`

En la seccion de Environment agrega las variables (`ORACLE_NET_TNS_ADMIN`, `ORACLE_TNS_ALIAS`, etc.).

Luego inicia el servicio:

```powershell
Start-Service JasperReportApi
Get-Service JasperReportApi
```

## 10. Actualizar a una nueva version

```powershell
Set-Location "C:\apps\JasperMicroServer"
git pull origin main
mvn clean package -DskipTests
Restart-Service JasperReportApi
```

## 11. Problemas comunes

- Error `ORA-12154`:
  - El alias no se resuelve.
  - Revisa que `ORACLE_NET_TNS_ADMIN` apunte a la carpeta correcta y que el alias exista en `tnsnames.ora`.

- Error de autenticacion Oracle:
  - Revisa `ORACLE_DB_USER` y `ORACLE_DB_PASSWORD`.

- Puerto ocupado:
  - Cambia `PORT` (por ejemplo `8090`) y reinicia.

- Variables no aplican:
  - Si son permanentes, abre una nueva consola o reinicia el servicio.
