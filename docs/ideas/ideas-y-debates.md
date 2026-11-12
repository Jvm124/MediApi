# Ideas y debates

Aquí queda lo que conversamos y evaluamos. Cuando una idea se vuelve decisión, pasa a [`adr/`](../adr/). Cuando un hallazgo se corrige, se marca como hecho.

## Debates

### 2026-11-09 — ¿Uno o dos repositorios?

- **Pregunta:** ¿conviene tener backend y frontend en el mismo repositorio, como hacen muchas empresas y startups?
- **Conclusión:** sí, cuando un equipo pequeño hace funcionalidades completas (API + pantalla). Separarlos tiene sentido cuando hay equipos distintos con despliegues independientes, algo común en bancos y corporativos grandes.
- **Resultado:** [ADR 0001](../adr/0001-monorepo.md).

### 2026-11-09 — ¿Cambiar el nombre del repositorio en GitHub?

- Se puede hacer en *Settings → Repository name*, y GitHub redirige los enlaces viejos.
- **Conclusión:** no hace falta, porque `MediApi` ya sirve como nombre del proyecto completo.

### 2026-11-09 — ¿Qué stack se usa hoy en Perú?

- Backend: Java + Spring Boot es muy común en banca, seguros y corporativos. Node.js (NestJS/Express) aparece mucho en startups.
- Frontend: React + TypeScript es lo más pedido. Angular sigue presente en corporativos y Next.js va creciendo.
- Prácticas: Git con PRs y revisión de código, Docker, CI/CD con GitHub Actions, nube (sobre todo AWS) y pruebas automatizadas.
- **Conclusión:** el stack de MediApi (Spring Boot + React + TypeScript) va alineado. Falta lo de alrededor: pruebas, Docker, CI y seguridad.
- **Pendiente:** validarlo con ofertas reales (LinkedIn, Computrabajo, Get on Board).

### Por debatir — ¿Token en `sessionStorage` o en cookie `HttpOnly`?

- **Hoy:** el JWT se guarda en `sessionStorage` y se envía en la cabecera `Authorization`. Es simple y se borra al cerrar la pestaña, pero si hubiera un XSS, un script podría leerlo.
- **Alternativa:** que el backend lo envíe en una cookie `HttpOnly` + `Secure` + `SameSite`. JavaScript no puede leerla, pero obliga a manejar CSRF y a configurar CORS con credenciales.
- **A decidir** antes de desplegar. Relacionado: CAL-06 en la [revisión inicial](../revisiones/0001-revision-inicial.md).

### Por debatir — ¿Un captcha de verdad en el login?

- **Hoy:** el login limita los intentos fallidos por correo y por IP ([ADR 0005](../adr/0005-limite-de-intentos-de-login.md)). El reCAPTCHA que estaba comentado en el frontend se quitó, porque no protegía nada: se enviaba vacío y el backend no lo verificaba.
- **Idea:** pedir un captcha (reCAPTCHA, hCaptcha o Cloudflare Turnstile, con planes gratuitos), por ejemplo solo después de 2 o 3 fallos, para frenar a los programas antes de que llegue el bloqueo.
- **Requisito:** el backend tiene que verificar cada token con el servicio del captcha, usando una clave secreta. Sin esa verificación no protege nada (SEC-04 en la [revisión inicial](../revisiones/0001-revision-inicial.md)).
- **A decidir** antes de desplegar.

## Hallazgos

Los hallazgos que se vieron al armar el monorepo se confirmaron y ampliaron en la [revisión inicial](../revisiones/0001-revision-inicial.md) (ver SEC-06, BUG-08, BUG-09, CAL-02 y CAL-05). El seguimiento se hace ahí.

- [ ] npm 12 bloquea por defecto el `postinstall` de `esbuild` (`allowScripts`). El build funciona igual; hay que decidir si se aprueba con `npm install-scripts approve esbuild`.

## Pendientes

- [x] Subir la rama `chore/monorepo`, abrir el PR hacia `main` y fusionarlo (PR #6, con merge commit).
- [ ] Archivar `Jvm124/Healthcare` en GitHub con un enlace a MediApi.
- [x] Revisar errores, mejoras y seguridad del backend y del frontend: [revisión inicial](../revisiones/0001-revision-inicial.md).
- [ ] Corregir los hallazgos por fases (plan en la revisión).
- [ ] Pruebas unitarias: JUnit 5 + Mockito en el backend y Vitest + Testing Library en el frontend.
- [ ] `docker-compose.yml` en la raíz (MySQL + backend + frontend).
- [ ] CI con GitHub Actions filtrado por carpeta.
- [ ] Instalar GitHub CLI (`gh`) para trabajar los PRs desde la terminal.
