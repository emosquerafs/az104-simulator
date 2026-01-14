# Admin & Question Bank

This document explains how to manage the question bank, import/export questions, and maintain question quality.

## Question Structure

Each question in the database has:

### Core Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `domain` | Enum | Azure domain | `COMPUTE`, `NETWORKING`, `STORAGE`, `MONITORING`, `IDENTITY`, `GOVERNANCE` |
| `difficulty` | Enum | Question difficulty | `EASY`, `MEDIUM`, `HARD` |
| `qtype` | Enum | Question type | `SINGLE` (one correct), `MULTI` (multiple correct), `YESNO` (True/False) |
| `tags_json` | JSON | Searchable keywords | `["VMs", "Scale Sets", "Availability"]` |

### Bilingual Content

Questions support both English and Spanish through nested objects:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `stem` | Object | Question text with `en` and `es` properties | `{"en": "What is...", "es": "¿Qué es..."}` |
| `explanation` | Object | Answer explanation with `en` and `es` properties | `{"en": "Because...", "es": "Porque..."}` |
| `options[].text` | Object | Option text with `en` and `es` properties | `{"en": "Virtual Machine", "es": "Máquina Virtual"}` |

**JSON Structure**:
```json
{
  "stem": {
    "en": "English question text",
    "es": "Texto de pregunta en español"
  },
  "explanation": {
    "en": "English explanation",
    "es": "Explicación en español"
  },
  "options": [
    {
      "label": "A",
      "text": {
        "en": "English option",
        "es": "Opción en español"
      },
      "isCorrect": true
    }
  ]
}
```

### Options

Each question has 2-6 options (typically 4):

| Field | Type | Purpose |
|-------|------|---------|
| `label` | String | Option identifier (`A`, `B`, `C`, `D`) |
| `text` | Object | Bilingual option text with `en` and `es` properties |
| `isCorrect` | Boolean | Whether this option is correct |

**Example**:
```json
{
  "label": "A",
  "text": {
    "en": "Virtual Machine",
    "es": "Máquina Virtual"
  },
  "isCorrect": true
}
```

## Question Types

### SINGLE (Single Choice)

- **Exactly one** correct answer
- Rendered as **radio buttons**
- Most common type (70% of questions)

**Example**:
```
Which Azure service provides serverless compute?
A. Virtual Machines
B. Azure Functions ✓
C. Azure Storage
D. Azure SQL Database
```

### MULTI (Multiple Choice)

- **One or more** correct answers
- Rendered as **checkboxes**
- Requires selecting all correct options (no partial credit)

**Example**:
```
Which services support geo-replication? (Select all that apply)
A. Azure SQL Database ✓
B. Blob Storage ✓
C. Azure Functions
D. Cosmos DB ✓
```

### YESNO (True/False)

- **Two options**: Yes/No or True/False
- Rendered as **radio buttons**
- Simplest question type

**Example**:
```
Azure VMs can be resized without deallocation.
A. Yes
B. No ✓
```

---

## JSON Format

### Complete Example (Current Format)

This is the **correct format** used by the application. Note the nested objects for bilingual content:

```json
{
  "domain": "MONITOR_MAINTAIN",
  "difficulty": "HARD",
  "qtype": "SINGLE",
  "stem": {
    "en": "After enabling diagnostic settings to send logs to Log Analytics, your KQL query returns no results. You verify that the resource is generating events. Which is the most likely explanation?",
    "es": "Después de habilitar diagnostic settings para enviar logs a Log Analytics, tu consulta KQL no devuelve resultados. Verificas que el recurso está generando eventos. ¿Cuál es la explicación más probable?"
  },
  "explanation": {
    "en": "The wrong log category or destination was selected in diagnostic settings (or the resource doesn't support that category to LA). A top distractor is 'KQL is incorrect'; even with a simple query, you should see some data if ingestion is correct.",
    "es": "Se seleccionó la categoría de logs equivocada o el destino incorrecto en diagnostic settings (o el recurso no soporta esa categoría hacia LA). Un distractor es 'KQL incorrecto'; incluso con una consulta simple deberías ver datos si la ingesta es correcta."
  },
  "options": [
    {
      "label": "A",
      "text": {
        "en": "The diagnostic settings are missing the correct log categories or target destination",
        "es": "Faltan las categorías correctas o el destino correcto en diagnostic settings"
      },
      "isCorrect": true
    },
    {
      "label": "B",
      "text": {
        "en": "A resource lock blocks log ingestion",
        "es": "Un bloqueo de recursos bloquea la ingesta de logs"
      },
      "isCorrect": false
    },
    {
      "label": "C",
      "text": {
        "en": "Service Health disables KQL queries during maintenance",
        "es": "Service Health deshabilita consultas KQL durante mantenimiento"
      },
      "isCorrect": false
    },
    {
      "label": "D",
      "text": {
        "en": "Metrics must be disabled to enable logs",
        "es": "Se deben deshabilitar métricas para habilitar logs"
      },
      "isCorrect": false
    }
  ],
  "tags": [
    "diagnostic-settings",
    "log-analytics",
    "kql",
    "troubleshooting"
  ]
}
```

