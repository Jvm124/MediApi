-- SEC-01: la contraseña del admin que sembró V5 quedó publicada en el repositorio.
-- Se borra ese admin solo si todavía tiene el hash publicado. Si ya se le cambió la contraseña, no se toca.
-- Desde ahora el admin inicial lo crea InicializadorDeAdmin con ADMIN_EMAIL y ADMIN_PASSWORD (docs/adr/0002).
DELETE FROM usuarios
WHERE correo = 'admin@voll.med'
  AND contrasenia = '$2a$10$5lZJvppi8gOMZ3fc62HoMe6aCsbC15BtHX8VTxhW2ivWmTGcWNQ8q';
