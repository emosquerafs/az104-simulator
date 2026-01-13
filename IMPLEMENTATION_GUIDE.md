# Guía de Implementación - Mejoras UX + i18n

## ✅ Cambios Completados

1. **Configuración i18n** - `I18nConfig.java` creado
2. **Archivos de mensajes** - `messages_es.properties` y `messages_en.properties`
3. **Migración Flyway V3** - Columnas bilingües agregadas
4. **Entidades actualizadas** - Question y OptionItem con campos *_es y *_en
5. **QuestionService** - Método `convertToDto` con parámetro `lang`
6. **QuestionDataLoader** - Pobla columnas bilingües
7. **CSS** - Clases para estados (q-answered, q-marked, q-unanswered, status-badge)

## 🔧 Cambios Pendientes (Aplicar Manualmente)

### 1. Actualizar Attempt Service para Locale

**Archivo**: `src/main/java/co/singularit/az104simulator/service/AttemptService.java`

**Cambio en método `getQuestionForAttempt`**:

```java
@Transactional(readOnly = true)
public QuestionDto getQuestionForAttempt(String attemptId, int index, ExamMode mode, String lang) {
    // ... código existente ...

    boolean includeCorrectAnswers = (mode == ExamMode.PRACTICE);
    QuestionDto dto = questionService.convertToDto(question, includeCorrectAnswers, lang); // AGREGAR lang

    // ... resto del código ...
}
```

### 2. Actualizar ExamController para Locale

**Archivo**: `src/main/java/co/singularit/az104simulator/controller/ExamController.java`

**Agregar import**:
```java
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;
```

**Modificar método `showQuestion`**:

```java
@GetMapping("/{attemptId}/question/{index}")
public String showQuestion(@PathVariable String attemptId,
                          @PathVariable int index,
                          Model model) {
    Attempt attempt = attemptService.getAttempt(attemptId);
    ExamConfigDto config = attemptService.getAttemptConfig(attemptId);

    if (index < 0 || index >= attempt.getTotalQuestions()) {
        return "redirect:/attempt/" + attemptId + "/question/0";
    }

    // AGREGAR: Obtener locale actual
    Locale locale = LocaleContextHolder.getLocale();
    String lang = locale.getLanguage();

    QuestionDto question = attemptService.getQuestionForAttempt(attemptId, index, attempt.getMode(), lang);
    Map<String, Object> status = attemptService.getAttemptStatus(attemptId);

    // AGREGAR: Estados de preguntas para sidebar
    List<Map<String, Object>> questionStates = attemptService.getQuestionStates(attemptId);
    model.addAttribute("questionStates", questionStates);

    model.addAttribute("attempt", attempt);
    model.addAttribute("question", question);
    model.addAttribute("currentIndex", index);
    model.addAttribute("status", status);
    model.addAttribute("config", config);

    return "exam";
}
```

### 3. Agregar método `getQuestionStates` en AttemptService

**Archivo**: `src/main/java/co/singularit/az104simulator/service/AttemptService.java`

```java
@Transactional(readOnly = true)
public List<Map<String, Object>> getQuestionStates(String attemptId) {
    Attempt attempt = getAttempt(attemptId);
    List<AttemptAnswer> answers = attempt.getAnswers();
    List<Map<String, Object>> states = new ArrayList<>();

    for (int i = 0; i < answers.size(); i++) {
        AttemptAnswer answer = answers.get(i);
        Map<String, Object> state = new HashMap<>();
        state.put("index", i);
        state.put("questionId", answer.getQuestionId());

        boolean answered = (answer.getSelectedOptionIdsJson() != null &&
                           !answer.getSelectedOptionIdsJson().isEmpty());
        boolean marked = answer.getMarked();

        state.put("answered", answered);
        state.put("marked", marked);
        state.put("status", marked ? "marked" : (answered ? "answered" : "unanswered"));

        states.add(state);
    }

    return states;
}
```

### 4. Actualizar exam.html - Sidebar Dinámico

**Archivo**: `src/main/resources/templates/exam.html`

