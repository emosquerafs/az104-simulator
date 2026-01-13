# 🚀 Cómo Ver la Nueva Funcionalidad en IntelliJ

## ✅ Cambios Ya Aplicados Automáticamente

Los siguientes cambios **YA ESTÁN LISTOS** en tu código:

1. ✅ **Configuración i18n completa** (I18nConfig.java)
2. ✅ **Archivos de mensajes** (messages_es.properties, messages_en.properties)
3. ✅ **Migración Flyway V3** (columnas bilingües en BD)
4. ✅ **Entidades actualizadas** (Question, OptionItem con campos *_es/*_en)
5. ✅ **QuestionService con soporte i18n**
6. ✅ **ExamController actualizado** (obtiene locale automáticamente)
7. ✅ **AttemptService actualizado** (pasa idioma a preguntas)
8. ✅ **Selector de idioma en home.html** (arriba a la derecha)
9. ✅ **CSS para estados visuales** (clases ready, solo falta aplicar en templates)

## 📋 Pasos para Ejecutar en IntelliJ

### 1. Rebuild del Proyecto

En IntelliJ:
```
Build → Rebuild Project
```

O desde terminal dentro de IntelliJ:
```bash
./gradlew clean build
```

### 2. Ejecutar la Aplicación

**Opción A: Desde IntelliJ (recomendado)**
1. Click derecho en `Az104SimulatorApplication.java`
2. Run 'Az104SimulatorApplication'

**Opción B: Desde terminal**
```bash
./gradlew bootRun
```

### 3. Abrir en el Navegador

```
http://localhost:8080
```

## 🎯 Qué Puedes Ver Ahora

### ✅ Selector de Idioma (Funciona)

**Ubicación**: Arriba a la derecha en la página principal

**Cómo probar**:
1. Abre http://localhost:8080
2. Verás un selector: "🇪🇸 Español | 🇬🇧 English"
3. Cambia el idioma → la página recarga

**Notas**:
- Los textos de la UI aún están en inglés (hardcodeados en templates)
- Las **preguntas** YA cambian de idioma (actualmente duplicadas ES/EN)
- El idioma se guarda en cookie por 30 días

### ✅ Preguntas Bilingües (Funciona)

**Cómo probar**:
1. Inicia un examen Practice o Exam
2. Agrega `?lang=en` o `?lang=es` a la URL del examen
3. Ejemplo: `http://localhost:8080/attempt/abc-123/question/0?lang=en`
4. Las preguntas cambiarán de idioma

**Actual**: Ambos idiomas muestran el mismo texto (duplicado)
**Para cambiar**: Edita la BD o espera a que agreguemos traducciones reales

### ⏳ Estados Visuales Sidebar (CSS Listo, Falta Template)

**Estado**:
- ✅ CSS configurado (verde/amarillo/gris)
- ❌ Templates necesitan actualización

**Para implementar**: Ver `IMPLEMENTATION_GUIDE.md` sección 4 (exam.html)

### ⏳ Review Screen Mejorada (Pendiente)

**Para implementar**: Ver `IMPLEMENTATION_GUIDE.md` sección 6 (review.html)

## 🔍 Verificar que Todo Funciona

### 1. Verificar Migración V3

**Opción A: H2 Console**
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:az104db
Username: sa
Password: (vacío)
```

**Query**:
```sql
SELECT * FROM flyway_schema_history;
```

Deberías ver:
- V1__schema (tabla creada)
- V3__i18n_questions (columnas bilingües agregadas)

**Query para verificar columnas**:
```sql
SELECT id, stem, stem_es, stem_en FROM question LIMIT 1;
```

### 2. Verificar Logs

En la consola de IntelliJ busca:
```
Successfully loaded 104 questions from JSON
```

### 3. Verificar Idioma en Cookie

1. Abre Developer Tools (F12)
2. Application → Cookies → http://localhost:8080
3. Busca cookie `APP_LOCALE` con valor `es` o `en`

## 🎨 Cómo Agregar Estados Visuales al Sidebar

Si quieres ver el sidebar con colores (verde/amarillo/gris) cuando respondes:

**Archivo**: `exam.html`

**Busca** (línea ~70):
```html
<div th:each="i : ${#numbers.sequence(0, attempt.totalQuestions - 1)}"
     class="question-number"
     th:classappend="${i == currentIndex} ? 'current' : ''"
     th:attr="data-index=${i}">
```

**Reemplaza** con:
```html
<div th:each="i : ${#numbers.sequence(0, attempt.totalQuestions - 1)}"
     class="question-number q-unanswered"
     th:classappend="${i == currentIndex} ? 'current' : ''"
     th:attr="data-index=${i}">
```

**Resultado**: Todos los números aparecerán en gris (unanswered)

**Para estados dinámicos**: Necesitas implementar el método `getQuestionStates()` en AttemptService (ver IMPLEMENTATION_GUIDE.md)

## 🐛 Solución de Problemas

### Problema: El selector de idioma no aparece

**Solución**:
1. Verifica que home.html se actualizó
2. Limpia caché: Build → Rebuild Project
3. Hard refresh: Cmd+Shift+R (Mac) / Ctrl+Shift+R (Windows)

### Problema: Las preguntas no cambian de idioma

**Solución**:
1. Verifica logs: `Successfully loaded X questions`
2. Verifica BD: columnas *_es y *_en deben tener contenido
3. Verifica URL tiene `?lang=en` o `?lang=es`

### Problema: Error de compilación

**Solución**:
```bash
./gradlew clean
./gradlew build
```

Si persiste:
- File → Invalidate Caches → Invalidate and Restart

### Problema: "Port 8080 already in use"

**Solución**:
```bash
# Mac/Linux
lsof -ti:8080 | xargs kill -9

# O desde el código
pkill -f "Az104Simulator"
```

## 📞 Siguiente Paso

Una vez que veas el **selector de idioma funcionando** en home:

1. Confirma que funciona
2. Podemos continuar con:
   - Estados visuales dinámicos del sidebar
   - Review screen mejorada
   - Internacionalizar textos del GUI

## 📄 Archivos Modificados (Para tu Referencia)

```
src/main/java/co/singularit/az104simulator/
├── config/
│   ├── I18nConfig.java ✅ NUEVO
│   └── QuestionDataLoader.java ✅ ACTUALIZADO
├── controller/
│   └── ExamController.java ✅ ACTUALIZADO
├── domain/
│   ├── Question.java ✅ ACTUALIZADO
│   └── OptionItem.java ✅ ACTUALIZADO
└── service/
    ├── AttemptService.java ✅ ACTUALIZADO
    └── QuestionService.java ✅ ACTUALIZADO

src/main/resources/
├── db/migration/
│   └── V3__i18n_questions.sql ✅ NUEVO
├── messages_es.properties ✅ NUEVO
├── messages_en.properties ✅ NUEVO
├── static/css/
│   └── style.css ✅ ACTUALIZADO (clases nuevas agregadas)
└── templates/
    └── home.html ✅ ACTUALIZADO (selector agregado)
```

---

**¡Listo!** Ahora solo necesitas hacer **Rebuild** en IntelliJ y ejecutar la app. 🎉
