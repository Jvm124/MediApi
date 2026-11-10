# 0002 — Crear el administrador inicial con variables de entorno

- **Fecha:** 2026-11-09
- **Estado:** aceptada

## Contexto

La migración `V5` sembraba al usuario `admin@voll.med` con un hash de BCrypt fijo, y la contraseña original estaba escrita en un test (`GenerarHashTest`). Como el repositorio es público, cualquiera podía entrar como ADMINISTRADOR en una base creada con esas migraciones. Es el hallazgo SEC-01 de la [revisión inicial](../revisiones/0001-revision-inicial.md).

Hace falta al menos un administrador para dar de alta a médicos y personal, pero su contraseña no puede estar en el código.

## Decisión

- El admin inicial se crea **al arrancar la aplicación**, con las variables de entorno `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Lo hace `InicializadorDeAdmin`, un `ApplicationRunner`, y solo si todavía no existe ningún ADMINISTRADOR. En los arranques siguientes no hace nada.
- Si faltan las variables, la aplicación arranca igual y lo avisa en el log.
- La aplicación **no arranca**, y explica por qué, si la contraseña tiene menos de 8 caracteres o más de 72 bytes (el límite de BCrypt), o si el correo ya es de otro usuario.
- La migración `V7` borra al admin de `V5` solo si todavía tiene el hash publicado. Si ya se le cambió la contraseña, no lo toca.
- `V5` no se modifica, porque ya está aplicada y Flyway rechazaría el cambio de checksum.
- Se borró `GenerarHashTest`. El historial de git no se reescribe: la contraseña sigue en commits viejos, pero ya no sirve en ninguna base.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Una migración con un hash nuevo | Es sencillo. | El hash queda en el repo y todas las bases tendrían la misma contraseña. |
| Crear el admin a mano con SQL | No queda nada en el código. | Hay que generar el hash aparte, y es fácil olvidarlo en cada base nueva. |
| Editar `V5` y ejecutar `flyway repair` | Ninguna base nueva recibiría el admin publicado. | Hay que reparar cada base que ya aplicó `V5`. Queda para cuando se corrija CAL-04. |
| **Variables de entorno y un `ApplicationRunner`** (elegida) | La contraseña nunca está en el código, cada entorno tiene la suya y se puede probar con pruebas unitarias. | Hay que configurar dos variables más. |

## Consecuencias

- Para tener un admin en local hay que definir `ADMIN_EMAIL` y `ADMIN_PASSWORD` en IntelliJ. Se puede volver a usar `admin@voll.med`: lo que cambia es la contraseña. Desde el [ADR 0003](0003-credenciales-locales-en-env.md) también se pueden poner en `backend/.env`.
- **Ojo con CAL-04:** `V5` inserta en `vollmed_api.usuarios`, con el nombre de la base escrito a mano. Si se migra otra base, por ejemplo `vollmed_api_test`, `V5` puede volver a insertar el admin publicado en `vollmed_api`, donde `V7` ya no se vuelve a ejecutar. Hay que corregir `V5` antes de migrar otra base, a más tardar al empezar las pruebas con base de datos (fase 3).
