# 0005 — Limitar los intentos fallidos de login en memoria

- **Fecha:** 2026-11-12
- **Estado:** aceptada

## Contexto

El login (`POST /login`) no limitaba los intentos. Un programa podía probar miles de contraseñas seguidas contra una cuenta, o una misma contraseña contra muchas cuentas. Es el hallazgo SEC-04 de la [revisión inicial](../revisiones/0001-revision-inicial.md).

El frontend tiene un reCAPTCHA de Google, pero desactivado: el componente está comentado en `LoginPage.tsx`, el login envía `recaptchaToken: ''` y el backend nunca verificó ningún token. No protege nada.

Restricciones:

- Solo herramientas gratuitas. El backend corre en un solo servidor.
- El límite no debe revelar qué correos tienen cuenta.
- MySQL compara los correos de `usuarios` sin distinguir mayúsculas, tildes ni letras de ancho completo (la colación `utf8mb4_0900_ai_ci`). Se ensayó en un MySQL 8.4 desechable, con Testcontainers:
  - Con un usuario guardado como `ana@ejemplo.com`, la búsqueda por correo también lo encuentra con `ANA@ejemplo.com`, `Ána@ejemplo.com`, `ãna@ejemplo.com`, `äna@ejemplo.com` y `ａna@ejemplo.com` (con una a de ancho completo).
  - A `jose@ejemplo.com` lo encuentra con `josé@ejemplo.com` y con `jøse@ejemplo.com`.
  - No lo encuentra con espacios al principio o al final, ni con una а cirílica.
  - Con cualquiera de las variantes que lo encuentran se entra en la cuenta, si la contraseña es correcta.

## Decisión

- `LimiteDeIntentosDeLogin` cuenta los fallos de login de dos formas y, al llegar al límite, bloquea 15 minutos:
  - **Por correo:** 5 fallos en 15 minutos. Frena a quien intenta adivinar la contraseña de una cuenta. Mientras dura el bloqueo, ese correo no entra ni con la contraseña correcta. Un login correcto pone el contador a cero.
  - **Por IP:** 50 fallos en 15 minutos, con cualquier correo. Frena a quien prueba una misma contraseña en muchas cuentas. El límite es más alto porque en una clínica varias personas pueden compartir la IP. Un login correcto no lo pone a cero: si no, quien tuviera una cuenta válida podría reiniciarlo entrando con ella entre intento e intento.
- Los 15 minutos se cuentan desde el primer fallo. Pasado ese tiempo, y si no hay bloqueo, el contador vuelve a empezar.
- Cuentan como fallo una contraseña incorrecta, un correo que no existe y una cuenta suspendida. Un correo que no existe se bloquea igual que uno que existe, para que el bloqueo no revele qué correos tienen cuenta. Los errores del servidor, como una base de datos que no responde, no cuentan: no son culpa del usuario.
- **Los fallos se cuentan con el correo tal como está guardado.** Antes de comprobar el límite, `AutenticacionService.correoGuardado` busca al usuario y devuelve su correo. Si se contara solo con el correo en minúsculas, cada variante del contexto daría 5 intentos más contra la misma cuenta. En Java no hay una forma sencilla de comparar igual que MySQL: `Normalizer` quita las tildes, pero no convierte `ø` en `o`. Si no encuentra a nadie, se cuenta con el correo recibido, en minúsculas y sin espacios alrededor.
- Con el correo o la IP bloqueados, el login responde **429 Too Many Requests** sin mirar la contraseña. La respuesta lleva la cabecera `Retry-After`, con los segundos que faltan, y un `detail` que el frontend ya muestra, como «Demasiados intentos fallidos. Vuelve a intentarlo en 15 minutos.». Los minutos se redondean hacia arriba.
- Los contadores viven en memoria, en dos `ConcurrentHashMap`, sin librerías ni tablas nuevas. Cuando uno pasa de 10.000 contadores, se borran los vencidos, para que la memoria no crezca sin límite. La hora sale del `Clock` de la aplicación (`ConfiguracionDelReloj`), así que las pruebas adelantan los 15 minutos sin esperar (`RelojDePrueba`).
- El reCAPTCHA desactivado se quita del frontend, en un commit aparte. La idea de un captcha de verdad queda en `docs/ideas/ideas-y-debates.md`.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Bucket4j, como proponía la revisión | Es una librería conocida para limitar peticiones. | Limita peticiones, no fallos: habría que adaptarla para que un login correcto reinicie el contador y para bloquear 15 minutos. Es una dependencia más para dos contadores. |
| Contadores en una tabla de la base de datos | Sobreviven a un reinicio y sirven con varios servidores. | Hace falta una migración nueva y una escritura en cada fallo. Con un solo servidor no compensa todavía. |
| Bloquear la cuenta hasta que un administrador la desbloquee | Frena del todo la fuerza bruta contra una cuenta. | Cualquiera puede dejar a otra persona sin entrar con 5 fallos a propósito, y hace falta un administrador cada vez. |
| Activar el reCAPTCHA de verdad | Distingue a personas de programas. | Hay que verificar el token en el backend, con una clave secreta y una llamada a Google en cada login, y depende de un servicio externo. |
| **Contadores en memoria por correo y por IP, con bloqueo de 15 minutos** (elegida) | Sin dependencias ni tablas nuevas, y fácil de probar con el reloj. | Los contadores se pierden al reiniciar y no se comparten entre servidores. |

## Consecuencias

- Al reiniciar el backend, los contadores se borran. En desarrollo sirve para desbloquear un correo sin esperar.
- Con varios servidores, cada uno contaría por su cuenta: 5 fallos por correo en cada uno. Si se llega a eso, los contadores tendrán que pasar a la base de datos o a un almacén compartido, como Redis.
- Detrás de un proxy (nginx, un balanceador), `request.getRemoteAddr()` devuelve la IP del proxy, y todos los usuarios compartirían el límite de 50 fallos. Antes de desplegar así hay que configurar `server.forward-headers-strategy` para que Spring tome la IP de la cabecera `X-Forwarded-For`. El proxy debe escribir esa cabecera él mismo, sin aceptar la que envía el cliente.
- Quien conozca un correo puede bloquearlo 15 minutos a propósito con 5 contraseñas incorrectas. Es el precio de frenar la fuerza bruta: el bloqueo es corto y no necesita a un administrador.
- Cada login busca al usuario una vez más en la base de datos (`correoGuardado`), además de la búsqueda de Spring Security.
- Si cambia la colación de `usuarios.correo`, hay que revisar con qué correo se cuentan los fallos.
