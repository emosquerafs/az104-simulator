# 🚀 INICIO RÁPIDO - AZ-104 Simulador Bilingüe

## Implementación Completada ✅

El simulador ahora tiene:
- ✅ GUI 100% bilingüe (ES/EN)
- ✅ Preguntas localizadas
- ✅ Navegación preserva idioma
- ✅ 25 preguntas de muestra bilingües

---

## Opción 1: Usar con Preguntas de Muestra (5 min)

```bash
# 1. Ir al proyecto
cd /Users/emosquera/Develop/SingularIt/Az104Simulator

# 2. Usar el banco de preguntas bilingüe (25 preguntas)
cp BILINGUAL_QUESTIONS_SAMPLE.json src/main/resources/seed/questions.json

# 3. Iniciar
./gradlew bootRun

# 4. Abrir navegador
# Español: http://localhost:8080/?lang=es
# English: http://localhost:8080/?lang=en
```

✅ **Listo!** Ahora puedes:
- Ver interfaz en español o inglés
- Iniciar práctica/examen en cualquier idioma
- Cambiar idioma en cualquier momento sin perder progreso
- Ver preguntas, opciones y explicaciones localizadas

---

## Opción 2: Mantener Preguntas Actuales (monolingües)

```bash
# Simplemente iniciar
./gradlew bootRun
```

⚠️ **Nota:** Las preguntas actuales solo están en inglés. El GUI funcionará en ambos idiomas, pero las preguntas solo se mostrarán en inglés.

Para migrar las preguntas existentes a formato bilingüe, consulta `BILINGUAL_IMPLEMENTATION_GUIDE.md`.

---

## Probar el Sistema Bilingüe

### Test 1: GUI en Español
1. Ir a: `http://localhost:8080/?lang=es`
2. Verificar:
   - ✅ Título: "Simulador de Examen AZ-104"
   - ✅ Botones: "Iniciar Práctica", "Iniciar Examen"
   - ✅ Dominios: "Identidad y Gobernanza", "Almacenamiento", etc.

### Test 2: GUI en Inglés
1. Ir a: `http://localhost:8080/?lang=en`
2. Verificar:
   - ✅ Título: "AZ-104 Exam Simulator"
   - ✅ Botones: "Start Practice", "Start Exam"
   - ✅ Dominios: "Identity & Governance", "Storage", etc.

### Test 3: Cambio de Idioma Durante Examen
1. Iniciar práctica en español (`?lang=es`)
2. Responder una pregunta
3. Cambiar selector a "English"
4. Verificar:
   - ✅ GUI cambia a inglés
   - ✅ Pregunta cambia a inglés
   - ✅ Respuesta anterior se mantiene
   - ✅ No se reinicia el intento

### Test 4: Navegación Preserva Idioma
1. Estar en español
2. Hacer clic en "Siguiente" → sigue en español
3. Hacer clic en "Revisar Todas" → sigue en español
4. Ver resultados → todo en español

---

## Verificar Base de Datos

Si usas las 25 preguntas de muestra:

```bash
# 1. Iniciar app
./gradlew bootRun

# 2. Abrir H2 Console en navegador
http://localhost:8080/h2-console

# 3. Conectar con:
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: (dejar vacío)

# 4. Ejecutar query:
SELECT id, 
       LEFT(stem_es, 50) as pregunta_es,
       LEFT(stem_en, 50) as pregunta_en
FROM question
LIMIT 5;
```

Deberías ver preguntas en ambos idiomas.

---

## Estructura del Banco de Preguntas Bilingüe

El archivo `BILINGUAL_QUESTIONS_SAMPLE.json` contiene **25 preguntas**:

| Dominio               | Cantidad | Niveles           |
|-----------------------|----------|-------------------|
| Identity/Governance   | 6        | EASY, MEDIUM, HARD|
| Compute               | 4        | EASY, MEDIUM, HARD|
| Networking            | 5        | EASY, MEDIUM, HARD|
| Storage               | 5        | EASY, MEDIUM, HARD|
| Monitor/Maintain      | 5        | EASY, MEDIUM, HARD|

