# 🚀 Quick Start - Ver Nueva Funcionalidad

## Estado Actual

✅ **Backend completado** (i18n, entities, migrations, CSS)
❌ **Frontend pendiente** (templates necesitan actualizarse)

## Para Ver la Funcionalidad Ahora

### Opción 1: Probar i18n básico (SIN modificar templates)

1. **Detén la app en IntelliJ**

2. **Ejecuta desde terminal**:
```bash
./gradlew clean build
./gradlew bootRun
```

3. **Prueba el cambio de idioma** agregando `?lang=en` o `?lang=es` a cualquier URL:
```
http://localhost:8080/?lang=es
http://localhost:8080/?lang=en
```

**Nota**: Los textos de la UI aún están hardcodeados en los templates, pero la configuración i18n ya funciona.

### Opción 2: Aplicar cambios mínimos para ver TODO funcionando

Necesitas actualizar 3 archivos. Aquí están los cambios:

## 📝 Archivos a Actualizar

### 1. ExamController.java

**Ubicación**: `src/main/java/co/singularit/az104simulator/controller/ExamController.java`

**Agregar imports** (después de línea 12):
```java
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;
```

**Reemplazar método `showQuestion`** (líneas ~44-62):
```java
@GetMapping("/{attemptId}/question/{index}")
public String showQuestion(
        @PathVariable String attemptId,
        @PathVariable int index,
        Model model) {

    Attempt attempt = attemptService.getAttempt(attemptId);
    ExamConfigDto config = attemptService.getAttemptConfig(attemptId);

    if (index < 0 || index >= attempt.getTotalQuestions()) {
        return "redirect:/attempt/" + attemptId + "/question/0";
    }

    // Obtener locale actual
    Locale locale = LocaleContextHolder.getLocale();
    String lang = locale.getLanguage();

    QuestionDto question = attemptService.getQuestionForAttempt(attemptId, index, attempt.getMode(), lang);
    Map<String, Object> status = attemptService.getAttemptStatus(attemptId);

    model.addAttribute("attempt", attempt);
    model.addAttribute("question", question);
    model.addAttribute("currentIndex", index);
    model.addAttribute("status", status);
    model.addAttribute("config", config);

    return "exam";
}
```

### 2. AttemptService.java

**Ubicación**: `src/main/java/co/singularit/az104simulator/service/AttemptService.java`

**Reemplazar método `getQuestionForAttempt`** (busca este método y reemplázalo):

```java
@Transactional(readOnly = true)
public QuestionDto getQuestionForAttempt(String attemptId, int index, ExamMode mode, String lang) {
    Attempt attempt = getAttempt(attemptId);
    List<AttemptAnswer> answers = attempt.getAnswers();

    if (index < 0 || index >= answers.size()) {
        throw new IllegalArgumentException("Invalid question index: " + index);
    }

    AttemptAnswer answer = answers.get(index);
    Long questionId = answer.getQuestionId();

    Question question = questionService.getRandomQuestions(List.of(Domain.values()), 1000).stream()
            .filter(q -> q.getId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

    boolean includeCorrectAnswers = (mode == ExamMode.PRACTICE);
    QuestionDto dto = questionService.convertToDto(question, includeCorrectAnswers, lang);

    // Add user's previous selection
    if (answer.getSelectedOptionIdsJson() != null && !answer.getSelectedOptionIdsJson().isEmpty()) {
        try {
            List<Long> selectedIds = objectMapper.readValue(
                    answer.getSelectedOptionIdsJson(),
                    new TypeReference<List<Long>>() {}
            );
            dto.setSelectedOptionIds(selectedIds);
            dto.setAnswered(true);
        } catch (Exception e) {
            log.error("Failed to parse selected options", e);
            dto.setSelectedOptionIds(new ArrayList<>());
            dto.setAnswered(false);
        }
    } else {
        dto.setSelectedOptionIds(new ArrayList<>());
        dto.setAnswered(false);
    }

    dto.setMarked(answer.getMarked());

    return dto;
}
```

**Si el método original no tiene parámetro `lang`**, agrégalo al final.

### 3. home.html - Agregar Selector de Idioma

**Ubicación**: `src/main/resources/templates/home.html`

**En el header** (después de `<h2>`), agregar:

```html
<div class="lang-selector" style="position: absolute; top: 20px; right: 20px;">
    <select onchange="window.location.href='/?lang='+this.value"
            style="padding: 8px 12px; border-radius: 5px; background: white; color: #0078d4; font-weight: 600; border: 2px solid white;">
        <option value="es">🇪🇸 Español</option>
        <option value="en">🇬🇧 English</option>
    </select>
</div>
```

## ✅ Después de Aplicar los Cambios

1. **En IntelliJ**:
   - Build → Rebuild Project
   - Run → Run 'Az104SimulatorApplication'

2. **Abre**: http://localhost:8080

3. **Prueba**:
   - Selector de idioma en home (arriba a la derecha)
   - Inicia un examen
   - Cambia idioma con `?lang=en` en la URL
   - Las preguntas deberían mostrarse (inicialmente duplicadas ES/EN)

## 🎨 Para Ver Estados Visuales del Sidebar

El CSS ya está listo, pero necesitas actualizar `exam.html` para aplicar las clases dinámicamente.

**Archivo completo actualizado** disponible en: `IMPLEMENTATION_GUIDE.md` sección 4.

## 🆘 Si Algo No Funciona

1. **Verifica logs en IntelliJ** (pestaña Run)
2. **Revisa que V3 migration se ejecutó**:
   - Abre: http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:mem:az104db`
   - Username: `sa`, Password: (vacío)
   - Query: `SELECT * FROM flyway_schema_history`
   - Deberías ver V1 y V3

3. **Limpia cache de IntelliJ**:
   - File → Invalidate Caches → Invalidate and Restart

## 📞 Siguiente Paso

Una vez que veas que el selector de idioma funciona, podemos continuar con los estados visuales del sidebar.
