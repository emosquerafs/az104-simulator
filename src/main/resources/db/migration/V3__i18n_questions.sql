-- Add bilingual columns to question table
ALTER TABLE question ADD COLUMN stem_es TEXT;
ALTER TABLE question ADD COLUMN stem_en TEXT;
ALTER TABLE question ADD COLUMN explanation_es TEXT;
ALTER TABLE question ADD COLUMN explanation_en TEXT;

-- Migrate existing data to both languages (initially duplicate)
UPDATE question SET stem_es = stem, stem_en = stem;
UPDATE question SET explanation_es = explanation, explanation_en = explanation;

-- Add bilingual columns to option_item table
ALTER TABLE option_item ADD COLUMN text_es TEXT;
ALTER TABLE option_item ADD COLUMN text_en TEXT;

-- Migrate existing data to both languages
UPDATE option_item SET text_es = text, text_en = text;

-- Keep original columns for backward compatibility (will be deprecated later)
-- stem, explanation, text columns remain but new code uses _es/_en variants
