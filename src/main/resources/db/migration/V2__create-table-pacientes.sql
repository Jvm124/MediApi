CREATE TABLE pacientes(
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          nombre VARCHAR(100) NOT NULL,
                          activo TINYINT(1),
                          email VARCHAR(100) NOT NULL UNIQUE,
                          telefono VARCHAR(20),
                          documento VARCHAR(12) NOT NULL UNIQUE,
                          calle VARCHAR(100) NOT NULL,
                          numero VARCHAR(20),
                          complemento VARCHAR(100),
                          barrio VARCHAR(100) NOT NULL,
                          ciudad VARCHAR(100) NOT NULL,
                          estado VARCHAR(100) NOT NULL,
                          codigo_postal VARCHAR(12) NOT NULL,

                          PRIMARY KEY(id)
);