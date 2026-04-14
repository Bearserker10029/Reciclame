-- ============================================================
-- RECÍCLAME — Repositorio Circular
-- DDL Script · MySQL 8.0 · Spring Boot
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS reciclame_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE reciclame_db;

-- ────────────────────────────────────────────
-- 1. TABLAS DE CATÁLOGO (Lookup / Referencia)
-- ────────────────────────────────────────────

CREATE TABLE roles (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(30)  NOT NULL UNIQUE COMMENT 'Tenemos SUPERADMIN, ADMINISTRADOR, SOCIO, VISUALIZADOR - ¿estará bien así?',
    descripcion VARCHAR(255) NULL,
    nivel       TINYINT      NOT NULL DEFAULT 0 COMMENT 'Pensé en una jerarquía numérica para validaciones: 0=Visualizador, 1=Socio, 2=Admin, 3=SuperAdmin. Más fácil que strings',
    estado_activo BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catálogo de roles - decidí guardarlos en tabla en vez de enum porque podrían crecer';

INSERT INTO roles (nombre, descripcion, nivel) VALUES
('SUPERADMIN',     'Administrador supremo PUCP — auditoría y gestión global', 3),
('ADMINISTRADOR',  'Gestión de usuarios, documentos y reportes',             2),
('SOCIO',          'Acceso completo al repositorio — lectura y descarga',     1),
('VISUALIZADOR',   'Acceso limitado — solo lectura de documentos públicos',   0);

CREATE TABLE tematicas (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(10)  NOT NULL UNIQUE COMMENT 'EC=Econ.Circular, GR=Gest.Residuos, EE=Envases, REP=Responsabilidad, OTRO. Códigos cortos para no repetir texto',
    nombre      VARCHAR(100) NOT NULL,
    color_hex   VARCHAR(7)   NOT NULL DEFAULT '#95A5A6' COMMENT 'Color para la UI - cada temática su color. Así en el front se pinta automático',
    estado_activo BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Temáticas de normativas - pensé que es mejor tabla que hardcodear los colores en código';

INSERT INTO tematicas (codigo, nombre, color_hex) VALUES
('EC',   'Economía Circular',             '#27AE60'),
('GR',   'Gestión de Residuos',           '#3498DB'),
('EE',   'Envases y Embalajes',           '#F39C12'),
('REP',  'Responsabilidad Extendida del Productor', '#E74C3C'),
('OTRO', 'Otros',                         '#95A5A6');

CREATE TABLE tipos_normativa (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL UNIQUE COMMENT 'Ley, Reglamento, NTP, Directiva, Hoja de Ruta, Resolución - tipos que maneja el usuario',
    estado_activo BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Tipos de norma legal - tabla de referencia';

INSERT INTO tipos_normativa (nombre) VALUES
('Ley'), ('Reglamento'), ('NTP / Norma técnica'), ('Directiva'), ('Hoja de Ruta'), ('Resolución');

CREATE TABLE estados_normativa (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60)  NOT NULL UNIQUE COMMENT 'Vigente, Proyecto publicado, En proceso, Consulta pública, Derogada - ciclo de vida de una norma',
    estado_activo BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Estados de vigencia - tabla de referencia, ¿me falta algún estado?';

INSERT INTO estados_normativa (nombre) VALUES
('Vigente'), ('Proyecto publicado'), ('Proyecto / borrador en proceso'), ('Consulta pública'), ('Derogada');

-- ────────────────────────────────────────────
-- 2. EMPRESAS Y USUARIOS
-- ────────────────────────────────────────────

CREATE TABLE empresas (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    razon_social  VARCHAR(200)  NOT NULL,
    ruc           VARCHAR(11)   NULL UNIQUE COMMENT 'RUC peruano - 11 dígitos. Ahí va el registro de cada empresa',
    sector        VARCHAR(100)  NULL COMMENT 'Envases Tetra, Plásticos PET, Cartón/Papel, Metales, etc. - lo dejé con TEXT libre por si varía',
    estado_activo BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_empresas_razon_social (razon_social)
) ENGINE=InnoDB COMMENT='Empresas socias de Recíclame - cada usuario está asociado a una empresa';

CREATE TABLE usuarios (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre_completo     VARCHAR(200)  NOT NULL,
    dni                 VARCHAR(8)    NOT NULL UNIQUE COMMENT 'DNI - 8 dígitos. Identificador único del usuario',
    correo_corporativo  VARCHAR(200)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255)  NOT NULL COMMENT 'BCrypt hash - nunca guardar en plano, obvio',
    cargo               VARCHAR(150)  NULL,
    foto_url            VARCHAR(500)  NULL COMMENT 'URL en cloud storage - mejor URL que blob en BD',
    
    -- FK relaciones
    rol_id              BIGINT        NOT NULL,
    empresa_id          BIGINT        NULL,
    
    -- Seguridad - esto es importante
    intentos_fallidos   TINYINT       NOT NULL DEFAULT 0,
    cuenta_bloqueada    BOOLEAN       NOT NULL DEFAULT FALSE,
    ip_bloqueada        VARCHAR(45)   NULL COMMENT 'IPv4/IPv6 - bloquear si muchos intentos. ¿Esto es muy estricto?',
    tfa_activo          BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Autenticación 2FA - opcional pero disponible',
    tfa_secret          VARCHAR(64)   NULL COMMENT 'Secret TOTP para 2FA - guardar solo si está activo',
    
    -- Estado y auditoría
    estado_cuenta       ENUM('PENDIENTE','ACTIVO','INACTIVO','BLOQUEADO') NOT NULL DEFAULT 'PENDIENTE',
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    ultimo_login        TIMESTAMP     NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_usuarios_rol     FOREIGN KEY (rol_id)     REFERENCES roles(id)     ON DELETE RESTRICT,
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)  ON DELETE SET NULL,
    
    INDEX idx_usuarios_correo      (correo_corporativo),
    INDEX idx_usuarios_dni         (dni),
    INDEX idx_usuarios_rol         (rol_id),
    INDEX idx_usuarios_empresa     (empresa_id),
    INDEX idx_usuarios_estado      (estado_cuenta)
) ENGINE=InnoDB COMMENT='Usuarios - muchos campos de seguridad porque es crítico';

CREATE TABLE solicitudes_registro (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre_completo     VARCHAR(200)  NOT NULL,
    dni                 VARCHAR(8)    NOT NULL,
    correo_corporativo  VARCHAR(200)  NOT NULL,
    empresa_nombre      VARCHAR(200)  NOT NULL COMMENT 'Lo pongo como texto libre porque al registrarse aún no sé qué empresa es',
    
    -- Resolución
    estado              ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    revisada_por_id     BIGINT        NULL COMMENT 'El admin que aprobó o rechazó',
    motivo_rechazo      TEXT          NULL,
    fecha_resolucion    TIMESTAMP     NULL,
    
    -- Auditoría
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_solicitudes_revisor FOREIGN KEY (revisada_por_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    
    INDEX idx_solicitudes_estado    (estado),
    INDEX idx_solicitudes_correo   (correo_corporativo)
) ENGINE=InnoDB COMMENT='Solicitudes de registro - tabla intermedia antes de crear usuario definitivo';

-- ────────────────────────────────────────────
-- 3. MÓDULO DE AUTENTICACIÓN Y TOKENS
-- ────────────────────────────────────────────

CREATE TABLE tokens_acceso (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NULL COMMENT 'NULL si es magic link para solicitud de registro nueva',
    token           VARCHAR(255)  NOT NULL UNIQUE,
    tipo            ENUM('MAGIC_LINK','RECOVERY','VERIFICACION') NOT NULL,
    usado           BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_expiracion TIMESTAMP    NOT NULL,
    ip_solicitante  VARCHAR(45)   NULL COMMENT 'Guardar IP para auditoría - ¿no es overkill? xd ',
    
    estado_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_tokens_usuario (usuario_id),
    INDEX idx_tokens_expiracion (fecha_expiracion)
) ENGINE=InnoDB COMMENT='Tokens para magic links y recuperación - son de corta vida';

CREATE TABLE sesiones_usuario (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL,
    token_sesion    VARCHAR(255)  NOT NULL UNIQUE COMMENT 'JWT token o UUID de sesión',
    ip_address      VARCHAR(45)   NULL COMMENT 'De dónde se conectó',
    user_agent      VARCHAR(500)  NULL COMMENT 'Navegador/device - útil para detectar cambios raros',
    
    activa          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_expiracion TIMESTAMP    NOT NULL,
    ultimo_acceso   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_sesiones_usuario (usuario_id),
    INDEX idx_sesiones_activa  (activa)
) ENGINE=InnoDB COMMENT='Sesiones activas del usuario - rastrear conexiones';

-- ────────────────────────────────────────────
-- 4. DOCUMENTOS PRINCIPALES: NORMATIVAS Y ESTUDIOS
-- ────────────────────────────────────────────

CREATE TABLE normativas (
    id                      BIGINT        AUTO_INCREMENT PRIMARY KEY,
    titulo                  VARCHAR(300)  NOT NULL,
    descripcion_breve       VARCHAR(500)  NULL,
    contenido_resumido      TEXT          NULL COMMENT 'Resumen ejecutivo - ¿debería estar en otra tabla por rendimiento? :v',
    
    -- Clasificación
    tematica_id             BIGINT        NOT NULL,
    tipo_normativa_id       BIGINT        NOT NULL,
    estado_normativa_id     BIGINT        NOT NULL,
    ambito                  ENUM('NACIONAL','REGIONAL','LOCAL') NOT NULL DEFAULT 'NACIONAL',
    
    -- Cronología
    anio                    INT           NOT NULL COMMENT 'Año de promulgación o proyecto',
    fecha_promulgacion      DATE          NULL,
    fecha_entrada_vigencia  DATE          NULL,
    fecha_derogacion        DATE          NULL COMMENT 'Si está derogada',
    
    -- Documentación
    archivo_url             VARCHAR(500)  NOT NULL COMMENT 'URL al PDF en cloud - ¿debería permitir múltiples archivos?',
    archivo_tipo_mime       VARCHAR(50)   NULL COMMENT 'application/pdf, etc',
    
    -- Metadata
    creada_por_id           BIGINT        NOT NULL,
    validada_por_id         BIGINT        NULL COMMENT 'Si un admin la validó',
    es_publica              BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Si todo el mundo la ve o solo socios',
    visualizaciones_count   INT           NOT NULL DEFAULT 0 COMMENT 'Contador de vistas - para ranking',
    descargas_count         INT           NOT NULL DEFAULT 0 COMMENT 'Cuántas veces la descargaron',
    
    -- Control
    estado_activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_normativas_tematica      FOREIGN KEY (tematica_id)         REFERENCES tematicas(id)            ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_tipo          FOREIGN KEY (tipo_normativa_id)   REFERENCES tipos_normativa(id)      ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_estado        FOREIGN KEY (estado_normativa_id) REFERENCES estados_normativa(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_creador       FOREIGN KEY (creada_por_id)       REFERENCES usuarios(id)             ON DELETE RESTRICT,
    CONSTRAINT fk_normativas_validador     FOREIGN KEY (validada_por_id)     REFERENCES usuarios(id)             ON DELETE SET NULL,
    
    INDEX idx_normativas_tematica    (tematica_id),
    INDEX idx_normativas_estado      (estado_normativa_id),
    INDEX idx_normativas_anio        (anio),
    INDEX idx_normativas_publica     (es_publica)
) ENGINE=InnoDB COMMENT='Normativas - documentos legales del sistema';

CREATE TABLE estudios (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    titulo              VARCHAR(300)  NOT NULL,
    descripcion_breve   VARCHAR(500)  NULL,
    resumen             TEXT          NULL COMMENT 'Resumen del estudio - síntesis',
    
    -- Clasificación
    tematica_id         BIGINT        NOT NULL,
    
    -- Datos del estudio
    autor               VARCHAR(200)  NULL COMMENT 'Quién lo escribió o institución',
    fecha_estudio       DATE          NULL COMMENT 'Cuándo se publicó/realizó el estudio',
    anio                INT           NULL COMMENT 'Año del estudio',
    
    -- Documentación
    archivo_url         VARCHAR(500)  NOT NULL COMMENT 'URL al PDF o documento en cloud',
    archivo_tipo_mime   VARCHAR(50)   NULL COMMENT 'Tipo MIME del archivo',
    
    -- Relación con normativas
    normativa_relacionada_id BIGINT   NULL COMMENT 'Si el estudio está asociado a una normativa específica - ¿no debería ser una tabla intermedia?',
    
    -- Metadata
    subida_por_id       BIGINT        NOT NULL,
    validado_por_id     BIGINT        NULL COMMENT 'Admin que lo validó',
    es_publico          BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Visibilidad',
    visualizaciones_count INT         NOT NULL DEFAULT 0 COMMENT 'Contador de vistas',
    descargas_count     INT           NOT NULL DEFAULT 0 COMMENT 'Descargas',
    
    -- Control
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_estudios_tematica   FOREIGN KEY (tematica_id)              REFERENCES tematicas(id)         ON DELETE RESTRICT,
    CONSTRAINT fk_estudios_normativa  FOREIGN KEY (normativa_relacionada_id) REFERENCES normativas(id)        ON DELETE SET NULL,
    CONSTRAINT fk_estudios_subidor    FOREIGN KEY (subida_por_id)            REFERENCES usuarios(id)          ON DELETE RESTRICT,
    CONSTRAINT fk_estudios_validador  FOREIGN KEY (validado_por_id)          REFERENCES usuarios(id)          ON DELETE SET NULL,
    
    INDEX idx_estudios_tematica     (tematica_id),
    INDEX idx_estudios_anio         (anio),
    INDEX idx_estudios_publico      (es_publico)
) ENGINE=InnoDB COMMENT='Estudios - investigaciones relacionadas con reciclaje';

-- ────────────────────────────────────────────
-- 5. MÓDULO DE BÚSQUEDA Y ETIQUETADO
-- ────────────────────────────────────────────

CREATE TABLE etiquetas (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50)   NOT NULL UNIQUE COMMENT 'Etiqueta libre - "plástico", "reciclabilidad", etc.',
    descripcion VARCHAR(255)  NULL,
    
    estado_activo BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Etiquetas personalizadas - para clasificar documentos más finamente';

CREATE TABLE normativas_etiquetas (
    normativa_id BIGINT NOT NULL,
    etiqueta_id  BIGINT NOT NULL,
    
    PRIMARY KEY (normativa_id, etiqueta_id),
    CONSTRAINT fk_norm_etiq_normativa FOREIGN KEY (normativa_id) REFERENCES normativas(id) ON DELETE CASCADE,
    CONSTRAINT fk_norm_etiq_etiqueta  FOREIGN KEY (etiqueta_id)  REFERENCES etiquetas(id)  ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Relación N:M entre normativas y etiquetas';

CREATE TABLE estudios_etiquetas (
    estudio_id  BIGINT NOT NULL,
    etiqueta_id BIGINT NOT NULL,
    
    PRIMARY KEY (estudio_id, etiqueta_id),
    CONSTRAINT fk_est_etiq_estudio  FOREIGN KEY (estudio_id)  REFERENCES estudios(id)   ON DELETE CASCADE,
    CONSTRAINT fk_est_etiq_etiqueta FOREIGN KEY (etiqueta_id) REFERENCES etiquetas(id)  ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Relación N:M entre estudios y etiquetas';

-- ────────────────────────────────────────────
-- 6. MÓDULO DE AUDITORÍA Y LOGS
-- ────────────────────────────────────────────

CREATE TABLE logs_actividad (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id          BIGINT        NOT NULL,
    tipo_accion         VARCHAR(50)   NOT NULL COMMENT 'LOGIN, LOGOUT, DESCARGAR, VISUALIZAR, CREAR, EDITAR, ELIMINAR, etc',
    recurso_tipo        VARCHAR(50)   NOT NULL COMMENT 'NORMATIVA, ESTUDIO, USUARIO, SISTEMA',
    recurso_id          BIGINT        NULL COMMENT 'ID del normativa/estudio/usuario',
    descripcion_detalle TEXT          NULL COMMENT 'Detalles específicos de la acción',
    ip_address          VARCHAR(45)   NULL COMMENT 'IP desde donde se hizo la acción',
    user_agent          VARCHAR(500)  NULL COMMENT 'Navegador/device',
    
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_logs_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    
    INDEX idx_logs_usuario      (usuario_id),
    INDEX idx_logs_tipo_accion  (tipo_accion),
    INDEX idx_logs_fecha        (created_at)
) ENGINE=InnoDB COMMENT='Auditoría - rastrear todo lo que hace el usuario - ¿será muy pesado?';

CREATE TABLE cambios_documento (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    documento_tipo      ENUM('NORMATIVA','ESTUDIO') NOT NULL,
    documento_id        BIGINT        NOT NULL COMMENT 'ID del normativa o estudio',
    modificado_por_id   BIGINT        NOT NULL,
    tipo_cambio         VARCHAR(50)   NOT NULL COMMENT 'CREACION, MODIFICACION, VALIDACION, ELIMINACION, etc',
    campos_antes        JSON          NULL COMMENT 'Valores anteriores - JSON de cambios',
    campos_despues      JSON          NULL COMMENT 'Valores nuevos - JSON de cambios',
    motivo              TEXT          NULL COMMENT 'Por qué se hizo el cambio',
    
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cambios_usuario FOREIGN KEY (modificado_por_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    
    INDEX idx_cambios_documento   (documento_tipo, documento_id),
    INDEX idx_cambios_usuario     (modificado_por_id),
    INDEX idx_cambios_fecha       (created_at)
) ENGINE=InnoDB COMMENT='Historial de cambios en documentos - versioning - ¿qué debería guardar exactamente?';

-- ────────────────────────────────────────────
-- 7. MÓDULO DE NOTIFICACIONES Y PREFERENCIAS
-- ────────────────────────────────────────────

CREATE TABLE notificaciones (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL,
    tipo            ENUM('NUEVA_NORMATIVA','NUEVO_ESTUDIO','DOCUMENTO_VALIDADO','MENSAJE_ADMIN','SISTEMA') NOT NULL,
    titulo          VARCHAR(255)  NOT NULL,
    mensaje          TEXT          NOT NULL,
    enlace_recurso  VARCHAR(500)  NULL COMMENT 'URL al documento si es relevante',
    
    leida           BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_lectura   TIMESTAMP     NULL,
    
    estado_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_notificaciones_usuario (usuario_id),
    INDEX idx_notificaciones_leida   (leida),
    INDEX idx_notificaciones_fecha   (created_at)
) ENGINE=InnoDB COMMENT='Notificaciones - push, email, etc. Decidí guardarlas en BD para historial';

CREATE TABLE preferencias_notificacion (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL UNIQUE COMMENT 'Una preferencia por usuario',
    
    -- Canales
    email_habilitado         BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Recibir notificaciones por email',
    push_habilitado          BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Notificaciones push en navegador',
    sms_habilitado           BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'SMS - ¿necesitamos esto?',
    
    -- Tipos de eventos
    notif_nuevas_normativas  BOOLEAN NOT NULL DEFAULT TRUE,
    notif_nuevos_estudios    BOOLEAN NOT NULL DEFAULT TRUE,
    notif_cambios_seguidos   BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Alertas de documentos que está siguiendo',
    notif_mensajes_admin     BOOLEAN NOT NULL DEFAULT TRUE,
    notif_digesto_semanal    BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Resumen semanal',
    
    -- Frecuencia
    frecuencia_email         ENUM('INMEDIATA','DIARIA','SEMANAL','NUNCA') NOT NULL DEFAULT 'DIARIA',
    
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_preferencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_preferencias_usuario (usuario_id)
) ENGINE=InnoDB COMMENT='Preferencias de notificación por usuario - cada uno elige cómo le aviso';

-- ────────────────────────────────────────────
-- 8. MÓDULO DE CHATBOT / ASISTENTE VIRTUAL
-- ────────────────────────────────────────────

CREATE TABLE conversaciones_chatbot (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL,
    idioma          VARCHAR(5)    NOT NULL DEFAULT 'es' COMMENT 'es, en - soporte multiidioma del bot',
    resumen         TEXT          NULL COMMENT 'Resumen automático de la conversación - útil para histórico',
    
    estado_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_conversaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_conversaciones_usuario (usuario_id),
    INDEX idx_conversaciones_fecha   (created_at)
) ENGINE=InnoDB COMMENT='Sesiones del chatbot - una conversación = una sesión';

CREATE TABLE mensajes_chatbot (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    conversacion_id     BIGINT        NOT NULL,
    rol                 ENUM('USER','BOT') NOT NULL,
    contenido           TEXT          NOT NULL,
    
    -- Metadata del procesamiento
    intent_detectado    VARCHAR(100)  NULL COMMENT 'Intent que el bot clasificó - "buscar_normativa", "ayuda", etc',
    confianza           DECIMAL(5,4)  NULL COMMENT 'Cuán seguro está el bot sobre el intent - 0 a 1',
    derivado_soporte    BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'Si fue derivado a un humano en soporte',
    
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensajes_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversaciones_chatbot(id) ON DELETE CASCADE,
    
    INDEX idx_mensajes_conversacion (conversacion_id),
    INDEX idx_mensajes_fecha        (created_at)
) ENGINE=InnoDB COMMENT='Mensajes individuales en cada conversación';

-- ────────────────────────────────────────────
-- 9. MÓDULO SUPERADMIN
-- ────────────────────────────────────────────

CREATE TABLE parametros_sistema (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    clave           VARCHAR(100)  NOT NULL UNIQUE COMMENT 'Identificador único - max_intentos_login, etc',
    valor           TEXT          NOT NULL,
    descripcion     VARCHAR(300)  NULL,
    tipo_dato       ENUM('STRING','INTEGER','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING',
    
    modificado_por_id BIGINT      NULL,
    estado_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_parametros_modificador FOREIGN KEY (modificado_por_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    
    INDEX idx_parametros_clave (clave)
) ENGINE=InnoDB COMMENT='Parámetros globales - configurable solo por SuperAdmin sin tocar código';

INSERT INTO parametros_sistema (clave, valor, descripcion, tipo_dato) VALUES
('max_intentos_login',       '5',     'Cuántos intentos fallidos de login antes de bloquear',          'INTEGER'),
('tiempo_bloqueo_minutos',   '30',    'Cuánto tiempo está bloqueado si excede intentos',    'INTEGER'),
('magic_link_expiracion_min','10',    'Minutos que dura válido un magic link de login',           'INTEGER'),
('recovery_expiracion_min',  '30',    'Minutos para recuperar contraseña', 'INTEGER'),
('filtro_max_anios_estudio', '1',     'Rango máximo de años en filtro de búsqueda',   'INTEGER'),
('chatbot_habilitado',       'true',  'Activar o desactivar el chatbot para todos',              'BOOLEAN'),
('idioma_predeterminado',    'es',    'Idioma por defecto del sistema',              'STRING');

CREATE TABLE solicitudes_respaldo (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    solicitado_por_id BIGINT      NOT NULL,
    tipo            ENUM('COMPLETO','PARCIAL_USUARIOS','PARCIAL_DOCUMENTOS','PARCIAL_LOGS') NOT NULL DEFAULT 'COMPLETO',
    estado          ENUM('PENDIENTE','EN_PROCESO','COMPLETADO','FALLIDO') NOT NULL DEFAULT 'PENDIENTE',
    storage_key     VARCHAR(500)  NULL COMMENT 'Ruta del respaldo en cloud storage',
    tamanio_bytes   BIGINT        NULL,
    notas           TEXT          NULL,
    
    fecha_completado TIMESTAMP    NULL,
    estado_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_respaldo_solicitante FOREIGN KEY (solicitado_por_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    
    INDEX idx_respaldos_estado      (estado),
    INDEX idx_respaldos_solicitante (solicitado_por_id)
) ENGINE=InnoDB COMMENT='Solicitudes de respaldo - auditoría de backups';

CREATE TABLE sesiones_impersonacion (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    superadmin_id       BIGINT        NOT NULL COMMENT 'Quién se está haciéndose pasar por otro usuario',
    usuario_objetivo_id BIGINT        NOT NULL COMMENT 'De quién está simulando ser',
    motivo              VARCHAR(500)  NULL,
    ip_address          VARCHAR(45)   NOT NULL,
    
    fecha_inicio        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin           TIMESTAMP     NULL COMMENT 'Cuándo terminó la sesión. NULL = todavía está activa',
    activa              BOOLEAN       NOT NULL DEFAULT TRUE,
    
    estado_activo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_impersonacion_sa   FOREIGN KEY (superadmin_id)       REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_impersonacion_user FOREIGN KEY (usuario_objetivo_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    
    INDEX idx_impersonacion_sa       (superadmin_id),
    INDEX idx_impersonacion_objetivo (usuario_objetivo_id),
    INDEX idx_impersonacion_activa   (activa)
) ENGINE=InnoDB COMMENT='Log de impersonaciones - cuando el admin se loguea como otro usuario';

-- ────────────────────────────────────────────
-- 10. TABLA DE DOCUMENTOS RECIENTES (Optimización UX)
-- ────────────────────────────────────────────

CREATE TABLE documentos_recientes (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL,
    documento_tipo  ENUM('NORMATIVA','ESTUDIO') NOT NULL,
    documento_id    BIGINT        NOT NULL,
    
    ultima_vista    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_recientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    UNIQUE INDEX uq_recientes_usuario_doc (usuario_id, documento_tipo, documento_id),
    INDEX idx_recientes_usuario     (usuario_id),
    INDEX idx_recientes_fecha       (ultima_vista)
) ENGINE=InnoDB COMMENT='Documentos vistos recientemente - para el widget de acceso rápido en el dashboard';

-- NOTA SOBRE DESNORMALIZACIÓN:
-- Decidí usar referencia polimórfica (documento_tipo + documento_id) en lugar de crear
-- dos tablas separadas (normativa_recientes y estudio_recientes). La razón es que:
--   1) El widget UI es simple y no requiere JOINs complejos
--   2) El volumen de escritura es bajo - máximo ~20 recientes por usuario
--   3) Usa UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) que es eficiente
--   4) Reduce duplicación de código en la aplicación
-- Si esto crece o se usa en reportes pesados, puedo normalizarlo después.

-- ────────────────────────────────────────────
-- 11. ÍNDICES COMPUESTOS PARA REPORTES
-- ────────────────────────────────────────────

-- Reporte: actividad por usuario en rango de fechas - necesito buscar por usuario y fecha rápido
CREATE INDEX idx_logs_reporte_usuario_fecha ON logs_actividad (usuario_id, created_at, tipo_accion);

-- Reporte: documentos más descargados - filtro por tipo de acción + recurso + fecha
CREATE INDEX idx_logs_reporte_descarga ON logs_actividad (tipo_accion, recurso_tipo, recurso_id, created_at);

-- Filtro combinado de normativas en búsqueda avanzada - usuario puede filtrar por múltiples campos
CREATE INDEX idx_normativas_filtro ON normativas (ambito, tematica_id, estado_normativa_id, tipo_normativa_id, anio);

-- ============================================================
-- FIN DEL SCRIPT DDL
-- ============================================================
