-- ============================================================
-- RECICLAME - Repositorio Circular
-- DDL Script · MySQL 8.0 · Spring Boot
-- Version 3 - con ambito + clasificacion
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

DROP DATABASE IF EXISTS reciclame_db;
CREATE DATABASE reciclame_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE reciclame_db;

-- ────────────────────────────────────────────
-- 1. TABLAS DE CATALOGO (Lookup / Referencia)
-- ────────────────────────────────────────────

CREATE TABLE roles (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(30)   NOT NULL UNIQUE COMMENT 'SUPERADMIN, ADMINISTRADOR, SOCIO, VISUALIZADOR',
    descripcion   VARCHAR(255)  NULL,
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catalogo de roles del sistema';

INSERT INTO roles (nombre, descripcion) VALUES
('SUPERADMIN',     'Administrador supremo PUCP - auditoria y gestion global'),
('ADMINISTRADOR',  'Gestion de usuarios, documentos y reportes'),
('SOCIO',          'Acceso completo al repositorio - lectura y descarga'),
('VISUALIZADOR',   'Acceso limitado - solo lectura de documentos publicos');

CREATE TABLE tematicas (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(10)   NOT NULL UNIQUE COMMENT 'EC, GR, EE, REP, OTRO',
    nombre        VARCHAR(100)  NOT NULL,
    color_hex     VARCHAR(7)    NOT NULL DEFAULT '#95A5A6' COMMENT 'Color distintivo para la UI',
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Tematicas con color distintivo';

INSERT INTO tematicas (codigo, nombre, color_hex) VALUES
('EC',   'Economia Circular',                         '#27AE60'),
('GR',   'Gestion de Residuos',                       '#3498DB'),
('EE',   'Envases y Embalajes',                       '#F39C12'),
('REP',  'Responsabilidad Extendida del Productor',   '#E74C3C'),
('OTRO', 'Otros',                                     '#95A5A6');

CREATE TABLE tipos_normativa (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(60)   NOT NULL UNIQUE,
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO tipos_normativa (nombre) VALUES
('Ley'), ('Reglamento'), ('NTP / Norma tecnica'),
('Directiva'), ('Hoja de Ruta'), ('Resolucion');

CREATE TABLE estados_normativa (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(60)   NOT NULL UNIQUE,
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO estados_normativa (nombre) VALUES
('Vigente'), ('Proyecto publicado'), ('Proyecto / borrador en proceso'),
('Consulta publica'), ('Derogada');

-- ────────────────────────────────────────────
-- 2. EMPRESAS Y USUARIOS
-- ────────────────────────────────────────────

CREATE TABLE empresas (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    razon_social  VARCHAR(200)  NOT NULL,
    ruc           VARCHAR(11)   NULL UNIQUE,
    sector        VARCHAR(100)  NULL,
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_empresas_razon_social (razon_social)
) ENGINE=InnoDB COMMENT='Empresas socias de ReciclamE';

INSERT INTO empresas (razon_social, ruc, sector) VALUES
('ReciclamE S.A.C.',           '20512345678', 'Gestion ambiental'),
('Plasticos del Peru S.A.',    '20498765432', 'Plasticos PET'),
('Envases Tetra Peru',         '20587654321', 'Envases Tetra');

CREATE TABLE usuarios (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre_completo     VARCHAR(200)  NOT NULL,
    dni                 VARCHAR(8)    NOT NULL UNIQUE,
    correo_corporativo  VARCHAR(200)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255)  NOT NULL,
    cargo               VARCHAR(150)  NULL,
    foto_url            VARCHAR(500)  NULL,

    rol_id              BIGINT        NOT NULL,
    empresa_id          BIGINT        NULL,

    intentos_fallidos   TINYINT       NOT NULL DEFAULT 0,
    cuenta_bloqueada    BOOLEAN       NOT NULL DEFAULT FALSE,
    ip_bloqueada        VARCHAR(45)   NULL,
    tfa_activo          BOOLEAN       NOT NULL DEFAULT FALSE,
    tfa_secret          VARCHAR(64)   NULL,

    estado_cuenta       VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, ACTIVO, INACTIVO, BLOQUEADO',
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    ultimo_login        TIMESTAMP     NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuarios_rol     FOREIGN KEY (rol_id)     REFERENCES roles(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE SET NULL,

    INDEX idx_usuarios_correo   (correo_corporativo),
    INDEX idx_usuarios_dni      (dni),
    INDEX idx_usuarios_rol      (rol_id),
    INDEX idx_usuarios_empresa  (empresa_id),
    INDEX idx_usuarios_estado   (estado_cuenta)
) ENGINE=InnoDB COMMENT='Usuarios del sistema';

CREATE TABLE solicitudes_registro (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre_completo     VARCHAR(200)  NOT NULL,
    dni                 VARCHAR(8)    NOT NULL,
    correo_corporativo  VARCHAR(200)  NOT NULL,
    empresa_nombre      VARCHAR(200)  NOT NULL,

    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, APROBADA, RECHAZADA',
    revisada_por_id     BIGINT        NULL,
    motivo_rechazo      TEXT          NULL,
    fecha_resolucion    TIMESTAMP     NULL,

    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_solicitudes_revisor FOREIGN KEY (revisada_por_id) REFERENCES usuarios(id) ON DELETE SET NULL,

    INDEX idx_solicitudes_estado (estado),
    INDEX idx_solicitudes_correo (correo_corporativo)
) ENGINE=InnoDB COMMENT='Cola de aprobacion para registro de nuevos usuarios';

-- ────────────────────────────────────────────
-- 3. AUTENTICACION Y TOKENS
-- ────────────────────────────────────────────

CREATE TABLE tokens_acceso (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT        NULL,
    token            VARCHAR(255)  NOT NULL UNIQUE,
    tipo             VARCHAR(20)   NOT NULL COMMENT 'MAGIC_LINK, RECOVERY, VERIFICACION',
    usado            BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_expiracion TIMESTAMP     NOT NULL,
    ip_solicitante   VARCHAR(45)   NULL,

    estado_activo    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    INDEX idx_tokens_usuario    (usuario_id),
    INDEX idx_tokens_expiracion (fecha_expiracion)
) ENGINE=InnoDB COMMENT='Tokens de corta vida para magic links y recuperacion';

CREATE TABLE sesiones_usuario (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT        NOT NULL,
    token_sesion     VARCHAR(255)  NOT NULL UNIQUE,
    ip_address       VARCHAR(45)   NULL,
    user_agent       VARCHAR(500)  NULL,

    activa           BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_expiracion TIMESTAMP     NOT NULL,
    ultimo_acceso    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    INDEX idx_sesiones_usuario (usuario_id),
    INDEX idx_sesiones_activa  (activa)
) ENGINE=InnoDB COMMENT='Sesiones activas del usuario';

-- ────────────────────────────────────────────
-- 4. DOCUMENTOS PRINCIPALES
-- ────────────────────────────────────────────

CREATE TABLE normativas (
    id                      BIGINT        AUTO_INCREMENT PRIMARY KEY,
    titulo                  VARCHAR(300)  NOT NULL,
    descripcion_breve       VARCHAR(500)  NULL,
    contenido_resumido      TEXT          NULL,

    tematica_id             BIGINT        NOT NULL,
    tipo_normativa_id       BIGINT        NOT NULL,
    estado_normativa_id     BIGINT        NOT NULL,
    ambito                  VARCHAR(15)   NOT NULL DEFAULT 'NACIONAL' COMMENT 'NACIONAL, INTERNACIONAL',
    clasificacion           VARCHAR(20)   NOT NULL DEFAULT 'VINCULANTE' COMMENT 'VINCULANTE, NO_VINCULANTE',

    anio                    INT           NOT NULL,
    fecha_promulgacion      DATE          NULL,
    fecha_entrada_vigencia  DATE          NULL,
    fecha_derogacion        DATE          NULL,

    archivo_url             VARCHAR(500)  NOT NULL,
    archivo_tipo_mime       VARCHAR(50)   NULL,

    creada_por_id           BIGINT        NOT NULL,
    validada_por_id         BIGINT        NULL,
    es_publica              BOOLEAN       NOT NULL DEFAULT FALSE,
    visualizaciones_count   INT           NOT NULL DEFAULT 0,
    descargas_count         INT           NOT NULL DEFAULT 0,

    estado_activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_normativas_tematica      FOREIGN KEY (tematica_id)         REFERENCES tematicas(id)         ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_tipo          FOREIGN KEY (tipo_normativa_id)   REFERENCES tipos_normativa(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_estado        FOREIGN KEY (estado_normativa_id) REFERENCES estados_normativa(id) ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_creador       FOREIGN KEY (creada_por_id)       REFERENCES usuarios(id)          ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_validador     FOREIGN KEY (validada_por_id)     REFERENCES usuarios(id)          ON DELETE SET NULL,
    CONSTRAINT chk_normativas_ambito       CHECK (ambito IN ('NACIONAL', 'INTERNACIONAL')),
    CONSTRAINT chk_normativas_clasificacion CHECK (clasificacion IN ('VINCULANTE', 'NO_VINCULANTE')),

    INDEX idx_normativas_tematica       (tematica_id),
    INDEX idx_normativas_estado         (estado_normativa_id),
    INDEX idx_normativas_anio           (anio),
    INDEX idx_normativas_publica        (es_publica),
    INDEX idx_normativas_clasificacion  (clasificacion)
) ENGINE=InnoDB COMMENT='Repositorio de normativas legales';

CREATE TABLE estudios (
    id                       BIGINT        AUTO_INCREMENT PRIMARY KEY,
    titulo                   VARCHAR(300)  NOT NULL,
    descripcion_breve        VARCHAR(500)  NULL,
    resumen                  TEXT          NULL,

    tematica_id              BIGINT        NOT NULL,
    autor                    VARCHAR(200)  NULL,
    fecha_estudio            DATE          NULL,
    anio                     INT           NULL,

    archivo_url              VARCHAR(500)  NOT NULL,
    archivo_tipo_mime        VARCHAR(50)   NULL,
    normativa_relacionada_id BIGINT        NULL,

    subida_por_id            BIGINT        NOT NULL,
    validado_por_id          BIGINT        NULL,
    es_publico               BOOLEAN       NOT NULL DEFAULT FALSE,
    visualizaciones_count    INT           NOT NULL DEFAULT 0,
    descargas_count          INT           NOT NULL DEFAULT 0,

    estado_activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_estudios_tematica  FOREIGN KEY (tematica_id)              REFERENCES tematicas(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_estudios_normativa FOREIGN KEY (normativa_relacionada_id) REFERENCES normativas(id) ON DELETE SET NULL,
    CONSTRAINT fk_estudios_subidor   FOREIGN KEY (subida_por_id)            REFERENCES usuarios(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_estudios_validador FOREIGN KEY (validado_por_id)          REFERENCES usuarios(id)   ON DELETE SET NULL,

    INDEX idx_estudios_tematica (tematica_id),
    INDEX idx_estudios_anio     (anio),
    INDEX idx_estudios_publico  (es_publico)
) ENGINE=InnoDB COMMENT='Estudios e investigaciones';

-- ────────────────────────────────────────────
-- 5. BUSQUEDA Y ETIQUETADO
-- ────────────────────────────────────────────

CREATE TABLE etiquetas (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(50)   NOT NULL UNIQUE,
    descripcion   VARCHAR(255)  NULL,
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Etiquetas libres para clasificar';

CREATE TABLE normativas_etiquetas (
    normativa_id BIGINT NOT NULL,
    etiqueta_id  BIGINT NOT NULL,

    PRIMARY KEY (normativa_id, etiqueta_id),
    CONSTRAINT fk_norm_etiq_normativa FOREIGN KEY (normativa_id) REFERENCES normativas(id) ON DELETE CASCADE,
    CONSTRAINT fk_norm_etiq_etiqueta  FOREIGN KEY (etiqueta_id)  REFERENCES etiquetas(id)  ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE estudios_etiquetas (
    estudio_id  BIGINT NOT NULL,
    etiqueta_id BIGINT NOT NULL,

    PRIMARY KEY (estudio_id, etiqueta_id),
    CONSTRAINT fk_est_etiq_estudio  FOREIGN KEY (estudio_id)  REFERENCES estudios(id)  ON DELETE CASCADE,
    CONSTRAINT fk_est_etiq_etiqueta FOREIGN KEY (etiqueta_id) REFERENCES etiquetas(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ────────────────────────────────────────────
-- 6. AUDITORIA Y LOGS
-- ────────────────────────────────────────────

CREATE TABLE logs_actividad (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id          BIGINT        NOT NULL,
    tipo_accion         VARCHAR(50)   NOT NULL COMMENT 'LOGIN, LOGOUT, DESCARGAR, VISUALIZAR, CREAR, EDITAR, ELIMINAR',
    recurso_tipo        VARCHAR(50)   NOT NULL COMMENT 'NORMATIVA, ESTUDIO, USUARIO, SISTEMA',
    recurso_id          BIGINT        NULL,
    descripcion_detalle TEXT          NULL,
    ip_address          VARCHAR(45)   NULL,
    user_agent          VARCHAR(500)  NULL,

    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_logs_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT,

    INDEX idx_logs_usuario     (usuario_id),
    INDEX idx_logs_tipo_accion (tipo_accion),
    INDEX idx_logs_fecha       (created_at)
) ENGINE=InnoDB COMMENT='Registro de actividad del sistema';

CREATE TABLE cambios_documento (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    documento_tipo    VARCHAR(20)   NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
    documento_id      BIGINT        NOT NULL,
    modificado_por_id BIGINT        NOT NULL,
    tipo_cambio       VARCHAR(50)   NOT NULL,
    campos_antes      JSON          NULL,
    campos_despues    JSON          NULL,
    motivo            TEXT          NULL,

    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cambios_usuario FOREIGN KEY (modificado_por_id) REFERENCES usuarios(id) ON DELETE RESTRICT,

    INDEX idx_cambios_documento (documento_tipo, documento_id),
    INDEX idx_cambios_usuario   (modificado_por_id),
    INDEX idx_cambios_fecha     (created_at)
) ENGINE=InnoDB COMMENT='Historial versionado de cambios';

-- ────────────────────────────────────────────
-- 7. NOTIFICACIONES
-- ────────────────────────────────────────────

CREATE TABLE notificaciones (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT        NOT NULL,
    tipo           VARCHAR(30)   NOT NULL COMMENT 'NUEVA_NORMATIVA, NUEVO_ESTUDIO, DOCUMENTO_VALIDADO, MENSAJE_ADMIN, SISTEMA',
    titulo         VARCHAR(255)  NOT NULL,
    mensaje        TEXT          NOT NULL,
    enlace_recurso VARCHAR(500)  NULL,

    leida          BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_lectura  TIMESTAMP     NULL,

    estado_activo  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    INDEX idx_notificaciones_usuario (usuario_id),
    INDEX idx_notificaciones_leida   (leida),
    INDEX idx_notificaciones_fecha   (created_at)
) ENGINE=InnoDB;

CREATE TABLE preferencias_notificacion (
    id                       BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id               BIGINT        NOT NULL UNIQUE,

    email_habilitado         BOOLEAN       NOT NULL DEFAULT TRUE,
    push_habilitado          BOOLEAN       NOT NULL DEFAULT FALSE,
    sms_habilitado           BOOLEAN       NOT NULL DEFAULT FALSE,

    notif_nuevas_normativas  BOOLEAN       NOT NULL DEFAULT TRUE,
    notif_nuevos_estudios    BOOLEAN       NOT NULL DEFAULT TRUE,
    notif_cambios_seguidos   BOOLEAN       NOT NULL DEFAULT TRUE,
    notif_mensajes_admin     BOOLEAN       NOT NULL DEFAULT TRUE,
    notif_digesto_semanal    BOOLEAN       NOT NULL DEFAULT FALSE,

    frecuencia_email         VARCHAR(15)   NOT NULL DEFAULT 'DIARIA' COMMENT 'INMEDIATA, DIARIA, SEMANAL, NUNCA',

    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_preferencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    INDEX idx_preferencias_usuario (usuario_id)
) ENGINE=InnoDB;

-- ────────────────────────────────────────────
-- 8. CHATBOT / ASISTENTE VIRTUAL
-- ────────────────────────────────────────────

CREATE TABLE conversaciones_chatbot (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id    BIGINT        NOT NULL,
    idioma        VARCHAR(5)    NOT NULL DEFAULT 'es',
    resumen       TEXT          NULL,

    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_conversaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    INDEX idx_conversaciones_usuario (usuario_id),
    INDEX idx_conversaciones_fecha   (created_at)
) ENGINE=InnoDB;

CREATE TABLE mensajes_chatbot (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    conversacion_id  BIGINT        NOT NULL,
    rol              VARCHAR(10)   NOT NULL COMMENT 'USER, BOT',
    contenido        TEXT          NOT NULL,

    intent_detectado VARCHAR(100)  NULL,
    confianza        DECIMAL(5,4)  NULL,
    derivado_soporte BOOLEAN       NOT NULL DEFAULT FALSE,

    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mensajes_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversaciones_chatbot(id) ON DELETE CASCADE,

    INDEX idx_mensajes_conversacion (conversacion_id),
    INDEX idx_mensajes_fecha        (created_at)
) ENGINE=InnoDB;

-- ────────────────────────────────────────────
-- 9. MODULO SUPERADMIN
-- ────────────────────────────────────────────

CREATE TABLE parametros_sistema (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    clave             VARCHAR(100)  NOT NULL UNIQUE,
    valor             TEXT          NOT NULL,
    descripcion       VARCHAR(300)  NULL,
    tipo_dato         VARCHAR(10)   NOT NULL DEFAULT 'STRING' COMMENT 'STRING, INTEGER, BOOLEAN, JSON',

    modificado_por_id BIGINT        NULL,
    estado_activo     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_parametros_modificador FOREIGN KEY (modificado_por_id) REFERENCES usuarios(id) ON DELETE SET NULL,

    INDEX idx_parametros_clave (clave)
) ENGINE=InnoDB COMMENT='Parametros globales configurables sin tocar codigo';

INSERT INTO parametros_sistema (clave, valor, descripcion, tipo_dato) VALUES
('max_intentos_login',        '5',    'Intentos fallidos antes de bloquear',    'INTEGER'),
('tiempo_bloqueo_minutos',    '30',   'Minutos que dura el bloqueo',            'INTEGER'),
('magic_link_expiracion_min', '10',   'Minutos de vida de un magic link',       'INTEGER'),
('recovery_expiracion_min',   '30',   'Minutos para recuperar contrasena',      'INTEGER'),
('filtro_max_anios_estudio',  '1',    'Rango maximo de anios en busqueda',      'INTEGER'),
('chatbot_habilitado',        'true', 'Activar o desactivar chatbot global',    'BOOLEAN'),
('idioma_predeterminado',     'es',   'Idioma por defecto del sistema',         'STRING');

CREATE TABLE solicitudes_respaldo (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    solicitado_por_id BIGINT        NOT NULL,
    tipo              VARCHAR(30)   NOT NULL DEFAULT 'COMPLETO' COMMENT 'COMPLETO, PARCIAL_USUARIOS, PARCIAL_DOCUMENTOS, PARCIAL_LOGS',
    estado            VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE' COMMENT 'PENDIENTE, EN_PROCESO, COMPLETADO, FALLIDO',
    storage_key       VARCHAR(500)  NULL,
    tamanio_bytes     BIGINT        NULL,
    notas             TEXT          NULL,

    fecha_completado  TIMESTAMP     NULL,
    estado_activo     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_respaldo_solicitante FOREIGN KEY (solicitado_por_id) REFERENCES usuarios(id) ON DELETE RESTRICT,

    INDEX idx_respaldos_estado      (estado),
    INDEX idx_respaldos_solicitante (solicitado_por_id)
) ENGINE=InnoDB COMMENT='Solicitudes de respaldo para auditoria de backups';

CREATE TABLE sesiones_impersonacion (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    superadmin_id       BIGINT        NOT NULL,
    usuario_objetivo_id BIGINT        NOT NULL,
    motivo              VARCHAR(500)  NULL,
    ip_address          VARCHAR(45)   NOT NULL,

    fecha_inicio        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin           TIMESTAMP     NULL,
    activa              BOOLEAN       NOT NULL DEFAULT TRUE,

    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_impersonacion_sa   FOREIGN KEY (superadmin_id)       REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_impersonacion_user FOREIGN KEY (usuario_objetivo_id) REFERENCES usuarios(id) ON DELETE RESTRICT,

    INDEX idx_impersonacion_sa       (superadmin_id),
    INDEX idx_impersonacion_objetivo (usuario_objetivo_id),
    INDEX idx_impersonacion_activa   (activa)
) ENGINE=InnoDB COMMENT='Auditoria de impersonaciones del SuperAdmin';

-- ────────────────────────────────────────────
-- 10. DOCUMENTOS RECIENTES (UX)
-- ────────────────────────────────────────────

CREATE TABLE documentos_recientes (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT        NOT NULL,
    documento_tipo VARCHAR(20)   NOT NULL COMMENT 'NORMATIVA, ESTUDIO',
    documento_id   BIGINT        NOT NULL,

    ultima_vista   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_recientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,

    UNIQUE INDEX uq_recientes_usuario_doc (usuario_id, documento_tipo, documento_id),
    INDEX idx_recientes_usuario (usuario_id),
    INDEX idx_recientes_fecha   (ultima_vista)
) ENGINE=InnoDB COMMENT='Acceso rapido a documentos vistos recientemente';

-- ────────────────────────────────────────────
-- 11. INDICES COMPUESTOS PARA REPORTES
-- ────────────────────────────────────────────

CREATE INDEX idx_logs_reporte_usuario_fecha ON logs_actividad (usuario_id, created_at, tipo_accion);
CREATE INDEX idx_logs_reporte_descarga      ON logs_actividad (tipo_accion, recurso_tipo, recurso_id, created_at);
CREATE INDEX idx_normativas_filtro          ON normativas (ambito, clasificacion, tematica_id, estado_normativa_id, tipo_normativa_id, anio);

-- ============================================================
-- FIN DEL SCRIPT DDL
-- ============================================================