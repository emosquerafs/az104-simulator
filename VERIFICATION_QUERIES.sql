-- ============================================================================
-- QUERIES DE VERIFICACIÓN PARA BUG DE REPETICIÓN DE PREGUNTAS
-- ============================================================================
-- Ejecuta estas queries en H2 Console (http://localhost:8080/h2-console)
-- o mediante cualquier cliente SQL para verificar la integridad de los datos.
--
-- OBJETIVO: Garantizar que en un Attempt/sesión nunca se repite una pregunta
-- y que el orden sea estable de principio a fin.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DETECTAR DUPLICADOS POR ATTEMPT (attempt_id, question_id)
-- ----------------------------------------------------------------------------
-- Esta query busca attempts que tengan la misma pregunta más de una vez.
-- RESULTADO ESPERADO: 0 rows (ningún duplicado)
--
SELECT
    attempt_id,
    question_id,
    COUNT(*) AS times_repeated
FROM attempt_answer
GROUP BY attempt_id, question_id
HAVING COUNT(*) > 1
ORDER BY times_repeated DESC, attempt_id, question_id;

-- Si retorna filas: HAY DUPLICADOS - el bug persiste
-- Si retorna 0 filas: ✓ Correcto - no hay preguntas duplicadas


-- ----------------------------------------------------------------------------
-- 2. DETECTAR DUPLICADOS DE POSICIÓN (attempt_id, position)
-- ----------------------------------------------------------------------------
-- Esta query busca attempts donde dos preguntas tengan la misma posición.
-- RESULTADO ESPERADO: 0 rows (cada posición es única por attempt)
--
SELECT
    attempt_id,
    position,
    COUNT(*) AS questions_at_same_position
FROM attempt_answer
GROUP BY attempt_id, position
HAVING COUNT(*) > 1
ORDER BY questions_at_same_position DESC, attempt_id, position;

-- Si retorna filas: HAY DUPLICADOS DE POSICIÓN - orden corrupto
-- Si retorna 0 filas: ✓ Correcto - cada posición es única


-- ----------------------------------------------------------------------------
-- 3. VALIDAR INTEGRIDAD COMPLETA POR ATTEMPT
-- ----------------------------------------------------------------------------
-- Compara el total de registros vs. preguntas únicas y posiciones únicas.
-- RESULTADO ESPERADO: Todas las columnas deben ser iguales
--
SELECT
    attempt_id,
    COUNT(*) AS total_records,
    COUNT(DISTINCT question_id) AS distinct_questions,
    COUNT(DISTINCT position) AS distinct_positions,
    CASE
        WHEN COUNT(*) = COUNT(DISTINCT question_id)
         AND COUNT(*) = COUNT(DISTINCT position)
        THEN '✓ OK'
        ELSE '✗ DUPLICATES DETECTED'
    END AS status
FROM attempt_answer
GROUP BY attempt_id
ORDER BY attempt_id;

-- Columna 'status':
-- ✓ OK: Attempt válido sin duplicados
-- ✗ DUPLICATES DETECTED: Attempt tiene duplicados - BUG


-- ----------------------------------------------------------------------------
-- 4. VER PREGUNTAS POR ORDEN EN UN ATTEMPT ESPECÍFICO
-- ----------------------------------------------------------------------------
-- Reemplaza '<ATTEMPT_ID>' con el ID real del attempt que quieres verificar.
-- RESULTADO ESPERADO: positions secuenciales 0, 1, 2, ..., N-1
--
SELECT
    aa.position,
    aa.question_id,
    q.stem_es AS question_text,
    aa.marked,
    CASE
        WHEN aa.selected_option_ids_json IS NOT NULL THEN 'Answered'
        ELSE 'Unanswered'
    END AS answer_status
FROM attempt_answer aa
LEFT JOIN question q ON aa.question_id = q.id
WHERE aa.attempt_id = '<ATTEMPT_ID>'
ORDER BY aa.position ASC;

-- Verifica:
-- 1. Positions son secuenciales: 0, 1, 2, ..., N-1 (sin huecos)
-- 2. Ningún question_id se repite
-- 3. El orden es estable (ejecuta múltiples veces, debe retornar mismo orden)


-- ----------------------------------------------------------------------------
-- 5. DETECTAR HUECOS EN POSICIONES (gaps in position sequence)
-- ----------------------------------------------------------------------------
-- Busca attempts donde las posiciones no sean secuenciales desde 0.
-- RESULTADO ESPERADO: 0 rows (todas las secuencias son completas)
--
WITH position_check AS (
    SELECT
        attempt_id,
        position,
        ROW_NUMBER() OVER (PARTITION BY attempt_id ORDER BY position) - 1 AS expected_position
    FROM attempt_answer
)
SELECT
    attempt_id,
    position AS actual_position,
    expected_position,
    (position - expected_position) AS gap
FROM position_check
WHERE position != expected_position
ORDER BY attempt_id, position;

-- Si retorna filas: HAY HUECOS en las posiciones - secuencia corrupta
-- Si retorna 0 filas: ✓ Correcto - posiciones secuenciales sin huecos


-- ----------------------------------------------------------------------------
-- 6. VERIFICAR CONSTRAINTS ÚNICOS (requiere H2 metadata)
-- ----------------------------------------------------------------------------
-- Lista los constraints únicos en attempt_answer para confirmar que existen.
-- RESULTADO ESPERADO: Debe aparecer ux_attempt_answer_attempt_position y
--                     ux_attempt_answer_attempt_question
--
SELECT
    constraint_name,
    column_list
FROM INFORMATION_SCHEMA.INDEXES
WHERE table_name = 'ATTEMPT_ANSWER'
  AND index_type_name = 'UNIQUE'
ORDER BY constraint_name;

-- Debe incluir:
-- ux_attempt_answer_attempt_position  -> (attempt_id, position)
-- ux_attempt_answer_attempt_question  -> (attempt_id, question_id)


-- ----------------------------------------------------------------------------
-- 7. ESTADÍSTICAS GENERALES (overview de todos los attempts)
-- ----------------------------------------------------------------------------
-- Muestra un resumen de todos los attempts activos y completados.
--
SELECT
    a.id AS attempt_id,
    a.mode,
    a.total_questions,
    a.is_completed,
    COUNT(aa.id) AS actual_answers,
    COUNT(DISTINCT aa.question_id) AS unique_questions,
    COUNT(DISTINCT aa.position) AS unique_positions,
    CASE
        WHEN COUNT(aa.id) = a.total_questions
         AND COUNT(DISTINCT aa.question_id) = a.total_questions
         AND COUNT(DISTINCT aa.position) = a.total_questions
        THEN '✓ Valid'
        ELSE '✗ Invalid'
    END AS integrity_status
FROM attempt a
LEFT JOIN attempt_answer aa ON a.id = aa.attempt_id
GROUP BY a.id, a.mode, a.total_questions, a.is_completed
ORDER BY a.started_at DESC;

-- Columna 'integrity_status':
-- ✓ Valid: Attempt tiene la cantidad correcta de preguntas únicas
-- ✗ Invalid: Attempt tiene inconsistencias - investigar


-- ----------------------------------------------------------------------------
-- 8. VALIDACIÓN RÁPIDA: ¿Existen attempts con problemas?
-- ----------------------------------------------------------------------------
-- Query simple para chequeo rápido de salud del sistema.
-- RESULTADO ESPERADO: 0
--
SELECT COUNT(*) AS problematic_attempts
FROM (
    SELECT
        attempt_id,
        COUNT(*) AS total,
        COUNT(DISTINCT question_id) AS unique_q,
        COUNT(DISTINCT position) AS unique_p
    FROM attempt_answer
    GROUP BY attempt_id
    HAVING COUNT(*) != COUNT(DISTINCT question_id)
        OR COUNT(*) != COUNT(DISTINCT position)
) AS bad_attempts;

-- Si retorna 0: ✓ Sistema saludable - no hay duplicados
-- Si retorna > 0: ✗ Hay attempts con duplicados - ejecutar queries 1-4 para detalles


-- ============================================================================
-- NOTAS FINALES
-- ============================================================================
--
-- 1. Ejecuta query #8 primero como chequeo rápido de salud
-- 2. Si query #8 retorna > 0, ejecuta queries #1, #2, #3 para investigar
-- 3. Para verificar un attempt específico, usa query #4 con el ID real
-- 4. Query #6 confirma que los constraints de BD están activos
-- 5. Query #7 da un overview general del sistema
--
-- ============================================================================
