ALTER TABLE usuarios ADD COLUMN estado VARCHAR(20);
UPDATE usuarios SET estado = 'ACTIVO' WHERE activo = true;
UPDATE usuarios SET estado = 'BAJA' WHERE activo = false OR activo IS NULL;
ALTER TABLE usuarios DROP COLUMN activo;
ALTER TABLE usuarios MODIFY estado VARCHAR(20) NOT NULL;