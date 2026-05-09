-- Tabla para tokens de recuperación de contraseña
-- Ejecutar en reciclame_db antes de levantar la aplicación
USE reciclame_db;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(255)  NOT NULL UNIQUE,
    usuario_id BIGINT        NOT NULL,
    expires_at DATETIME(6)   NOT NULL,
    usado      TINYINT(1)    NOT NULL DEFAULT 0,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_prt_token   (token),
    INDEX idx_prt_usuario (usuario_id),
    INDEX idx_prt_expires (expires_at),

    CONSTRAINT fk_prt_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
