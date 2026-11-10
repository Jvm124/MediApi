# 0001 — Revisión inicial: seguridad, errores y mejoras

- **Fecha:** 2026-11-09
- **Alcance:** todo el backend (`backend/src`, migraciones, configuración y tests) y el frontend (`frontend/src`, configuración y dependencias).
- **Cómo se hizo:** lectura del código, `./mvnw test`, `npm run build`, `npm run lint`, `npm audit` y comparación de versiones con Maven Central y npm.
- **Qué NO se hizo:** no se levantó la aplicación completa contra la base de datos. Los comportamientos marcados como *(análisis)* salen de leer el código y se confirmarán con una prueba al corregirlos. Los componentes estáticos de la landing no se revisaron a fondo.

## Resumen

| Nivel | Significa | Cantidad |
|---|---|---|
| 🔴 Crítico | Arreglar ya | 1 |
| 🟠 Alto | Arreglar antes de desplegar o mostrar a usuarios reales | 6 |
| 🟡 Medio | Errores de lógica o de experiencia de usuario | 11 |
| 🔵 Bajo | Limpieza y buenas prácticas | 7 |

Lo más urgente: **la contraseña del administrador inicial está publicada en GitHub** (SEC-01). Después vienen el manejo del token (SEC-02 y SEC-03) y las dependencias con vulnerabilidades conocidas (SEC-06).

## Lo que ya está bien hecho ✅

Antes de los problemas, lo que está bien (y conviene mantener):

- Contraseñas guardadas con **BCrypt**. Los secretos (`DB_PASSWORD`, `JWT_SECRET`) se leen de variables de entorno, y `include-stacktrace=never` evita mostrar trazas al cliente.
- Cada endpoint tiene `@PreAuthorize` y, lo más valioso, **se verifica que el recurso sea del usuario**: un paciente no puede cancelar ni dar de baja lo de otro, y un médico solo atiende sus propias consultas (`ReservaDeConsultas.verificarPermisoDeCancelacion`). Así se evita el **IDOR**, uno de los fallos más comunes en APIs.
- Al reservar, el paciente se toma del token y no del cuerpo de la petición.
- Las reglas de negocio están separadas en validadores (patrón *Strategy*), lo que facilita mucho las pruebas unitarias.
- La base de datos se versiona con Flyway, y los DTOs son `record` con Bean Validation.
- En el frontend: validación con zod, rutas protegidas por rol y token en `sessionStorage` en vez de `localStorage`. Además, React escapa el HTML por defecto.

## Hallazgos

### 🔴 Crítico