**Reemplazar el sidebar (líneas ~70-90)**:

```html
<aside class="sidebar">
    <h3 th:text="#{exam.questions}">Questions</h3>
    <div class="question-grid">
        <div th:each="qState, iterStat : ${questionStates}"
             class="question-number"
             th:classappend="${qState.index == currentIndex} ? 'current' : '' +
                           ' q-' + ${qState.status}">
            <button type="button" class="question-btn"
                    th:onclick="'navigateToQuestion(' + ${qState.index} + ')'">
                [[${qState.index + 1}]]
            </button>
        </div>
    </div>

    <div class="sidebar-actions">
        <a th:href="@{/attempt/{id}/review(id=${attempt.id})}"
           class="btn btn-secondary"
           th:text="#{exam.review.all}">
            Review All
        </a>
    </div>

    <div class="legend">
        <h4 th:text="#{exam.legend}">Legend:</h4>
        <div class="legend-item">
            <span class="legend-box answered"></span>
            <span th:text="#{exam.legend.answered}">Answered</span>
        </div>
        <div class="legend-item">
            <span class="legend-box marked"></span>
            <span th:text="#{exam.legend.marked}">Marked</span>
        </div>
        <div class="legend-item">
            <span class="legend-box unanswered"></span>
            <span th:text="#{exam.legend.unanswered}">Not Answered</span>
        </div>
    </div>
</aside>
```

### 5. Agregar Selector de Idioma en Header

**Archivo**: `src/main/resources/templates/exam.html`

**Agregar en header (después de header-left)**:

```html
<div class="header-center"
     th:if="${attempt.mode.name() == 'EXAM' && config.timeLimitMinutes != null}">
    <div class="timer" id="timer" th:attr="data-time-limit=${config.timeLimitMinutes * 60}">
        <span id="timer-display">--:--</span>
    </div>
</div>

<!-- AGREGAR AQUÍ -->
<div class="header-right">
    <div class="lang-selector">
        <select onchange="changeLanguage(this.value)"
                th:attr="data-current-url=${#httpServletRequest.requestURI}">
            <option value="es" th:selected="${#locale.language == 'es'}">🇪🇸 ES</option>
            <option value="en" th:selected="${#locale.language == 'en'}">🇬🇧 EN</option>
        </select>
    </div>
    <span class="progress-info">
        <span th:text="#{exam.answered}">Answered</span>: <strong th:text="${status.answeredCount}">0</strong> |
        <span th:text="#{exam.marked}">Marked</span>: <strong th:text="${status.markedCount}">0</strong>
    </span>
</div>
```

**Agregar JavaScript al final de exam.html**:

```html
<script th:inline="javascript">
// ... código existente ...

function changeLanguage(lang) {
    const currentUrl = window.location.pathname + window.location.search;
    const separator = currentUrl.includes('?') ? '&' : '?';
    window.location.href = currentUrl + separator + 'lang=' + lang;
}
</script>
```

### 6. Actualizar review.html con Estados y Filtros

**Archivo**: `src/main/resources/templates/review.html`

**Agregar después de review-summary**:

```html
<div class="review-filters">
    <button class="filter-btn active" onclick="filterQuestions('all')"
            th:text="#{review.filter.all}">
        View All
    </button>
    <button class="filter-btn" onclick="filterQuestions('unanswered')"
            th:text="#{review.filter.unanswered}">
        View Unanswered
    </button>
    <button class="filter-btn" onclick="filterQuestions('marked')"
            th:text="#{review.filter.marked}">
        View Marked
    </button>
</div>

<div class="question-list">
    <h3 th:text="#{review.all.questions}">All Questions</h3>
    <div class="question-grid-review">
        <div th:each="qState, iterStat : ${questionStates}"
             class="question-review-item"
             th:attr="data-status=${qState.status}">
            <a th:href="@{/attempt/{id}/question/{index}(id=${attempt.id}, index=${qState.index})}"
               class="question-review-link">
                <span class="question-number-badge">[[${qState.index + 1}]]</span>
                <span class="status-badge"
                      th:classappend="${qState.status}"
                      th:text="#{|review.status.${qState.status}|}">
                    Status
                </span>
            </a>
        </div>
    </div>
</div>

<script>
function filterQuestions(filter) {
    const items = document.querySelectorAll('.question-review-item');
    const buttons = document.querySelectorAll('.filter-btn');

    buttons.forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');

    items.forEach(item => {
        if (filter === 'all') {
            item.style.display = '';
        } else {
            item.style.display = item.dataset.status === filter ? '' : 'none';
        }
    });
}
</script>
```

