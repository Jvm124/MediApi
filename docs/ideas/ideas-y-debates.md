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

## Hallazgos para la revisión (por confirmar)

Cosas que se vieron de paso al armar el monorepo. Se revisan con calma en la revisión de errores y mejoras.

- [ ] `frontend/.env.example` trae `VITE_API_BASE_URL=http://localhost:8080/api`, pero el backend no usa el prefijo `/api`, así que con ese valor fallan todas las llamadas. El proxy `/api` de `vite.config.ts` tampoco se está usando.
- [ ] `frontend/README.md` dice React 18, pero `package.json` usa React 19. Además, `@types/react` y `@types/react-dom` siguen en la versión 18.
- [ ] En `CorsConfig.java` quedó un comentario sobrante (`// <- renombrada (ver nota abajo)`) y el origen permitido (`http://localhost:5173`) está fijo en el código. Conviene moverlo a la configuración.
- [ ] Sigue el endpoint de prueba `/hello` del curso.
- [ ] `npm run lint` marca 8 errores y 1 advertencia, que ya estaban antes de la migración. El build sí pasa.
  - `setState` llamado directamente dentro de un `useEffect` (provoca renders en cascada): `AdminMedicosPage`, `MedicoDashboardPage` y `MedicoPerfilPage`.
  - Tipos `any` en `LoginPage` y `RegisterPage`.
  - Variables sin usar (`confirmPassword`, `acceptTerms`) en `RegisterPage`.
  - `AuthContext.tsx` exporta el contexto y el componente en el mismo archivo, lo que rompe el *fast refresh*.
  - Advertencia en `ReservarCitaPage`: `watch()` de React Hook Form no es compatible con la memoización del React Compiler.
- [ ] npm 12 bloquea por defecto el script `postinstall` de `esbuild` (`allowScripts`). El build funciona igual, pero hay que decidir si se aprueba con `npm install-scripts approve esbuild`.

## Pendientes

- [ ] Subir la rama `chore/monorepo`, abrir el PR hacia `main` y fusionarlo.
- [ ] Archivar `Jvm124/Healthcare` en GitHub con un enlace a MediApi.
- [ ] Revisar errores y mejoras del backend y del frontend.
- [ ] Revisar la seguridad: JWT, CORS, validaciones y dependencias.
- [ ] Pruebas unitarias: JUnit 5 + Mockito en el backend y Vitest + Testing Library en el frontend.
- [ ] `docker-compose.yml` en la raíz (MySQL + backend + frontend).
- [ ] CI con GitHub Actions filtrado por carpeta.
- [ ] Instalar GitHub CLI (`gh`) para trabajar los PRs desde la terminal.
