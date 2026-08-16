CREATE TABLE usuarios(
     id BIGINT NOT NULL AUTO_INCREMENT,
     correo VARCHAR(100) NOT NULL,
     rol VARCHAR(50) NOT NULL,
     contrasenia VARCHAR(255) NOT NULL,
     activo TINYINT(1),
     paciente_id BIGINT,
     medico_id BIGINT,

     PRIMARY KEY (id),
     CONSTRAINT fk_usuarios_paciente_id FOREIGN KEY (paciente_id) REFERENCES pacientes(id),
     CONSTRAINT fk_usuarios_medico_id FOREIGN KEY (medico_id) REFERENCES medicos(id),
     CONSTRAINT uk_usuarios_correo UNIQUE (correo)

);