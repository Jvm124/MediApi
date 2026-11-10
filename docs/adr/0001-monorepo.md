# 0001 — Monorepo para backend y frontend

- **Fecha:** 2026-11-09
- **Estado:** aceptada

## Contexto

MediApi estaba repartido en dos repositorios de GitHub:

- `Jvm124/MediApi`: backend (Spring Boot)
- `Jvm124/Healthcare`: frontend (React)

Lo desarrolla una sola persona y casi todas las funcionalidades tocan las dos partes. Por ejemplo, los estados de usuario necesitaron un PR en cada repositorio (Jvm124/MediApi#5 y Jvm124/Healthcare#1). Con dos repos, la documentación, la configuración y el futuro CI iban a quedar duplicados.

## Decisión

Usar un solo repositorio, `Jvm124/MediApi`, con esta estructura:

```
MediApi/
├── backend/    ← Spring Boot (se abre con IntelliJ IDEA)
├── frontend/   ← React + Vite (se abre con WebStorm)
├── docs/       ← decisiones, problemas resueltos e ideas
└── README.md
```

- Se conserva el historial completo de los dos proyectos.
- Se mantiene el nombre `MediApi`. GitHub permite renombrar repositorios y redirige los enlaces viejos, pero este nombre ya representa el proyecto completo.
- Cada IDE abre su propia carpeta y guarda su configuración (`.idea/`) dentro de ella. Esa configuración no se versiona.

## Alternativas consideradas

| Alternativa | A favor | En contra |
|---|---|---|
| Dos repositorios (polyrepo) | Despliegues y permisos independientes. Útil cuando back y front los llevan equipos distintos. | Hay que coordinar PRs entre repos, la documentación y el CI se duplican y cuesta ver el proyecto completo. |
| Monorepo con herramientas (Nx, Turborepo) | Cachés y builds incrementales cuando hay muchos paquetes. | Demasiada complejidad para dos aplicaciones en lenguajes distintos. |
| **Monorepo simple con carpetas** (elegida) | Un solo PR por funcionalidad completa, una documentación y un CI. Más fácil de mostrar en un portafolio. | El CI tiene que filtrar por carpeta para no compilar todo en cada cambio. |

## Consecuencias

- Un cambio que toca la API y la pantalla va en un solo commit o PR.
- El CI (GitHub Actions) tiene que usar filtros `paths: backend/**` y `paths: frontend/**`.
- `Jvm124/Healthcare` queda como histórico. Conviene archivarlo en GitHub con un enlace a MediApi.
- Si más adelante hay equipos separados, se puede volver a separar con `git subtree split`.

## Cómo se hizo

Todo se hizo en la rama `chore/monorepo`, sin reescribir el historial que ya estaba en GitHub:

```bash
# 1. Backend: mover todo a backend/ (git registra los movimientos como renombres)
git switch -c chore/monorepo
mkdir backend
git mv .gitattributes .gitignore .mvn mvnw mvnw.cmd pom.xml src backend/
git commit -m "refactor: mover el backend a la carpeta backend/"

# 2. Frontend: en un clon temporal de Healthcare, mover todo a frontend/
git clone <ruta-del-frontend> frontend-import
cd frontend-import
mkdir frontend
git mv $(git ls-tree --name-only HEAD) frontend/
git commit -m "refactor: mover el frontend a la carpeta frontend/"

# 3. Unir los dos historiales en el monorepo
cd <ruta-de-MediApi>
git fetch <ruta>/frontend-import main
git merge --allow-unrelated-histories FETCH_HEAD -m "chore: unificar el frontend (Healthcare) en el monorepo MediApi"
```

Para ver el historial de un archivo desde antes del movimiento: `git log --follow -- frontend/package.json`.

En IntelliJ y en WebStorm se cambió `.idea/vcs.xml` para que apunte a la raíz del repositorio (`$PROJECT_DIR$/..`), porque ahora el `.git` está una carpeta más arriba.
