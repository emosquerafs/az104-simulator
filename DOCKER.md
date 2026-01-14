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

### In-Memory (Actual configuración)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:az104db
```
No requiere volúmenes persistentes. Los datos se pierden al reiniciar.

### File Mode (Para persistencia)
```yaml
spring:
  datasource:
    url: jdbc:h2:file:/app/data/az104db
```

Si cambias a file mode, usa:
```bash
# Crear volumen
docker volume create az104-data

# Ejecutar con volumen
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  -v az104-data:/app/data \
  --tmpfs /tmp-app:mode=1777,size=104857600 \
  az104-simulator:latest
```

O descomenta la sección de volumes en `docker-compose.yml`.

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
Verifica que los directorios `/tmp-app` y `/app/data` estén montados correctamente con tmpfs o volúmenes.

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