**Key Points**:
- `stem`, `explanation`, and option `text` are **objects** with `en` and `es` properties
- NOT flat fields like `stem_en` / `stem_es` (legacy format in database, but JSON import uses objects)
- `tags` is a simple JSON array of strings

**Important**: The JSON import format uses **nested objects** (`stem: {en, es}`), but the database stores them as **separate columns** (`stem_en`, `stem_es`). The `AdminController` handles the conversion automatically during import/export.

### Minimal Example (Legacy)

```json
{
  "domain": "COMPUTE",
  "difficulty": "MEDIUM",
  "qtype": "SINGLE",
  "stem": "What is a Virtual Machine?",
  "explanation": "A VM is a virtualized compute resource...",
  "tags": ["VMs", "Compute"],
  "options": [
    {
      "label": "A",
      "text": "A physical server",
      "isCorrect": false
    },
    {
      "label": "B",
      "text": "A virtualized compute resource",
      "isCorrect": true
    }
  ]
}
```

**Note**: This legacy format uses flat strings. For bilingual content, use the nested object format below.

### Bilingual Example (Recommended)

```json
{
  "domain": "STORAGE",
  "difficulty": "EASY",
  "qtype": "SINGLE",
  "stem": {
    "en": "Which Azure service is optimized for unstructured data?",
    "es": "¿Qué servicio de Azure está optimizado para datos no estructurados?"
  },
  "explanation": {
    "en": "Blob Storage is designed for unstructured data like images, videos, and backups.",
    "es": "Blob Storage está diseñado para datos no estructurados como imágenes, videos y copias de seguridad."
  },
  "options": [
    {
      "label": "A",
      "text": {
        "en": "Azure SQL Database",
        "es": "Azure SQL Database"
      },
      "isCorrect": false
    },
    {
      "label": "B",
      "text": {
        "en": "Blob Storage",
        "es": "Blob Storage"
      },
      "isCorrect": true
    },
    {
      "label": "C",
      "text": {
        "en": "Azure Table Storage",
        "es": "Azure Table Storage"
      },
      "isCorrect": false
    },
    {
      "label": "D",
      "text": {
        "en": "Cosmos DB",
        "es": "Cosmos DB"
      },
      "isCorrect": false
    }
  ],
  "tags": ["Storage", "Blobs", "Unstructured Data"]
}
```

### Multiple Correct Answers

```json
{
  "domain": "NETWORKING",
  "difficulty": "HARD",
  "qtype": "MULTI",
  "stem": {
    "en": "Which Azure services provide DDoS protection? (Select all that apply)",
    "es": "¿Qué servicios de Azure proporcionan protección DDoS? (Seleccione todos los que apliquen)"
  },
  "explanation": {
    "en": "Azure DDoS Protection Standard and Azure Front Door both provide DDoS mitigation.",
    "es": "Azure DDoS Protection Standard y Azure Front Door proporcionan mitigación DDoS."
  },
  "options": [
    {
      "label": "A",
      "text": {
        "en": "Azure Firewall",
        "es": "Azure Firewall"
      },
      "isCorrect": false
    },
    {
      "label": "B",
      "text": {
        "en": "DDoS Protection Standard",
        "es": "DDoS Protection Standard"
      },
      "isCorrect": true
    },
    {
      "label": "C",
      "text": {
        "en": "Azure Front Door",
        "es": "Azure Front Door"
      },
      "isCorrect": true
    },
    {
      "label": "D",
      "text": {
        "en": "Network Security Group",
        "es": "Network Security Group"
      },
      "isCorrect": false
    }
  ],
  "tags": ["Networking", "Security", "DDoS"]
}
```

---

## Import Process

### 1. Prepare JSON File

Create or edit `questions.json` with an array of questions:

```json
[
  { /* question 1 */ },
  { /* question 2 */ },
  ...
]
```

### 2. Verify File Encoding

**Critical**: File must be UTF-8 encoded.

**Check encoding** (macOS/Linux):
```bash
file -I questions.json
# Should output: charset=utf-8
```

