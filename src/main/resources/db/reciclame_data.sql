-- ============================================================
-- RECICLAME - Datos de Prueba
-- Restaurado: usuarios completos + empresas
-- ============================================================

USE reciclame_db;

-- Empresas (INSERT IGNORE para no duplicar si ya existen del DDL)
INSERT IGNORE INTO empresas (razon_social, ruc, sector, estado_activo, created_at, updated_at) VALUES
('ReciclamE S.A.C.',           '20512345678', 'Gestion ambiental',      1, NOW(), NOW()),
('Plasticos del Peru S.A.',    '20498765432', 'Plasticos PET',           1, NOW(), NOW()),
('Envases Tetra Peru',         '20587654321', 'Envases Tetra',           1, NOW(), NOW()),
('EcoIndustrias Peru',         '20611223344', 'Reciclaje industrial',    1, NOW(), NOW());

-- Usuarios (INSERT IGNORE para no duplicar)
INSERT IGNORE INTO usuarios (
    nombre_completo, dni, correo_corporativo, password_hash,
    cargo, foto_url, rol_id, empresa_id,
    intentos_fallidos, cuenta_bloqueada, tfa_activo,
    estado_cuenta, estado_activo, created_at, updated_at
) VALUES
(
    'Admin General',
    '71234567',
    'admin@reciclame.com',
    'admin123',
    'Administrador del Sistema',
    NULL,
    (SELECT id FROM roles WHERE nombre = 'ADMINISTRADOR'),
    NULL,
    0, 0, 0,
    'ACTIVO', 1, NOW(), NOW()
),
(
    'Super Administrador',
    '71234568',
    'superadmin@reciclame.com',
    'super123',
    'SuperAdmin',
    NULL,
    (SELECT id FROM roles WHERE nombre = 'SUPERADMIN'),
    NULL,
    0, 0, 0,
    'ACTIVO', 1, NOW(), NOW()
),
(
    'Socio Demo',
    '71234569',
    'socio@reciclame.com',
    'socio123',
    'Socio Empresarial',
    NULL,
    (SELECT id FROM roles WHERE nombre = 'SOCIO'),
    (SELECT id FROM empresas WHERE ruc = '20512345678'),
    0, 0, 0,
    'ACTIVO', 1, NOW(), NOW()
),
(
    'Visualizador Demo',
    '71234570',
    'visualizador@reciclame.com',
    'vis123',
    'Visualizador',
    NULL,
    (SELECT id FROM roles WHERE nombre = 'VISUALIZADOR'),
    NULL,
    0, 0, 0,
    'ACTIVO', 1, NOW(), NOW()
),
(
    'Usuario Pendiente',
    '71234571',
    'pendiente@reciclame.com',
    'password',
    'Nuevo Usuario',
    NULL,
    (SELECT id FROM roles WHERE nombre = 'SOCIO'),
    NULL,
    0, 0, 0,
    'PENDIENTE', 1, NOW(), NOW()
);

-- ── NOTIFICACIONES ──────────────────────────────────────────
INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, enlace_recurso, leida, fecha_lectura, estado_activo, created_at) VALUES
-- Socio: 2 no leídas + 1 leída
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'socio@reciclame.com'),
  'NUEVA_NORMATIVA',
  'Nueva normativa publicada',
  'Se ha agregado el Decreto Legislativo 1501 — Ley del Plástico al Repositorio Normativo.',
  '/socio/normativo',
  FALSE, NULL, TRUE, NOW() - INTERVAL 2 HOUR
),
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'socio@reciclame.com'),
  'NUEVO_ESTUDIO',
  'Nuevo estudio disponible',
  'El estudio "Análisis de Flujo de Materiales 2025" ya está disponible en la sección de Estudios.',
  '/socio/estudios',
  FALSE, NULL, TRUE, NOW() - INTERVAL 1 DAY
),
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'socio@reciclame.com'),
  'MENSAJE_ADMIN',
  'Cuenta aprobada',
  'Tu solicitud de acceso fue aprobada. Bienvenido al Repositorio Circular Recíclame.',
  NULL,
  TRUE, NOW() - INTERVAL 5 DAY, TRUE, NOW() - INTERVAL 6 DAY
),
-- Admin: 1 no leída + 1 leída
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'admin@reciclame.com'),
  'NUEVO_ESTUDIO',
  'Estudio pendiente de validación',
  'El estudio "Hoja de Ruta Economía Circular Perú 2030" está pendiente de revisión y validación.',
  '/admin/documentos',
  FALSE, NULL, TRUE, NOW() - INTERVAL 4 HOUR
),
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'admin@reciclame.com'),
  'DOCUMENTO_VALIDADO',
  'Normativa validada correctamente',
  'La normativa "NTP 900.058-2019 Gestión de Residuos" fue validada y publicada exitosamente.',
  '/admin/documentos',
  TRUE, NOW() - INTERVAL 2 DAY, TRUE, NOW() - INTERVAL 3 DAY
),
-- SuperAdmin: 1 no leída
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'superadmin@reciclame.com'),
  'SISTEMA',
  'Nuevo usuario registrado',
  'El usuario "Usuario Pendiente" ha solicitado acceso al sistema y espera aprobación.',
  '/superadmin/usuarios',
  FALSE, NULL, TRUE, NOW() - INTERVAL 1 HOUR
);

-- ── PREFERENCIAS DE NOTIFICACIÓN (defaults para usuarios demo) ──
INSERT IGNORE INTO preferencias_notificacion
  (usuario_id, email_habilitado, push_habilitado, sms_habilitado,
   notif_nuevas_normativas, notif_nuevos_estudios, notif_cambios_seguidos,
   notif_mensajes_admin, notif_digesto_semanal, frecuencia_email,
   created_at, updated_at)
VALUES
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'socio@reciclame.com'),
  TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, 'DIARIA', NOW(), NOW()
),
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'admin@reciclame.com'),
  TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, 'INMEDIATA', NOW(), NOW()
),
(
  (SELECT id FROM usuarios WHERE correo_corporativo = 'superadmin@reciclame.com'),
  TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, 'SEMANAL', NOW(), NOW()
);

-- Verificar inserciones
SELECT 'Usuarios creados:' AS info;
SELECT id, nombre_completo, correo_corporativo, estado_cuenta FROM usuarios;
SELECT 'Notificaciones creadas:' AS info;
SELECT n.id, u.correo_corporativo, n.tipo, n.titulo, n.leida, n.created_at
FROM notificaciones n JOIN usuarios u ON n.usuario_id = u.id ORDER BY n.created_at DESC;
SELECT 'Preferencias creadas:' AS info;
SELECT p.id, u.correo_corporativo, p.email_habilitado, p.notif_nuevas_normativas,
       p.notif_nuevos_estudios, p.frecuencia_email
FROM preferencias_notificacion p JOIN usuarios u ON p.usuario_id = u.id;