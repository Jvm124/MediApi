# 0004 — Vaciar la migración V5 y reparar su checksum

- **Fecha:** 2026-11-10
- **Estado:** aceptada. Cambia lo que decía el [ADR 0002](0002-admin-inicial-por-variables-de-entorno.md) sobre `V5`.

## Contexto

`V5` sembraba al usuario `admin@voll.med` con la contraseña que quedó publicada (SEC-01), y lo hacía en `vollmed_api.usuarios`, con el nombre de la base escrito a mano. Es el hallazgo CAL-04 de la [revisión inicial](../revisiones/0001-revision-inicial.md). En una base con otro nombre:

- Si el servidor no tiene una base `vollmed_api`, las migraciones fallan en `V5`. Con una base de Testcontainers llamada `test`, Flyway se detiene con «INSERT command denied … for table 'usuarios'».
- Si el servidor sí la tiene, como el MySQL local, `V5` intenta volver a meter al admin publicado en `vollmed_api`, donde `V7` ya no se vuelve a ejecutar.

Por eso las pruebas con MySQL tenían que llamar `vollmed_api` a su base.

El ADR 0002 decidió no tocar `V5` porque ya estaba aplicada. Flyway guarda en la tabla `flyway_schema_history` un checksum de cada migración aplicada, un número que calcula con su texto, y no deja arrancar la aplicación si el archivo cambia.

## Decisión

- `V5` queda solo con comentarios y no inserta nada. El admin inicial lo sigue creando `InicializadorDeAdmin` (ADR 0002), y `V7` sigue borrando al admin publicado en las bases que lo tenían.
- El archivo conserva su nombre, `V5__insert-usuario-admin.sql`, aunque ya no inserta nada, porque Flyway también compara la descripción de las migraciones aplicadas.
- Cada base que ya aplicó `V5` actualiza su checksum una vez, con el `UPDATE` de las consecuencias. Las bases nuevas, como las de las pruebas, no necesitan nada.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Quitar solo `vollmed_api.` de `V5` | El cambio es mínimo. | Cada base nueva seguiría recibiendo al admin publicado hasta que `V7` lo borra. Si la migración se corta entre las dos, se queda. |
| Dejar `V5` y llamar siempre `vollmed_api` a la base | No hay que reparar nada. | Es el problema de CAL-04: una base con otro nombre falla o escribe en la equivocada. |
| `flyway repair` | Es la herramienta de Flyway para esto. | El proyecto no tiene la CLI ni el plugin de Maven de Flyway, y `repair` reajusta todas las migraciones, no solo `V5`. |
| **`V5` vacía y un `UPDATE` del checksum** (elegida) | Ninguna base nueva recibe al admin publicado, y la reparación toca una sola fila. | Hay que ejecutar el `UPDATE` a mano, una vez, en cada base que ya tenía `V5`. |

## Consecuencias

- Las migraciones funcionan en una base con cualquier nombre. Las pruebas con MySQL usan una base llamada `test`, y `AplicacionCompletaConMySQLTest` comprueba que una base nueva no trae ningún usuario.
- Una base que ya tenía `V5` aplicada no arranca con el `V5` nuevo hasta repararla. Flyway lo avisa con «Migration checksum mismatch for migration version 5». La reparación, una sola vez y solo en la base de MediApi (en local, `vollmed_api`):

  ```sql
  UPDATE vollmed_api.flyway_schema_history
  SET checksum = 1620849086
  WHERE version = '5' AND checksum = -1703459837;
  ```

  - Cambia 1 fila, y si se repite, 0. En local se puede ejecutar desde IntelliJ, en la consola de la base (ventana Database).
  - Flyway calcula el checksum con el texto de cada línea, sin los fines de línea, así que da igual que el archivo tenga CRLF o LF.
  - Si el mensaje de Flyway muestra otro valor en «Applied to database», ese es el valor viejo que hay que poner en el `WHERE`.
  - Para saber si una base tiene `V5` aplicada: `SELECT checksum FROM flyway_schema_history WHERE version = '5'`. Si la tabla no existe, la base nunca se migró y no hay nada que reparar.
  - Para volver a una versión anterior de MediApi con una base ya reparada, se ejecuta el mismo `UPDATE` con los dos números al revés.
- La reparación se ensayó antes en un MySQL desechable con las migraciones viejas. Sin ella, Flyway no deja arrancar; después del `UPDATE`, valida y no aplica nada.
- Si alguna vez se vuelve a editar una migración aplicada, su checksum cambia y hay que hacer lo mismo. Lo normal es no editarlas y añadir una nueva.
