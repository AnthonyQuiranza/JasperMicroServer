# Guia de Instalacion y Despliegue (Debian / Linux)

Esta guia parte desde cero: descargar el proyecto desde GitHub y dejarlo corriendo como servicio en el servidor.

Repositorio oficial:

- https://github.com/AnthonyQuiranza/JasperMicroServer

## 1. Requisitos

- Debian 12+ o Ubuntu 22.04+
- Usuario con permisos sudo
- Acceso de red desde el servidor hacia Oracle (host y puerto)
- Archivo `tnsnames.ora` con el alias que vas a usar

## 2. Instalar dependencias del sistema

```bash
sudo apt update
sudo apt install -y git curl unzip openjdk-17-jdk maven
java -version
mvn -version
git --version
```

## 3. Crear usuario tecnico y carpetas de trabajo

```bash
sudo useradd --system --create-home --shell /bin/bash jasperapi
sudo mkdir -p /opt/jasper-report-api
sudo mkdir -p /opt/jasper-report-api/config/oracle
sudo mkdir -p /opt/jasper-report-api/logs
sudo chown -R jasperapi:jasperapi /opt/jasper-report-api
```

## 4. Descargar el codigo desde GitHub

```bash
sudo -u jasperapi git clone https://github.com/AnthonyQuiranza/JasperMicroServer.git /opt/jasper-report-api/source
```

Verifica que se haya clonado:

```bash
ls -la /opt/jasper-report-api/source
```

## 5. Compilar el proyecto en el servidor

```bash
cd /opt/jasper-report-api/source
sudo -u jasperapi mvn clean package -DskipTests
```

Crear enlace al jar para ejecucion estable:

```bash
sudo ln -sf /opt/jasper-report-api/source/target/jasper-report-api-0.0.1-SNAPSHOT.jar /opt/jasper-report-api/app.jar
sudo chown -h jasperapi:jasperapi /opt/jasper-report-api/app.jar
```

## 6. Configurar `tnsnames.ora`

Copia tu archivo TNS al servidor:

```bash
sudo cp tnsnames.ora /opt/jasper-report-api/config/oracle/tnsnames.ora
sudo chown jasperapi:jasperapi /opt/jasper-report-api/config/oracle/tnsnames.ora
sudo chmod 640 /opt/jasper-report-api/config/oracle/tnsnames.ora
```

Ejemplo de alias dentro del archivo:

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

## 7. Crear variables de entorno del servicio

```bash
sudo nano /etc/jasper-report-api.env
```

Contenido sugerido:

```bash
PORT=8080
ORACLE_NET_TNS_ADMIN=/opt/jasper-report-api/config/oracle
ORACLE_TNS_ALIAS=ORCLPDB1
ORACLE_DB_USER=app_user
ORACLE_DB_PASSWORD=app_password_seguro
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
JAVA_OPTS=-Xms256m -Xmx1024m
```

Proteger archivo de entorno:

```bash
sudo chown root:root /etc/jasper-report-api.env
sudo chmod 600 /etc/jasper-report-api.env
```

## 8. Crear servicio systemd

```bash
sudo nano /etc/systemd/system/jasper-report-api.service
```

Contenido:

```ini
[Unit]
Description=Jasper Report API (Spring Boot)
After=network.target

[Service]
Type=simple
User=jasperapi
Group=jasperapi
WorkingDirectory=/opt/jasper-report-api
EnvironmentFile=/etc/jasper-report-api.env
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/jasper-report-api/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=5
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

Recargar y levantar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable jasper-report-api
sudo systemctl start jasper-report-api
```

## 9. Verificar que quedo funcionando

```bash
sudo systemctl status jasper-report-api
sudo journalctl -u jasper-report-api -f
```

Health checks:

```bash
curl http://localhost:8080/api/v1/reports/health
curl http://localhost:8080/actuator/health
```

## 10. Exponer por dominio con Nginx (opcional)

```bash
sudo apt install -y nginx
```

Ejemplo de configuracion:

```nginx
server {
    listen 80;
    server_name reportes.tu-dominio.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Aplicar cambios:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 11. Actualizar a una nueva version desde GitHub

```bash
cd /opt/jasper-report-api/source
sudo -u jasperapi git pull origin main
sudo -u jasperapi mvn clean package -DskipTests
sudo ln -sf /opt/jasper-report-api/source/target/jasper-report-api-0.0.1-SNAPSHOT.jar /opt/jasper-report-api/app.jar
sudo systemctl restart jasper-report-api
```

## 12. Solucion de problemas rapida

- Error `ORA-12154`:
  - El alias no se esta resolviendo. Verifica `ORACLE_NET_TNS_ADMIN` y que exista `tnsnames.ora`.
- Error de conexion Oracle:
  - Revisar firewall, rutas y puerto 1521.
- El servicio no arranca:
  - Revisar logs con `sudo journalctl -u jasper-report-api -f`.
- Puerto ocupado:
  - Cambiar `PORT` en `/etc/jasper-report-api.env` y reiniciar.
