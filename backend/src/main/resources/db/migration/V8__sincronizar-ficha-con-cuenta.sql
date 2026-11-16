-- BUG-05: la ficha del médico o del paciente solo está activa si su cuenta está ACTIVA (docs/adr/0006).
-- Desde BUG-05 la aplicación cambia las dos a la vez. Esta migración corrige, una sola vez, las que ya estaban
-- desincronizadas. Solo apaga: desactiva fichas y da de baja cuentas, nunca activa nada.
-- Ojo: medicos.estado y pacientes.estado son parte de la dirección. El estado de la cuenta es usuarios.estado.

-- 1. Las fichas activas cuya cuenta está suspendida o de baja se desactivan.
UPDATE medicos m
SET m.activo = false
WHERE m.activo = true
  AND m.id IN (SELECT u.medico_id FROM usuarios u WHERE u.estado IN ('SUSPENDIDO', 'BAJA'));

UPDATE pacientes p
SET p.activo = false
WHERE p.activo = true
  AND p.id IN (SELECT u.paciente_id FROM usuarios u WHERE u.estado IN ('SUSPENDIDO', 'BAJA'));

-- 2. Las cuentas ACTIVO cuya ficha está desactivada, o sin valor, pasan a BAJA, como hace ahora la aplicación al
--    dar de baja desde la ficha. Si hace falta, un administrador las «Reincorpora» y vuelven la cuenta y la ficha.
UPDATE usuarios u
SET u.estado = 'BAJA'
WHERE u.estado = 'ACTIVO'
  AND u.medico_id IN (SELECT m.id FROM medicos m WHERE m.activo = false OR m.activo IS NULL);

UPDATE usuarios u
SET u.estado = 'BAJA'
WHERE u.estado = 'ACTIVO'
  AND u.paciente_id IN (SELECT p.id FROM pacientes p WHERE p.activo = false OR p.activo IS NULL);