### 7. Actualizar ReviewController

**Archivo**: `src/main/java/co/singularit/az104simulator/controller/ExamController.java`

**Modificar método `reviewAttempt`**:

```java
@GetMapping("/{attemptId}/review")
public String reviewAttempt(@PathVariable String attemptId, Model model) {
    Attempt attempt = attemptService.getAttempt(attemptId);
    Map<String, Object> status = attemptService.getAttemptStatus(attemptId);
    List<Map<String, Object>> questionStates = attemptService.getQuestionStates(attemptId); // AGREGAR

    model.addAttribute("attempt", attempt);
    model.addAttribute("status", status);
    model.addAttribute("questionStates", questionStates); // AGREGAR

    return "review";
}
```

### 8. Actualizar home.html con i18n

**Archivo**: `src/main/resources/templates/home.html`

Reemplazar todos los textos hardcodeados con `th:text="#{key}"`:

```html
<h1 th:text="#{app.title}">AZ-104 Microsoft Azure Administrator</h1>
<h2 th:text="#{app.subtitle}">Exam Simulator</h2>
<!-- etc... -->
```

### 9. Forzar Recarga de Sidebar tras Responder

**En exam.html, modificar función `saveAnswer()`**:

```javascript
function saveAnswer() {
    const form = document.getElementById('answer-form');
    const questionType = document.getElementById('questionType').value;
    let selectedOptionIds = [];

    if (questionType === 'SINGLE' || questionType === 'YESNO') {
        const selected = form.querySelector('input[name="selectedOption"]:checked');
        if (selected) {
            selectedOptionIds = [parseInt(selected.value)];
        }
    } else if (questionType === 'MULTI') {
        const selected = form.querySelectorAll('input[name="selectedOptions"]:checked');
        selectedOptionIds = Array.from(selected).map(el => parseInt(el.value));
    }

    const data = {
        questionId: questionId,
        selectedOptionIds: selectedOptionIds,
        marked: isMarked
    };

    fetch(`/attempt/${attemptId}/answer`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    }).then(() => {
        // AGREGAR: Recargar página para actualizar sidebar
        location.reload();
    });
}
```

## 🚀 Pasos para Aplicar

1. **Detener la aplicación** actualmente corriendo
2. **Aplicar cambios** de los archivos listados arriba
3. **Compilar** el proyecto: `./gradlew clean build`
4. **Ejecutar**: `./gradlew bootRun`
5. **Probar**:
   - Sidebar muestra estados en tiempo real (verde/amarillo/gris)
   - Selector de idioma cambia GUI y preguntas
   - Review screen muestra estados y filtros

## ✨ Resultado Final

- ✅ Sidebar con estados visuales en tiempo real
- ✅ Preguntas bilingües (ES/EN)
- ✅ GUI completamente internacionalizada
- ✅ Review screen con filtros y estados claros
- ✅ Selector de idioma persistente
- ✅ Sin romper funcionalidad existente

## 📝 Notas

- El idioma se guarda en cookie (30 días)
- Cambiar idioma NO reinicia el intento
- Las preguntas actuales están duplicadas en ES/EN (puedes editarlas individualmente en la BD)
- Para traducciones reales, edita las columnas *_es y *_en en H2 Console o vía admin/import

## 🐛 Si algo falla

1. Revisar logs: `tail -f app.log`
2. Verificar migración V3 se ejecutó: `SELECT * FROM flyway_schema_history`
3. Verificar columnas bilingües existen: `DESCRIBE question`
4. Limpiar y rebuild: `./gradlew clean build`