**Windows** (PowerShell):
```powershell
Get-Content questions.json | Get-FileEncoding
# Should output: UTF8
```

**Fix encoding** if needed:
```bash
iconv -f ISO-8859-1 -t UTF-8 questions.json > questions_utf8.json
mv questions_utf8.json questions.json
```

### 3. Import via API

**Start the application**:
```bash
./gradlew bootRun
```

**Import via curl**:
```bash
curl -X POST http://localhost:8080/admin/import \
  -H "Content-Type: multipart/form-data" \
  -F "file=@questions.json"
```

**Response**:
```json
{
  "success": true,
  "message": "Successfully imported 42 questions"
}
```

### 4. Verify Import

**Check H2 Console**:
```sql
SELECT COUNT(*) FROM question;
SELECT domain, COUNT(*) FROM question GROUP BY domain;
```

**Or via application**: Visit http://localhost:8080/ to see question counts.

---

## Export Process

### Export All Questions

```bash
curl http://localhost:8080/admin/export -o questions_backup.json
```

This creates a JSON file with all questions in the database.

### Use Cases

- **Backup**: Before making changes
- **Migration**: Move questions to another environment
- **Version Control**: Track question changes in Git
- **Collaboration**: Share question bank with team

---

## Validation & Quality Control

### Automated Validation

The import endpoint validates:

1. **Required Fields**: `domain`, `difficulty`, `qtype`, `options`
2. **Enum Values**: Domain/difficulty/qtype must match defined enums
3. **Option Correctness**:
   - `SINGLE`/`YESNO`: Exactly one correct option
   - `MULTI`: At least one correct option
4. **Option Labels**: No duplicates (`A`, `B`, `C`, `D`)

**Example Error**:
```json
{
  "success": false,
  "message": "Question has no correct answer at line 42"
}
```

### Manual Quality Checks

Before importing, verify:

#### ✅ Content Quality

- [ ] Question is clear and unambiguous
- [ ] No typos or grammatical errors
- [ ] Technical accuracy verified
- [ ] Both languages are complete (if bilingual)
- [ ] Explanation teaches the concept (not just states the answer)

#### ✅ Answer Quality

- [ ] Correct answer is actually correct
- [ ] Distractors (wrong answers) are plausible
- [ ] No "all of the above" or "none of the above" (unless necessary)
- [ ] Options are roughly same length (avoid bias)

#### ✅ Technical Accuracy

- [ ] Uses current Azure terminology
- [ ] No deprecated services (e.g., Classic VMs)
- [ ] Pricing info is generic (prices change)
- [ ] No region-specific details (unless stated in question)

#### ✅ Fairness

- [ ] Not a trick question (unless testing edge cases)
- [ ] No cultural bias
- [ ] Vocabulary appropriate for target audience

### Distractor Guidelines

**Good Distractors** (wrong answers that look plausible):

- Related services with similar names
- Services from the same domain but wrong use case
- Common misconceptions

**Bad Distractors**:

- Completely unrelated services
- Obviously wrong answers
- Services that don't exist

**Example**:

❌ **Bad**:
```
Which service provides serverless compute?
A. Azure Functions
B. Microsoft Word
C. Google Cloud
D. Kubernetes
```

✅ **Good**:
```
Which service provides serverless compute?
A. Azure Functions (correct)
B. Azure App Service (related but not serverless)
C. Azure Container Instances (related but not serverless)
D. Azure Virtual Machines (related but not serverless)
```

---

## Best Practices

### 1. Version Control Questions

Track questions in Git:

```bash
# Export from DB
curl http://localhost:8080/admin/export -o src/main/resources/seed/questions.json

# Commit
git add src/main/resources/seed/questions.json
git commit -m "Add 10 new Networking questions"
```

### 2. Avoid Encoding Issues

**Always**:
- Save files as UTF-8 without BOM
- Use direct Unicode characters (é, ñ, ü) not HTML entities
- Test import on sample file first

**Example** of correct characters:
```json
{
  "stem_es": "¿Qué es Azure Active Directory?",
  "explanation_es": "Azure AD es un servicio de gestión de identidades..."
}
```

### 3. Keep Questions Balanced

Target distribution:

| Dimension | Target |
|-----------|--------|
| **Domain** | Proportional to real exam (Compute ~20%, Networking ~15%, etc.) |
| **Difficulty** | 30% Easy, 50% Medium, 20% Hard |
| **Type** | 70% Single, 25% Multi, 5% YesNo |

### 4. Write Explanations

Good explanations:

- **Explain why** the correct answer is right
- **Explain why** wrong answers are wrong
- **Provide context** (when to use X vs Y)
- **Link to docs** (optional but helpful)