| ID | Dónde | Problema | Solución |
|---|---|---|---|
| SEC-01 | `backend/src/test/.../GenerarHashTest.java:10` y `db/migration/V5__insert-usuario-admin.sql:1` | La contraseña del administrador inicial (`admin@voll.med`) está **en texto plano** en un test, y el repo es **público**. Se verificó que coincide con el hash que inserta V5. Cualquiera que lea el repo puede entrar como ADMINISTRADOR en cualquier base creada con estas migraciones. | Ver [detalle de SEC-01](#sec-01--contraseña-del-administrador-publicada). |

### 🟠 Alto

| ID | Dónde | Problema | Solución |
|---|---|---|---|
| SEC-02 | `infra/security/SecurityFilter.java:29`, `SecurityConfigurations.java:36`, `frontend/src/api/axiosClient.ts:30` | *(análisis)* Un token vencido o inválido hace que el filtro lance una excepción que ningún `@RestControllerAdvice` atrapa. Además no hay *entry point*, y Spring responde **403** a lo no autenticado. El frontend solo cierra sesión con **401**, así que cuando vence el token (2 h) todas las pantallas fallan hasta recargar. | [Detalle de SEC-02 y SEC-03](#sec-02-y-sec-03--manejo-del-token). |
| SEC-03 | `infra/security/SecurityFilter.java:30-31` | El filtro autentica al usuario del token sin revisar su estado: un usuario **suspendido o dado de baja sigue entrando hasta 2 h**. Si el usuario ya no existe, `findByCorreo` devuelve `null` y se produce una `NullPointerException`. | Mismo detalle que SEC-02. |
| SEC-04 | `frontend/src/pages/LoginPage.tsx:48`, `AutenticacionController` | El login no limita intentos y el reCAPTCHA está desactivado (se envía vacío). El backend tampoco verifica ningún captcha, así que se puede probar contraseñas por fuerza bruta. | Limitar intentos por IP y por correo (por ejemplo con Bucket4j) y bloquear unos minutos tras varios fallos. Si se reactiva el reCAPTCHA, **verificar el token en el backend**: sin esa verificación no protege nada. |
| SEC-05 | `DatosRegistroUsuario.java:9`, `DatosRegistroPaciente.java:16`, `DatosRegistroMedico.java:21`, `frontend/src/schemas/registrarMedicoSchema.ts:21` | El backend solo exige `@NotBlank`: llamando a la API directamente se acepta la contraseña `"1"`. El front pide 8 caracteres con complejidad para pacientes, pero solo 6 para médicos y personal. Además, BCrypt solo admite 72 bytes, y una contraseña más larga hace fallar el registro con un **500**. | Una anotación propia `@ContraseniaSegura` (mínimo 8, máximo 72, con mayúscula, minúscula y número) en los tres DTOs, y el mismo criterio en los esquemas del front. |
| SEC-06 | `frontend/package.json`, `backend/pom.xml` | **Dependencias con vulnerabilidades conocidas.** `npm audit` reporta 7 en producción (6 altas): `axios` 1.16.0, `zod` 4.4.3 y `react-router-dom` 7.15.0, más otras en desarrollo. En el backend se usa Spring Boot **4.0.1** y ya existe el **4.0.9** (ocho parches); `java-jwt` 4.5.1 → 4.6.2. | `npm audit fix` (dentro de las mismas versiones mayores), subir Spring Boot a 4.0.9 y activar **Dependabot** en GitHub para que avise solo. |
| SEC-07 | `infra/security/TokenService.java:35`, `ValidadorConsultaConAnticipacion.java:15`, `ValidadorHorarioAntecedencia.java:18` | La hora depende de la zona del servidor. La expiración del token suma un desfase fijo de `-05:00` a la hora local: en un servidor en UTC (lo normal en la nube y en Docker), el token dura 7 h. Las reglas de 30 min, 24 h y de 7:00 a 18:00 comparan contra la hora del servidor, mientras que el front envía la hora de Lima sin zona, así que en la nube fallarían por 5 horas. | [Detalle de SEC-07](#sec-07--zona-horaria). |

### 🟡 Medio

| ID | Dónde | Problema | Solución |
|---|---|---|---|
| BUG-01 | `ConsultaRepository.java:13`, `MedicoRepository.java:19` | **Las consultas canceladas siguen bloqueando el horario.** Las dos consultas que revisan si el médico está libre no filtran por estado, así que un horario cancelado nunca se puede volver a reservar con ese médico. | Filtrar por `estado = PROGRAMADA` en ambas. |
| BUG-02 | `frontend/src/utils/getErrorMessage.ts:4`, `TratadorDeErrores.java:26` | **Los mensajes del backend nunca llegan a la pantalla.** El front lee `data.message`, pero el backend responde texto plano (`ValidacionException`) o una lista `[{campo, error}]`. El usuario siempre ve el mensaje genérico y nunca, por ejemplo, "Ya existe un paciente con ese email". | Usar un formato de error único: `ProblemDetail` (RFC 9457), que Spring ya trae. Luego leer `data.detail` en `getErrorMessage`. |
| BUG-03 | `TratadorDeErrores.java:12` | *(análisis)* Faltan *handlers*. Con credenciales incorrectas o una cuenta suspendida se responde un **403 sin mensaje**. Un correo o documento duplicado que se cuela por concurrencia termina en `DataIntegrityViolationException`, es decir, un **500**. | Agregar *handlers* para `AuthenticationException` (401 "Credenciales inválidas" / "Cuenta suspendida"), `DataIntegrityViolationException` (409) y uno genérico (500 con un mensaje seguro y log). |
| BUG-04 | `frontend/src/types/auth.types.ts:7`, `LoginPage.tsx:14` | **El rol RECEPCIONISTA no tiene interfaz.** El admin puede crearlo, pero el front no lo conoce: al iniciar sesión no tiene ruta y vuelve al login. | Definir qué hace (reservar para pacientes, alta por mostrador, cancelar) y crear su panel, o no permitir crearlo todavía. |
| BUG-05 | `Usuario.java:67` y `:117`, `PacienteService.java:91`, `MedicoService.java:98` | **El estado del usuario y el del médico o paciente se desincronizan**, porque hay dos fuentes de verdad (`Usuario.estado` y `Medico.activo`/`Paciente.activo`). Dar de baja a un paciente deja su usuario como "ACTIVO" en el panel. Reactivar un usuario no reactiva al médico, que sigue sin poder entrar aunque el panel diga "Activo". Y dar de baja a un médico cuyo usuario ya estaba de baja lanza una excepción y revierte todo. | Un solo servicio que cambie los dos estados juntos, o dejar un único estado. |
| BUG-06 | `ReservaDeConsultas.java:61` | **Doble reserva por concurrencia.** Si dos pacientes reservan el mismo médico a la misma hora en el mismo instante, ambos pasan las validaciones. | Restricción única en la base de datos para (`medico_id`, `fecha`) en las consultas programadas, o un bloqueo. |
| BUG-07 | `AdminDashboardPage.tsx:64`, `AdminMedicosPage.tsx:37` | Las tablas de administración muestran solo los primeros 10 registros y no tienen paginación. | Agregar controles de página (el backend ya devuelve `Page`). |
| BUG-08 | `frontend/.env.example:1`, `frontend/vite.config.ts:15` | `.env.example` apunta a `http://localhost:8080/api`, pero el backend no usa ese prefijo, así que con el ejemplo tal cual falla el login. El proxy `/api` de Vite no se usa. | Dejar `http://localhost:8080` en el ejemplo y quitar el proxy, o usar el proxy y prescindir de CORS en desarrollo. |
| BUG-09 | `frontend/src/api/authApi.ts:28` | `authApi.reservar` envía una "reserva" a `/pacientes`, que es el registro de pacientes. Hoy no se usa, pero está mal. | Borrarla: la buena es `appointmentsApi.create`. |
| BUG-10 | `ReservaDeConsultas.java:100` | El médico puede marcar como **atendida una cita futura**, porque no se valida la fecha. | Permitir "atender" solo desde la hora de la cita. |
| BUG-11 | `PacienteService.registrar` | Un paciente dado de alta en mostrador **nunca podrá crear su cuenta web**, porque el auto-registro rechaza su DNI por existir ya. | Un flujo de "activar mi cuenta" que vincule el usuario nuevo con el paciente existente. |

### 🔵 Bajo

| ID | Dónde | Problema | Solución |
|---|---|---|---|
| CAL-01 | Todos los servicios y controladores (ej. `MedicoController.java:23`) | La inyección se hace por campo (`@Autowired` en atributos). | Inyección por constructor (`final` + `@RequiredArgsConstructor`). Es clave para las pruebas unitarias con Mockito y se hará en esa etapa. |
| CAL-02 | Varios | `ResponseEntity` sin tipo. `@Transactional` de dos paquetes distintos (`ReservaDeConsultas.java:12` usa el de `jakarta`). Nombres confusos (`Consulta.idMedico` es un `Medico`, `Consulta.java:27`). Un comentario sobrante en `CorsConfig`. El `HelloController` del curso. Y `DELETE` con cuerpo para cancelar (`ConsultaController.java:56`), que algunos proxies descartan. | Limpieza gradual. Para cancelar, mejor `POST /consultas/{id}/cancelar`. |
| CAL-03 | `application.properties:4` y `:8`, `SecurityConfigurations.java:35`, `CorsConfig.java:17` | Todo en una sola configuración: la app se conecta a MySQL como **root**, `show-sql=true`, Swagger abierto a todos y el origen de CORS fijo en el código. Además, `allowCredentials` no hace falta con el JWT en la cabecera, y `open-in-view` está activo por defecto. | Perfiles `dev` y `prod`, un usuario de BD con permisos mínimos, variables `DB_URL`/`DB_USERNAME`, Swagger desactivado en producción, CORS por configuración y `spring.jpa.open-in-view=false`. |
| CAL-04 | `V5__insert-usuario-admin.sql:1` | V5 trae fijo el nombre de la base (`vollmed_api.usuarios`). En la base de tests (`vollmed_api_test`) o en Docker con otro nombre, inserta en la base equivocada o falla. | Corregirlo junto con SEC-01. Ojo: una migración ya aplicada no se edita sin `flyway repair`. |
| CAL-05 | Frontend | Código muerto: `getRol`/`setRol` en `tokenStorage.ts:24` con un comentario desactualizado, `useDebounce.ts` vacío, `sanitize.ts` sin usos y enlaces `#` en `Sidebar`. El lint marca 8 errores. `@types/react` sigue en 18 con React 19. Las metas de `index.html:8` no tienen efecto: `X-Content-Type-Options` solo funciona como cabecera HTTP del servidor. La marca es inconsistente (HEALTHCARE, Voll.med, MediApp, MediApi) y el enlace a `/terms` no existe. | Limpieza en un PR propio. |
| CAL-06 | `frontend/src/utils/tokenStorage.ts:16` | El token en `sessionStorage` lo puede leer cualquier script si hubiera un XSS. Para empezar es aceptable. | Para debatir: cookie `HttpOnly`. Ver [ideas y debates](../ideas/ideas-y-debates.md). |
| TEST-01 | `backend/src/test` | **No hay pruebas reales.** `GenerarHashTest` no verifica nada (y es el que expone la contraseña), y `MedicoRepositoryTest` está entero comentado. Cobertura: 0. | Etapa de pruebas unitarias: validadores, `ReservaDeConsultas`, `Usuario` (transiciones de estado) y el filtro de seguridad. |

## Detalle de los más importantes

### SEC-01 — Contraseña del administrador publicada

**Por qué es grave:** el repo es público, así que la contraseña ya está expuesta. Además queda en el historial de git aunque se borre el archivo, y cualquiera pudo haber hecho un *fork*. Hay que darla por **comprometida para siempre**.

Pasos:

1. **Ya:** cambia la contraseña de `admin@voll.med` en tu base local por una nueva, que nunca se escriba en el código.
2. Borra `GenerarHashTest.java`.
3. Crear el admin inicial **desde variables de entorno** (`ADMIN_EMAIL`, `ADMIN_PASSWORD`), con un `ApplicationRunner` que lo cree solo si no existe ningún ADMINISTRADOR.
4. Una migración nueva (V7) que borre al admin sembrado **solo si todavía tiene el hash conocido**. Así ninguna base nueva queda con esa contraseña, y la tuya no se toca si ya la cambiaste.
5. No hace falta reescribir el historial de git: lo importante es que esa contraseña ya no sirva para nada.

### SEC-02 y SEC-03 — Manejo del token

Hoy un token vencido rompe la sesión con un 403 y un usuario suspendido sigue entrando. La corrección va en dos lugares:

```java
// SecurityConfigurations: responder 401 (no 403) cuando falta o falla la autenticación
.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
```

```java
// SecurityFilter: un token inválido no debe romper la petición, y solo se autentica a usuarios habilitados
if (tokenJWT != null) {
    try {
        var usuario = usuarioRepository.findByCorreo(tokenService.getSubject(tokenJWT));
        if (usuario != null && usuario.isEnabled()) {
            var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    } catch (TokenInvalidoException e) {
        // se sigue sin autenticar: Spring responde 401 y el front cierra la sesión
    }
}
```

`TokenService` debería lanzar una excepción propia (`TokenInvalidoException`) en vez de una `RuntimeException` genérica. Además, `AutenticacionService` debe lanzar `UsernameNotFoundException` cuando no encuentra el correo, en lugar de devolver `null`, que rompe el contrato de Spring Security.

### SEC-07 — Zona horaria

```java
// TokenService: la expiración no debe depender de la zona del servidor
private Instant fechaExpiracion() {
    return Instant.now().plus(Duration.ofHours(2));
}
```

```java
// Un reloj único para las reglas de negocio, con la zona de la clínica
@Bean
Clock clock() {
    return Clock.system(ZoneId.of("America/Lima"));
}
// En los validadores: LocalDateTime.now(clock) en lugar de LocalDateTime.now()
```

Bonus: con un `Clock` inyectado, las pruebas unitarias pueden **fijar la hora** (`Clock.fixed(...)`) y probar casos como "faltan 29 minutos" sin depender de cuándo se ejecuten.

## Plan de corrección propuesto

Una rama y un PR por fase. Cada commit y cada PR, con tu autorización.

| Fase | Rama | Incluye |
|---|---|---|
| 1 | `fix/seguridad-critica` | SEC-01, SEC-02, SEC-03, SEC-06 y BUG-08 |
| 2 | `fix/errores-y-mensajes` | BUG-02, BUG-03, SEC-05 y BUG-01 |
| 3 | `refactor/reloj-y-pruebas` | SEC-07, CAL-01 y las primeras pruebas unitarias (validadores y `ReservaDeConsultas`) |
| 4 | Por decidir | SEC-04 (límite de intentos), BUG-04 (recepcionista), BUG-05 (estados), BUG-06 (concurrencia), BUG-11 y CAL-02 a CAL-06 |

## Seguimiento

### Fase 1 — `fix/seguridad-critica`

- [x] SEC-01: el admin inicial se crea con `ADMIN_EMAIL` y `ADMIN_PASSWORD`, `V7` borra al admin publicado y se eliminó `GenerarHashTest`. Ver el [ADR 0002](../adr/0002-admin-inicial-por-variables-de-entorno.md).
- [x] SEC-02 y SEC-03: con un token vencido o alterado, o de un usuario suspendido, dado de baja o borrado, se responde 401, y el frontend cierra la sesión sin cambios. Antes de corregirlo, `SeguridadDelTokenTest` confirmó el análisis: sin token se respondía 403, un usuario suspendido entraba y un token vencido o un usuario borrado hacían fallar el filtro. Un login con credenciales incorrectas también pasa de 403 a 401; los mensajes claros llegan con BUG-03.
- [x] SEC-06: dependencias con vulnerabilidades.
  - Backend: Spring Boot 4.0.9 y java-jwt 4.6.2.
  - Backend, segunda ronda: al recargar Maven, IntelliJ marcó 7 librerías con CVE publicados entre julio y octubre de 2026. Ni Spring Boot 4.0.9 ni 4.1.2 traen las versiones corregidas, y OSV, que reúne los avisos de GitHub que usa Dependabot, todavía no los tiene. Se fijaron en `pom.xml` el driver de MySQL 26.10.0 (la 26.7.0 que sugería IntelliJ ya tiene fallos de octubre), Jackson 3.1.8 y 2.21.8 (el último parche de cada línea: NVD aún no dice en qué versión se corrigen varios fallos de octubre, y casi todos exigen funciones que la app no usa) y json-smart 2.6.1, que trae `asm` 9.11.1. Esas propiedades se quitan cuando Spring Boot traiga esas versiones o mayores.
  - Avisos aceptados: logback 1.5.38 (CVE-2026-19880 y CVE-2026-104721) solo afecta a quien escribe logs en archivos con rutas armadas desde el MDC, y esta app solo usa la consola; la corrección está en logback 1.6, que Spring Boot todavía no usa. `android-json` solo se usa en pruebas y no tiene versión corregida.
  - Frontend: `npm audit fix` dejó producción sin vulnerabilidades (antes eran 7, 6 de ellas altas).
  - Dependabot (`.github/dependabot.yml`) revisa Maven y npm cada semana. Empieza a funcionar cuando la rama se fusione en `main`.
  - Quedan 12 avisos solo de desarrollo, que no llegan al navegador de los usuarios. El de Vite 5 (alto, afecta al servidor de desarrollo) se corrige subiendo a Vite 8, un cambio mayor que conviene hacer aparte. Los de eslint (moderados) vienen de un `minimatch` afectado que ninguna versión de eslint 10 corrige todavía.
- [x] BUG-08: `frontend/.env.example` apunta a `http://localhost:8080`, como el `README` del frontend; con `/api` fallaba el login. Se quitó el proxy `/api` de `vite.config.ts`: nadie lo usaba y, como no quitaba el prefijo, tampoco habría funcionado.

CAL-04 no se corrigió junto con SEC-01, como se proponía arriba, porque editar `V5` obliga a reparar cada base que ya la aplicó. Hay que corregirlo antes de migrar otra base, como `vollmed_api_test`. Ver las consecuencias del [ADR 0002](../adr/0002-admin-inicial-por-variables-de-entorno.md).

### Después de la fase 1 — PR de Dependabot

Dependabot empezó a funcionar al fusionar la fase 1 y abrió seis PR, y después otros tres. Los que se fusionaron se probaron en local: en el backend, `./mvnw test` y la aplicación en marcha; en el frontend, `npm ci`, `npm run build` y la aplicación en marcha.

Desde el [PR #17](https://github.com/Jvm124/MediApi/pull/17), el CI (GitHub Actions, en `.github/workflows/ci.yml`) también prueba cada PR, incluidos los de Dependabot, y cada cambio que llega a `main`: en el backend, `./mvnw test`; en el frontend, `npm ci` y `npm run build`. El lint todavía no, porque fallaría por los 8 errores de CAL-05.

- [PR #8](https://github.com/Jvm124/MediApi/pull/8), backend: Spring Boot 4.1.2, springdoc 3.2.0 y Maven Wrapper 3.10.0. Spring Boot 4.1.2 trae Flyway 12, que validó las 7 migraciones sin cambiar la base. Las propiedades de SEC-06 en `pom.xml` siguen haciendo falta, porque Spring Boot 4.1.2 tampoco trae las versiones corregidas.
- [PR #13](https://github.com/Jvm124/MediApi/pull/13) y [PR #10](https://github.com/Jvm124/MediApi/pull/10), frontend: Vite 8 y `@vitejs/plugin-react` 6. Tienen que ir juntos, porque la versión 4 del plugin solo acepta hasta Vite 7. Con Vite 8 desaparece el aviso alto de Vite 5 que había quedado pendiente en SEC-06.
- [PR #9](https://github.com/Jvm124/MediApi/pull/9), frontend: React 19.3, react-hook-form 7.91, lucide-react 1.70 y herramientas de desarrollo. `@types/react` y `@types/react-dom` pasan de la 18 a la 19, lo que resuelve esa parte de CAL-05.
- [PR #15](https://github.com/Jvm124/MediApi/pull/15), frontend: `@hookform/resolvers` 7. Antes de fusionarlo se comprobó en una copia que los seis esquemas de `src/schemas` dan los mismos errores y mensajes con la 5.2.2 y con la 7.0.3.
- [PR #12](https://github.com/Jvm124/MediApi/pull/12), Tailwind 4: se fusionó y se revirtió con el [PR #14](https://github.com/Jvm124/MediApi/pull/14). Tailwind 4 mueve su plugin de PostCSS a `@tailwindcss/postcss`, y sin una migración el build falla. La migración (`npx @tailwindcss/upgrade` y revisar las pantallas) queda como tarea aparte. Dependabot volvió a proponerlo en el [PR #19](https://github.com/Jvm124/MediApi/pull/19), que el CI marcó en rojo y no se fusiona. Ahora `.github/dependabot.yml` ignora las versiones mayores de Tailwind; esa regla se quita al hacer la migración.
- [PR #11](https://github.com/Jvm124/MediApi/pull/11), `@types/node` 26, y [PR #18](https://github.com/Jvm124/MediApi/pull/18), `@types/node` 25: no se fusionan, porque el proyecto usa Node.js 24. Comentar `@dependabot ignore this major version` en el #11 solo ignoró la 26, así que ahora `.github/dependabot.yml` ignora todas las versiones mayores de `@types/node`. Las menores y los parches de la 24 siguen llegando.
- TypeScript 7: `typescript-eslint`, incluso en su última versión (8.72.2), solo admite TypeScript hasta la 6.0. Es la única actualización pendiente que choca con otra dependencia, y la revisión de npm de Dependabot fallaba con «Error while updating peer dependency». `.github/dependabot.yml` ignora las versiones mayores de TypeScript; pasar a la 6 o a la 7 queda como tarea aparte.

Quedan 10 avisos moderados de `npm audit`, todos de desarrollo; en producción no hay ninguno. Vienen del mismo `minimatch` afectado, que llega a través de eslint 10 y `typescript-eslint`. `npm audit fix --force` no es una solución: bajaría eslint a la 9.

### Fase 2 — `fix/errores-y-mensajes`

- [x] BUG-02: el backend responde los errores con el formato `ProblemDetail` (RFC 9457), con el mensaje para el usuario en `detail`, y `getErrorMessage` lee ese campo. Ahora se ve, por ejemplo, «Ya existe un paciente con ese email» en vez del mensaje genérico. Los errores de validación traen además la lista `errores`, con cada campo y su mensaje, y el 404 ya no llega vacío. El login, el registro y la reserva leían `data.message` por su cuenta; ahora usan `getErrorMessage`, y con eso desaparecen dos errores del lint (`no-explicit-any`). `TratadorDeErroresTest` comprueba los tres casos.
- [x] BUG-03: el login fallido responde 401 con un mensaje claro: «Correo o contraseña incorrectos.» (también si el correo no existe, para no revelar qué correos tienen cuenta) o «Tu cuenta está suspendida o dada de baja.». Si falla el servidor al buscar al usuario (por ejemplo, la base de datos), responde 500 en vez de un 401 que parecía una contraseña incorrecta. Un dato repetido que llega a la base de datos (por ejemplo, dos registros a la vez con el mismo correo) responde 409, `@PreAuthorize` responde 403 con mensaje y cualquier otro error responde 500 con un mensaje seguro y queda en el log; antes, estos tres casos no tenían manejador. `TratadorDeErrores` hereda de `ResponseEntityExceptionHandler` para que los errores propios de Spring MVC (JSON mal formado, método no permitido, ruta inexistente) conserven su código en vez de caer en el 500 genérico. Spring revisa el estado de la cuenta antes que la contraseña, así que el aviso de cuenta suspendida sale aunque la contraseña sea incorrecta; como el registro ya revela si un correo tiene cuenta, se dejó así. `AutenticacionControllerTest` prueba el login con la seguridad real, y las pruebas nuevas fallan con el código anterior.
- [x] SEC-05: las contraseñas nuevas siguen una sola regla en el backend y en el frontend: entre 8 y 72 caracteres, con al menos una mayúscula, una minúscula y un número. En el backend es la anotación `@ContraseniaSegura`, que reemplaza a `@NotBlank` en `DatosRegistroUsuario`, `DatosRegistroPaciente` y `DatosRegistroMedico`; antes, llamando a la API se aceptaba la contraseña «1». El máximo se mide en bytes porque BCrypt no admite más de 72 (una letra con tilde o una ñ ocupa 2): con más, el registro terminaba en un 500, y ahora responde 400 con el motivo. En el frontend, `contraseniaSchema.ts` aplica la misma regla en el registro de pacientes y en las altas de médicos y personal, que antes solo pedían 6 caracteres. El registro de pacientes ya no exige un carácter especial, para que la regla sea la misma en todas partes, y el login acepta hasta 72 bytes en vez de 64 caracteres, para que cualquier contraseña válida pueda entrar. La contraseña del admin inicial (`ADMIN_PASSWORD`) mantiene su regla de SEC-01. `ContraseniaSeguraTest` prueba la regla y que los tres registros la usan; esto último falla con los DTOs anteriores.
- [x] BUG-01: una consulta cancelada ya no bloquea la hora del médico. Las dos consultas que revisan si el médico está libre (`ConsultaRepository.existsByIdMedicoIdAndFechaAndEstado`, que usa `ValidadorMedicoConOtraConsultaEnElMismoHorario`, y `MedicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha`) ahora solo cuentan las consultas `PROGRAMADA`. Antes, al volver a reservar una hora cancelada salía «Medico ya tiene otra consulta en esa misma fecha y hora» si se elegía al médico, o «No existe un médico disponible en ese horario» si se pedía solo la especialidad. `ReservaDeConsultasConMySQLTest` prueba la reserva contra un MySQL real con Testcontainers (`spring-boot-testcontainers` y `testcontainers-mysql`, solo para pruebas): arranca un MySQL 8.4 desechable en Docker, Flyway aplica las migraciones y al terminar se borra, sin tocar la base local. La base del contenedor se llama `vollmed_api` para que V5 funcione; CAL-04 sigue pendiente. Sin Docker, estas pruebas se saltan en local; en el CI se ejecutan siempre. Las dos pruebas de BUG-01 fallan con las consultas anteriores. Se borró `MedicoRepositoryTest`, que estaba comentado entero: sus dos casos los cubre la prueba nueva.

### Fase 3 — `refactor/reloj-y-pruebas`

- [x] CAL-01: las 16 clases que recibían sus dependencias con `@Autowired` en los atributos (los controladores, los servicios, los validadores de la reserva, `SecurityFilter` y `SecurityConfigurations`) ahora las reciben por el constructor. Los atributos son `final` y Lombok genera el constructor con `@RequiredArgsConstructor`. Lo que hace la aplicación no cambia. Las dependencias ya no pueden cambiar después de crear el objeto, se ven todas en un solo lugar y las pruebas unitarias pueden crear estas clases con dependencias simuladas, sin arrancar Spring. `AplicacionCompletaConMySQLTest` arranca la aplicación completa contra un MySQL desechable con Testcontainers, y falla si a alguna clase le falta una dependencia.
- [x] SEC-07: la aplicación tiene un solo reloj, con la zona horaria de la clínica (`ConfiguracionDelReloj`, `America/Lima`). Antes, los validadores preguntaban la hora con `LocalDateTime.now()`, que usa la zona del servidor. En un servidor con hora UTC, como el CI de GitHub o muchos de la nube, «ahora» iba 5 horas adelantado: no se podía reservar con menos de 5 horas y media de anticipación (en vez de 30 minutos), y para cancelar hacían falta 29 horas en vez de 24. Ahora `ValidadorConsultaConAnticipacion` y `ValidadorHorarioAntecedencia` reciben ese reloj y preguntan la hora con `LocalDateTime.now(reloj)`. `@Future` de `DatosReservaConsulta` tenía el mismo problema, aunque la revisión no lo mencionaba: ahora Bean Validation usa el mismo reloj, con un `ValidationConfigurationCustomizer`. El token vence con `Instant.now()` más 2 horas, igual en cualquier zona; antes, en un servidor con hora UTC, duraba 7 horas. Las pruebas de los dos validadores fijan la hora con `Clock.fixed(...)`, y `ReservaDeConsultasConMySQLTest` también, así que ya no dejará de pasar en 2030. La prueba nueva de `SeguridadDelTokenTest` pone el servidor en UTC y falla con el código anterior.
- [x] TEST-01, primera parte: pruebas unitarias de los cinco validadores de la reserva que no tenían (los otros dos se probaron con SEC-07) y de `ReservaDeConsultas`, sin Spring ni base de datos. Mockito simula los repositorios y los validadores (`@Mock`, `when(...)`, `verify(...)`), y gracias a CAL-01 cada prueba crea la clase con esas piezas simuladas. `ReservaDeConsultasTest` (14 pruebas) cubre a nombre de quién se reserva (un paciente siempre a su nombre, aunque envíe otro `idPaciente`), la elección del médico, que no se guarde nada si un validador rechaza la reserva, y quién puede cancelar o atender una consulta. Los validadores suman 15 pruebas. Antes del commit se rompieron a propósito cuatro reglas en una copia del código: un paciente que cancela la consulta de otro, un paciente que reserva con otro `idPaciente`, contar las consultas canceladas al elegir médico y atender a las 18:00. En los cuatro casos falló alguna prueba. Falta probar las transiciones de estado de `Usuario`. El filtro de seguridad ya lo prueba `SeguridadDelTokenTest`.
