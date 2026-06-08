-- Migración: ampliar archivo_tipo_mime en normativas de varchar(50) a varchar(255)
-- Necesario para soportar tipos MIME de archivos Word (.docx)
ALTER TABLE normativas
    MODIFY COLUMN archivo_tipo_mime VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL;
