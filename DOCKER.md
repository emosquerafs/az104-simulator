# Az104 Simulator - Docker Documentation

## 📦 Archivos Docker Creados

Este proyecto incluye una configuración completa de Docker con hardening de seguridad DevSecOps:

- **Dockerfile** - Imagen multi-stage optimizada y segura
- **.dockerignore** - Exclusión de archivos innecesarios del contexto
- **docker-compose.yml** - Orquestación local con configuración hardened
- **docker-run-examples.sh** - Script con ejemplos de comandos

## 🔒 Características de Seguridad

### Multi-Stage Build
- **Stage 1 (Builder)**: Compila con Gradle usando Eclipse Temurin JDK 21
- **Stage 2 (Runtime)**: Imagen mínima con JRE 21 Alpine (sin herramientas de build)

### Hardening Aplicado
- ✅ Usuario no-root (UID/GID 1001)
- ✅ Read-only filesystem compatible
- ✅ Sin privilegios (`no-new-privileges`)
- ✅ Capabilities dropped (`--cap-drop=ALL`)
- ✅ Resource limits (memoria, CPU, PIDs)
- ✅ Versiones fijas de imágenes base (no `latest`)
- ✅ Healthcheck integrado
- ✅ Tini como init process (manejo correcto de señales)
- ✅ JVM optimizado para contenedores

### JVM Configuration
```bash
JAVA_TOOL_OPTIONS:
  -XX:MaxRAMPercentage=75.0          # Usa 75% de memoria del contenedor
  -XX:+UseContainerSupport           # Detección de límites del contenedor
  -XX:+OptimizeStringConcat          # Optimización de strings
  -XX:+UseStringDeduplication        # Deduplicación de strings
  -XX:+ExitOnOutOfMemoryError        # Exit limpio en OOM
  -Djava.security.egd=file:/dev/./urandom  # Entropía no bloqueante
  -Dfile.encoding=UTF-8              # UTF-8 forzado
  -Duser.timezone=UTC                # Zona horaria consistente
  -Djava.io.tmpdir=/tmp-app          # Directorio temporal custom
```

## 🚀 Inicio Rápido

### Opción 1: Docker Compose (Recomendado)
```bash
# Construir e iniciar
docker compose up -d

# Ver logs
docker compose logs -f

# Detener
docker compose down
```

### Opción 2: Docker CLI
```bash
# Construir
docker build -t az104-simulator:latest .

# Ejecutar
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  az104-simulator:latest
```

### Opción 3: Hardened Production Run
```bash
docker run -d \
  --name az104-simulator \
  --restart unless-stopped \
  --read-only \
  --cap-drop=ALL \
  --security-opt no-new-privileges:true \
  --pids-limit 100 \
  --memory=512m \
  --cpus=1.0 \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=default \
  --tmpfs /tmp:mode=1777,size=104857600 \
  --tmpfs /tmp-app:mode=1777,size=104857600 \
  az104-simulator:latest
```

## 📊 Verificación

```bash
# Estado del contenedor
docker ps

# Health status
docker inspect --format='{{.State.Health.Status}}' az104-simulator

# Logs
docker logs -f az104-simulator

# Recursos
docker stats az104-simulator

# Acceso a la aplicación
curl http://localhost:8080
```

## 🗄️ H2 Database Modes

### File Mode (Configuración por defecto - Persistente)

**⚠️ ACTUALIZACIÓN:** La aplicación ahora usa H2 en modo archivo por defecto para persistencia de datos.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:h2:file:/app/data/az104db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}
```

**Uso con named volume (recomendado):**
```bash
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -v az104_data:/app/data \
  singularitsas/az104-exam-simulator:1.0.0
```

**Con Docker Compose:**
```bash
docker-compose up -d
# El volumen az104_data se crea automáticamente
```

**Ventajas:**
- ✅ Los exámenes y respuestas persisten entre reinicios
- ✅ Puedes detener y reiniciar el contenedor sin perder progreso
- ✅ Ideal para uso real del simulador

### In-Memory Mode (Para testing o demos)

Si prefieres modo volátil (datos se borran al reiniciar):

```bash
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:h2:mem:az104db" \
  singularitsas/az104-exam-simulator:1.0.0
```

**Ventajas:**
- ✅ No requiere volúmenes
- ✅ Útil para demos o testing
- ✅ Cada reinicio es una BD limpia

### Acceso a H2 Console

- **URL**: http://localhost:8080/h2-console
- **JDBC URL (file mode)**: `jdbc:h2:file:/app/data/az104db`
- **JDBC URL (memory mode)**: `jdbc:h2:mem:az104db`
- **Username**: `sa`
- **Password**: (vacío)

### Gestión de Volúmenes

```bash
# Listar volúmenes
docker volume ls

# Inspeccionar volumen
docker volume inspect az104_data

# Backup de la base de datos
docker run --rm \
  -v az104_data:/source:ro \
  -v $(pwd):/backup \
  alpine tar -czf /backup/az104-backup-$(date +%Y%m%d).tar.gz -C /source .

# Restaurar backup
docker run --rm \
  -v az104_data:/target \
  -v $(pwd):/backup \
  alpine tar -xzf /backup/az104-backup-20260116.tar.gz -C /target

