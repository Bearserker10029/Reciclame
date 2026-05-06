-- Mi Unidad (carpetas personales por socio). Ejecutar en reciclame_db si aún no existen las tablas.
USE reciclame_db;

CREATE TABLE IF NOT EXISTS unidad_carpetas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  nombre VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_unidad_carpeta_usuario (usuario_id),
  KEY idx_unidad_carpeta_parent (parent_id),
  CONSTRAINT fk_unidad_carpeta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
  CONSTRAINT fk_unidad_carpeta_parent FOREIGN KEY (parent_id) REFERENCES unidad_carpetas (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS unidad_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  carpeta_id BIGINT DEFAULT NULL,
  documento_tipo VARCHAR(20) NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
  documento_id BIGINT NOT NULL,
  titulo_presentacion VARCHAR(500) DEFAULT NULL COMMENT 'Texto mostrado al usuario (ej. desde la tabla normativo/estudios)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_unidad_item_usuario (usuario_id),
  KEY idx_unidad_item_carpeta (carpeta_id),
  KEY idx_unidad_item_doc (documento_tipo, documento_id),
  CONSTRAINT fk_unidad_item_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
  CONSTRAINT fk_unidad_item_carpeta FOREIGN KEY (carpeta_id) REFERENCES unidad_carpetas (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Si la tabla ya existía sin titulo_presentacion:
-- ALTER TABLE unidad_items ADD COLUMN titulo_presentacion VARCHAR(500) NULL COMMENT 'Texto mostrado al usuario' AFTER documento_id;


ALTER TABLE unidad_items
ADD COLUMN titulo_presentacion VARCHAR(500) NULL
COMMENT 'Texto mostrado al usuario (ej. desde la tabla normativo/estudios)'
AFTER documento_id;