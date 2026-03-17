# Guia de Despliegue en Debian / Linux

Esta guia esta pensada para que el microservicio quede estable en un servidor real, con buenas practicas operativas y pasos claros.

## 1. Requisitos del servidor

- Debian 12+ o Ubuntu 22.04+
- Acceso sudo
- Java 17 instalado
- Acceso de red a Oracle (host/puerto)
- Archivo `tnsnames.ora` disponible

## 2. Preparar el servidor

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless curl unzip
java -version
```

## 3. Crear usuario tecnico y estructura de carpetas

```bash
sudo useradd --system --create-home --shell /bin/bash jasperapi
sudo mkdir -p /opt/jasper-report-api
sudo mkdir -p /opt/jasper-report-api/config/oracle
sudo mkdir -p /opt/jasper-report-api/logs
sudo chown -R jasperapi:jasperapi /opt/jasper-report-api
```

## 4. Copiar artefacto y configuraciones

Desde tu maquina de build, genera el jar:

```bash
mvn clean package -DskipTests
```

Copia el jar al servidor (ajusta host/usuario):

```bash
scp target/jasper-report-api-0.0.1-SNAPSHOT.jar usuario@tu-servidor:/opt/jasper-report-api/app.jar
```

Copia tambien el `tnsnames.ora` al servidor:

```bash
sudo cp tnsnames.ora /opt/jasper-report-api/config/oracle/tnsnames.ora
sudo chown jasperapi:jasperapi /opt/jasper-report-api/config/oracle/tnsnames.ora
sudo chmod 640 /opt/jasper-report-api/config/oracle/tnsnames.ora
```

## 5. Crear archivo de entorno

Crea este archivo:

```bash
sudo nano /etc/jasper-report-api.env
```

Contenido recomendado:

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

Protege el archivo:

```bash
sudo chown root:root /etc/jasper-report-api.env
sudo chmod 600 /etc/jasper-report-api.env
```

## 6. Crear servicio systemd

Crea el servicio:

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

Activar y arrancar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable jasper-report-api
sudo systemctl start jasper-report-api
```

## 7. Verificacion operativa

Estado del servicio:

```bash
sudo systemctl status jasper-report-api
```

Logs en tiempo real:

```bash
sudo journalctl -u jasper-report-api -f
```

Health check:

```bash
curl http://localhost:8080/api/v1/reports/health
curl http://localhost:8080/actuator/health
```

## 8. Exponer por Nginx (opcional, recomendado)

Instala Nginx:

```bash
sudo apt install -y nginx
```

Configura un virtual host:

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

Luego habilita y recarga:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 9. Despliegue de nuevas versiones

1. Compilar nuevo jar.
2. Subir nuevo jar a `/opt/jasper-report-api/app.jar`.
3. Reiniciar servicio.

```bash
sudo systemctl restart jasper-report-api
```

## 10. Problemas comunes

- `ORA-12154`: alias TNS no resuelto.
  - Verifica `ORACLE_NET_TNS_ADMIN` y la existencia de `tnsnames.ora`.
- `Connection refused` hacia Oracle.
  - Revisa firewall/rutas/puerto 1521.
- Servicio no levanta.
  - Revisa `journalctl -u jasper-report-api -f`.
