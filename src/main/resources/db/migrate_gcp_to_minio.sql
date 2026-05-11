-- ============================================================
-- Migración: reemplazar URLs de GCP Storage por URLs de MinIO
-- Ejecutar UNA SOLA VEZ contra la BD live cuando MinIO sea el
-- almacenamiento activo y los archivos ya existan en el bucket.
-- ============================================================

-- IMPORTANTE: los archivos referenciados deben existir en el
-- bucket MinIO (reciclame-docs) antes de ejecutar este script.
-- Si los archivos no fueron migrados de GCP a MinIO, el visor
-- mostrará error al intentar abrirlos.

UPDATE estudios
SET archivo_url = REPLACE(
        archivo_url,
        'https://storage.googleapis.com/reciclame-docs/',
        'http://localhost:9000/reciclame-docs/'
    )
WHERE archivo_url LIKE 'https://storage.googleapis.com/reciclame-docs/%';

UPDATE normativas
SET archivo_url = REPLACE(
        archivo_url,
        'https://storage.googleapis.com/reciclame-docs/',
        'http://localhost:9000/reciclame-docs/'
    )
WHERE archivo_url LIKE 'https://storage.googleapis.com/reciclame-docs/%';

-- Verificación: mostrar registros actualizados
SELECT 'estudios' AS tabla, id, titulo, archivo_url
FROM estudios
WHERE archivo_url LIKE 'http://localhost:9000/reciclame-docs/%'
UNION ALL
SELECT 'normativas', id, titulo, archivo_url
FROM normativas
WHERE archivo_url LIKE 'http://localhost:9000/reciclame-docs/%';
