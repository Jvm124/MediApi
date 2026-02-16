create table pacientes(
    id bigint not null auto_increment,
    nombre varchar(100) not null,
    activo tinyInt,
    email varchar(100) not null unique,
    telefono varchar(20),
    documento varchar(12) not null unique,
    calle varchar(100) not null,
    numero varchar(20),
    complemento varchar(100),
    barrio varchar(100) not null,
    ciudad varchar(100) not null,
    estado char(100) not null,
    codigo_postal varchar(12) not null,

    primary key(id)

);