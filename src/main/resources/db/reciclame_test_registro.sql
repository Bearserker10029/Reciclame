-- ============================================================
-- SCRIPT DE PRUEBA: AC1 + AC3 - Registro con correo corporativo
-- Ejecutar DESPUÉS de reciclame_ddl.sql + reciclame_data.sql
-- ============================================================

USE reciclame_db;

-- ── Limpiar solicitudes anteriores de prueba ─────────────────
DELETE FROM solicitudes_registro
WHERE correo_corporativo IN (
  'pendiente@reciclame.org',
  'aprobado@reciclame.org',
  'nuevo@reciclame.org',
  'dnidupe@reciclame.org'
);

-- ── CASO 1: Correo con solicitud PENDIENTE ───────────────────
-- Esperado: "Ya existe una solicitud pendiente. Espera aprobación."
INSERT INTO solicitudes_registro
  (nombre_completo, dni, correo_corporativo, empresa_nombre, estado, estado_activo, created_at)
VALUES
  ('Ana García Torres', '72345001', 'pendiente@reciclame.org', 'EcoPerú S.A.C.',
   'PENDIENTE', TRUE, NOW() - INTERVAL 1 DAY);

-- ── CASO 2: Correo con solicitud APROBADA ───────────────────
-- Esperado: "Ya tienes una cuenta activa. Inicia sesión."
INSERT INTO solicitudes_registro
  (nombre_completo, dni, correo_corporativo, empresa_nombre, estado, estado_activo, created_at)
VALUES
  ('Carlos Mendoza', '72345002', 'aprobado@reciclame.org', 'Recicla Más S.A.',
   'APROBADA', TRUE, NOW() - INTERVAL 3 DAY);

-- ── CASO 3: DNI ya registrado en otra solicitud ──────────────
-- Usar dni='72345001' (ya insertado arriba) con correo distinto
-- Esperado: "Este DNI ya está registrado."

-- ── CASO 4: Usuario activo en tabla usuarios ─────────────────
-- Los usuarios del data.sql (socio@reciclame.com etc.) usan @reciclame.com,
-- no @reciclame.org, por lo que no bloquearían por correo.
-- Para probar bloqueo por usuario activo, insertar uno con @reciclame.org:
INSERT INTO usuarios
  (nombre_completo, dni, correo_corporativo, password_hash,
   cargo, rol_id, intentos_fallidos, cuenta_bloqueada, tfa_activo,
   estado_cuenta, estado_activo, created_at, updated_at)
VALUES
  ('Luis Paredes Vega', '72345003', 'activo@reciclame.org', 'test123',
   'Socio',
   (SELECT id FROM roles WHERE nombre = 'SOCIO'),
   0, 0, 0,
   'ACTIVO', 1, NOW(), NOW());

-- ── VERIFICACIÓN: estado actual antes de las pruebas ─────────
SELECT '=== SOLICITUDES DE REGISTRO ===' AS info;
SELECT id, nombre_completo, correo_corporativo, dni, estado, created_at
FROM solicitudes_registro
ORDER BY created_at DESC;

SELECT '=== USUARIOS CON @reciclame.org ===' AS info;
SELECT id, nombre_completo, correo_corporativo, estado_cuenta
FROM usuarios
WHERE correo_corporativo LIKE '%@reciclame.org';