# Eliminar volumen (⚠️ borra todos los datos)
docker-compose down -v
# o
docker volume rm az104_data
```

## 🔐 Seguridad Avanzada

### Escaneo de Vulnerabilidades

```bash
# Trivy (recomendado)
trivy image az104-simulator:latest

# Docker Scout
docker scout cves az104-simulator:latest

# Grype
grype az104-simulator:latest
```

### Generación de SBOM

```bash
# Con Syft
syft az104-simulator:latest -o spdx-json > sbom.json
syft az104-simulator:latest -o cyclonedx-json > sbom-cyclonedx.json

# Con Docker SBOM
docker sbom az104-simulator:latest
```

### Runtime Security Scanning

```bash
# Falco para monitoreo runtime
# Anchore Engine para políticas
```

## 🎛️ Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring | `default` |
| `APP_LOCALE_DEFAULT` | Locale por defecto | `en` |
| `TZ` | Timezone | `UTC` |
| `JAVA_TOOL_OPTIONS` | Opciones JVM | (Ver arriba) |

### Ejemplo con variables custom
```bash
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e APP_LOCALE_DEFAULT=es \
  -e TZ=Europe/Madrid \
  az104-simulator:latest
```

## 📝 Logs y Debugging

```bash
# Logs en tiempo real
docker logs -f az104-simulator

# Últimas 100 líneas
docker logs --tail 100 az104-simulator

# Shell dentro del contenedor (debugging)
docker exec -it az104-simulator sh

# Inspeccionar contenedor
docker inspect az104-simulator
```

## 🔄 Actualización y Rebuild

```bash
# Con Docker Compose
docker compose up -d --build

# Con Docker CLI
docker build -t az104-simulator:latest .
docker stop az104-simulator
docker rm az104-simulator
docker run -d --name az104-simulator -p 8080:8080 az104-simulator:latest
```

## 🌐 Acceso a la Aplicación

- **Aplicación**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:az104db`
  - Username: `sa`
  - Password: (vacío)

## 📚 Optimizaciones de Capas

El Dockerfile usa Spring Boot Layered JARs para optimizar el rebuild:
1. **dependencies** - Dependencias externas (rara vez cambian)
2. **spring-boot-loader** - Loader de Spring Boot
3. **snapshot-dependencies** - Dependencias SNAPSHOT
4. **application** - Tu código (cambia frecuentemente)

Esto permite que Docker cachee las capas inferiores y solo reconstruya la capa de aplicación cuando cambies código.

## 🛠️ Troubleshooting

### El contenedor no inicia
```bash
# Ver logs
docker logs az104-simulator

# Verificar health
docker inspect az104-simulator | grep -A 10 Health
```

### Problemas de permisos

#### ❌ Error: `AccessDeniedException` en `/data` o `/app/data`

**Síntoma:**
```
org.h2.message.DbException: Log file error: "/data/az104db.trace.db", 
cause: "java.nio.file.AccessDeniedException: /data/az104db.trace.db"
```

**Causa:**  
El contenedor corre con usuario no-root (UID 1001) y no tiene permisos de escritura en el volumen montado.

**Solución 1: Usar named volumes (recomendado)**
```bash
# Docker maneja automáticamente los permisos
docker volume create az104_data

docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/az104db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" \
  -v az104_data:/app/data \
  singularitsas/az104-exam-simulator:1.0.0
```

**Solución 2: Bind mount con permisos correctos**
```bash
# Crear directorio y asignar permisos al UID del contenedor
mkdir -p ./data
sudo chown -R 1001:1001 ./data
chmod 755 ./data

# Ejecutar con bind mount
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/az104db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" \
  -v $(pwd)/data:/app/data \
  singularitsas/az104-exam-simulator:1.0.0
```

**Solución 3: Usar Docker Compose (más simple)**
```bash
# docker-compose.yml ya tiene la configuración correcta
docker-compose up -d
```

#### ⚠️ Importante: Path del volumen

El Dockerfile declara `VOLUME ["/tmp-app", "/app/data"]`.  
**Siempre monta volúmenes en `/app/data`, NO en `/data`:**

```bash
# ✅ CORRECTO
-v az104_data:/app/data

# ❌ INCORRECTO (causa permission denied)
-v az104_data:/data
```

#### Verificar permisos dentro del contenedor

```bash
# Ver usuario y permisos
docker exec az104-simulator id
# Output esperado: uid=1001(appuser) gid=1001(appgroup)

# Ver permisos del directorio
docker exec az104-simulator ls -la /app/data
# Debe mostrar: drwxr-xr-x appuser appgroup
```

### Out of Memory
Ajusta los límites de memoria:
```bash
docker run -d --memory=1g --memory-reservation=512m ...
```

O modifica `JAVA_TOOL_OPTIONS` para usar menos RAM:
```bash
-e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50.0 ..."
```

## 📦 Tamaño de la Imagen

```bash
# Ver tamaño
docker images az104-simulator

# Analizar capas
docker history az104-simulator:latest
```

Tamaño esperado: ~300-400MB (JRE Alpine + App)

## 🔗 Referencias

- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [OWASP Docker Security](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)

## 📄 Licencia

Ver LICENSE en el repositorio principal.

