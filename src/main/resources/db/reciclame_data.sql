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

-- Verificar inserciones
SELECT 'Usuarios creados:' AS info;
SELECT id, nombre_completo, correo_corporativo, estado_cuenta FROM usuarios;