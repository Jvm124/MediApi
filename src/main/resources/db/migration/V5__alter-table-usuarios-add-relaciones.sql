-- Agregar columnas de relación a usuarios
ALTER TABLE usuarios ADD COLUMN paciente_id BIGINT;
ALTER TABLE usuarios ADD COLUMN medico_id BIGINT;

-- Crear las foreign keys hacia pacientes y medicos
ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_paciente_id
        FOREIGN KEY (paciente_id) REFERENCES pacientes(id);

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_medico_id
        FOREIGN KEY (medico_id) REFERENCES medicos(id);
ALTER TABLE consultas ADD COLUMN estado VARCHAR(50);
UPDATE consultas SET estado = 'PROGRAMADA' WHERE estado IS NULL;
ALTER TABLE consultas MODIFY estado VARCHAR(50) NOT NULL;
ALTER TABLE usuarios CHANGE COLUMN login correo VARCHAR(100) NOT NULL;