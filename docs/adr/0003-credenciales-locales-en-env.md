# 0003 — Leer las credenciales locales del backend desde `backend/.env`

- **Fecha:** 2026-11-09
- **Estado:** aceptada

## Contexto

El backend necesita cuatro valores que no pueden estar en el repositorio, porque es público: `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` y `ADMIN_PASSWORD` (los dos últimos vienen del [ADR 0002](0002-admin-inicial-por-variables-de-entorno.md)). Hasta ahora solo se podían dar como variables de entorno, y eso traía problemas:

- Las variables de Windows las ven todos los programas, y para que IntelliJ note un cambio hay que reiniciarlo.
- El campo *Environment variables* de IntelliJ es fácil de escribir mal: las variables van separadas con `;` y sin comillas. Al probar SEC-01, una coma hizo que `ADMIN_PASSWORD` llegara vacía.
- Nada en `backend/` decía qué variables hacían falta: había que buscarlo en el README.

El frontend ya resuelve lo mismo con `frontend/.env` y `frontend/.env.example`.

## Decisión

- `application.properties` importa `backend/.env` con `spring.config.import=optional:file:.env[.properties]`. El `[.properties]` le dice a Spring Boot que lo lea como un archivo `.properties`: una variable por línea, `NOMBRE=valor`.
- La ruta es relativa a la carpeta desde la que arranca el backend, que es `backend/` tanto en IntelliJ como con `./mvnw spring-boot:run`.
- Es opcional: si el archivo no existe, el backend arranca igual con las variables de entorno.
- `backend/.env.example` lista las variables, con las contraseñas vacías. Se copia como `backend/.env`, que el `.gitignore` de la raíz ignora.
- Si una variable está en el `.env` y también como variable de entorno, gana la variable de entorno. Así, en un servidor se configuran como variables de entorno y no hace falta el archivo.

Se comprobó con un programa aparte, que arrancó Spring Boot sin base de datos y con este `application.properties`: el `.env` se lee, `${ADMIN_EMAIL:}` toma su valor, una variable de entorno de Windows le gana al `.env` y una `ñ` se lee mal.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Variables de entorno de Windows | No hay que tocar el código. | Las ven todos los programas, hay que reiniciar IntelliJ al cambiarlas y no quedan documentadas en `backend/`. |
| Campo *Environment variables* de IntelliJ | Solo afecta a esa configuración de ejecución. | Es fácil de escribir mal y no sirve fuera de IntelliJ. |
| Cargar el `.env` desde IntelliJ, por ejemplo con el plugin EnvFile | No hay que tocar el código. | Solo funciona dentro de IntelliJ. |
| `application-local.properties` con un perfil `local` | Es la forma clásica de Spring. | Hay que activar el perfil en cada arranque y no se parece al `.env` del frontend. |
| La librería `spring-dotenv` | Entiende todo el formato `.env`, comillas incluidas. | Es una dependencia más para algo que Spring Boot ya hace solo. |
| **`spring.config.import` con `backend/.env`** (elegida) | Funciona igual en IntelliJ y en la terminal, no agrega dependencias y se parece al frontend. | El archivo se lee como `.properties`: las comillas y los espacios al final cuentan como parte del valor, la `\` se escribe `\\` y las tildes y la `ñ` se leen mal. |

## Consecuencias

- Para trabajar en local basta con copiar `backend/.env.example` como `backend/.env` y completar los valores. No hay que configurar nada en IntelliJ.
- Si `DB_PASSWORD` o `JWT_SECRET` siguen definidas en Windows, mandan esas y se ignora lo que diga el `.env`. Lo mismo pasa con el campo *Environment variables* de IntelliJ.
- Los valores del `.env` no deben llevar tildes ni `ñ`. Una contraseña con `ñ` se guardaría distinta de la que se escribe al iniciar sesión.
- El `.env` guarda las contraseñas en texto plano en la PC, igual que las variables de Windows o la configuración de IntelliJ. Nunca debe subirse al repositorio.
- Las pruebas no lo necesitan: `./mvnw test` pasa sin él.