Cada pregunta tiene:
- ✅ Stem en ES + EN
- ✅ Explanation en ES + EN  
- ✅ Opciones en ES + EN
- ✅ Tags técnicos
- ✅ Tipo: SINGLE, MULTI o YESNO

---

## Expandir a 800 Preguntas

Para crear un banco completo, sigue estos pasos:

### 1. Usar la Plantilla
Copia el formato de `BILINGUAL_QUESTIONS_SAMPLE.json` y crea más preguntas siguiendo el patrón.

### 2. Distribución Recomendada
- **Identity/Governance:** 200 preguntas (25%)
- **Compute:** 180 preguntas (22%)
- **Networking:** 140 preguntas (18%)
- **Storage:** 160 preguntas (20%)
- **Monitor/Maintain:** 120 preguntas (15%)

### 3. Mix de Dificultad
- **EASY:** 25% (200 preguntas)
- **MEDIUM:** 55% (440 preguntas)
- **HARD:** 20% (160 preguntas)

### 4. Mix de Tipos
- **SINGLE:** 70% (560 preguntas)
- **MULTI:** 25% (200 preguntas)
- **YESNO:** 5% (40 preguntas)

### 5. Validación
Cada pregunta debe:
- ✅ Ser 100% original (no copiar de exámenes reales)
- ✅ Tener ambos idiomas (ES + EN)
- ✅ Tener explicación técnica concisa
- ✅ Tener distractores plausibles
- ✅ Tener tags relevantes

Ver `BILINGUAL_IMPLEMENTATION_GUIDE.md` para reglas detalladas.

---

## Solución de Problemas

### Problema: GUI sigue en inglés con `?lang=es`

**Solución:**
1. Limpiar cookies del navegador
2. Reiniciar la app: `./gradlew clean bootRun`
3. Verificar URL incluye `?lang=es`

### Problema: Preguntas no cambian de idioma

**Causa:** Estás usando el banco de preguntas monolingüe antiguo.

**Solución:**
```bash
cp BILINGUAL_QUESTIONS_SAMPLE.json src/main/resources/seed/questions.json
./gradlew clean bootRun
```

### Problema: Error al iniciar

**Causa:** Puerto 8080 ocupado.

**Solución:**
```bash
# Matar proceso en puerto 8080
lsof -ti:8080 | xargs kill -9

# O cambiar puerto en application.yml
server.port=8081
```

---

## Archivos de Documentación

| Archivo | Descripción |
|---------|-------------|
| `DELIVERY_SUMMARY.md` | Resumen ejecutivo de lo entregado |
| `BILINGUAL_IMPLEMENTATION_GUIDE.md` | Guía completa de implementación (300+ líneas) |
| `BILINGUAL_QUESTIONS_SAMPLE.json` | 25 preguntas bilingües de muestra |
| `QUICK_START.md` | Este archivo |

---

## Contacto de Soporte

- **Logs de la app:** `app.log`
- **H2 Console:** http://localhost:8080/h2-console
- **Build info:** `./gradlew build --info`

---

## ✅ Checklist de Validación

Después de iniciar la app, verifica:

- [ ] Home en español muestra textos en español
- [ ] Home en inglés muestra textos en inglés
- [ ] Selector de idioma funciona
- [ ] Config page muestra textos localizados
- [ ] Exam muestra preguntas en idioma seleccionado
- [ ] Cambiar idioma durante exam funciona
- [ ] Next/Previous mantienen idioma
- [ ] Review mantiene idioma
- [ ] Results mantienen idioma
- [ ] Base de datos tiene preguntas en ambos idiomas

---

**¡Listo para usar!** 🎉

Para más detalles, consulta `BILINGUAL_IMPLEMENTATION_GUIDE.md`.