**Example**:

❌ **Bad Explanation**:
```
"B is correct because it's the right answer."
```

✅ **Good Explanation**:
```
"Azure Functions is the correct answer because it provides serverless compute 
that scales automatically and you only pay for execution time. App Service 
requires a hosting plan (always running), Container Instances run continuously, 
and VMs require manual scaling."
```

### 5. Tag Thoroughly

Good tags help users filter questions:

```json
{
  "tags": ["VMs", "Availability Sets", "Fault Domains", "Update Domains", "High Availability"]
}
```

Future features might allow filtering by tags.

### 6. Avoid Outdated Content

**Don't hardcode**:
- Specific pricing ("VMs cost $100/month")
- Version numbers ("Python 2.7")
- Deprecated services ("Classic VMs")
- Preview features (unless clearly stated)

**Instead**:
- Use general terms ("VMs are billed hourly")
- Use current versions ("Python 3.x")
- Use current services ("Azure VMs")
- Mark preview features clearly ("Preview: Azure X")

### 7. Test Your Questions

Before importing 100 questions:

1. Import 5 sample questions
2. Start a practice session
3. Answer questions
4. Check:
   - Text renders correctly
   - Options display properly
   - Correct answer is actually correct
   - Explanation makes sense
5. Fix issues
6. Import remaining questions

---

## Bulk Operations

### Update Existing Questions

Currently not supported via UI. Options:

1. **Export → Edit → Delete All → Import**:
   ```bash
   curl http://localhost:8080/admin/export -o questions.json
   # Edit questions.json
   # Manually delete questions from H2 Console
   curl -X POST http://localhost:8080/admin/import -F "file=@questions.json"
   ```

2. **Direct SQL** (H2 Console):
   ```sql
   UPDATE question SET difficulty = 'HARD' WHERE id = 123;
   ```

### Delete All Questions

**H2 Console**:
```sql
DELETE FROM exam_session_question;
DELETE FROM exam_session;
DELETE FROM option_item;
DELETE FROM question;
```

**Warning**: This is irreversible. Export first!

---

## Seeding Initial Questions

### Automatic Seeding (Optional)

To load questions on first startup:

1. Place `questions.json` in `src/main/resources/seed/`
2. Create `DataSeeder` component:

```java
@Component
public class DataSeeder implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Check if questions exist
        if (questionRepository.count() == 0) {
            // Load from src/main/resources/seed/questions.json
            // Parse and save
        }
    }
}
```

Currently, seeding is manual via `/admin/import`.

---

## Question Bank Statistics

### Count by Domain

**SQL**:
```sql
SELECT domain, COUNT(*) as count 
FROM question 
GROUP BY domain 
ORDER BY count DESC;
```

### Count by Difficulty

**SQL**:
```sql
SELECT difficulty, COUNT(*) as count 
FROM question 
GROUP BY difficulty;
```

### Questions Without Bilingual Content

**SQL**:
```sql
SELECT id, domain 
FROM question 
WHERE stem_en IS NULL OR stem_es IS NULL;
```

---

## Troubleshooting

### Import Fails with "Invalid JSON"

**Cause**: Malformed JSON

**Solution**:
1. Validate JSON: https://jsonlint.com/
2. Check for:
   - Missing commas
   - Trailing commas
   - Unescaped quotes
   - Unicode issues

### Accents Show as "�"

**Cause**: Encoding mismatch

**Solution**:
1. Ensure file is UTF-8: `file -I questions.json`
2. If not, convert: `iconv -f ISO-8859-1 -t UTF-8 input.json > output.json`
3. Check browser encoding (should be UTF-8)

### Questions Appear in Wrong Language

**Cause**: Missing bilingual columns

**Solution**:
- Use `stem_en`/`stem_es` not `stem`
- Import uses legacy columns if new ones are empty

### Duplicate Questions After Import

**Cause**: No deduplication logic

**Solution**:
- Currently, imports always create new questions
- Delete duplicates manually via H2 Console:
  ```sql
  DELETE FROM question WHERE id IN (
    SELECT MAX(id) FROM question GROUP BY stem_en HAVING COUNT(*) > 1
  );
  ```

---

## Future Enhancements

Planned features:

- [ ] Web UI for question editing (no JSON needed)
- [ ] Bulk update via CSV
- [ ] Duplicate detection on import
- [ ] Question versioning (track changes)
- [ ] Community contributions (review workflow)
- [ ] AI-generated distractors (GPT-4 integration)

---

**Protip**: Keep a `questions_backup.json` file under version control. Export after every major change. Future you will thank present you when things break. 🙏

