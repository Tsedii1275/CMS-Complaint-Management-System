# CMS — Complaint Management System

Dashen Bank complaint workflow: Spring Boot + Flowable backend and React frontend.

Production uses two servers. A developer machine can still run every service from the root Compose file.

## Layout

- `complaint-management-system` — Spring Boot Dockerfile, and the MySQL Compose file for the database server
- `Complaint Management System Frontend` — React Dockerfile, Nginx config, and the Compose file that runs Nginx and Spring Boot together
- `docker-compose.yml` — local all-in-one stack (MySQL, API, Nginx, phpMyAdmin)

## Configuration

Each folder has `.env` (switch), `.env.dev` (localhost), and `.env.prod` (UAT). Set `APP_ENV=dev` or `APP_ENV=prod` in `.env`. Compose, Spring, and the frontend scripts load `.env.${APP_ENV}`.

- Backend: `APP_PUBLIC_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, DB, mail, JWT.
- Frontend: `CMS_BACKEND` is the Nginx `/api` upstream. `REACT_APP_API_BASE_URL` is for `npm start`; the Docker image uses same-origin `/api`.

Edit `.env` on the target machine, then:

```powershell
docker compose up -d --build
```

## Docker (production)

Database server `dbhqcvmsprodbesrv` (`172.24.17.113`) runs MySQL only. Start it first. `APP_ENV=prod` in `.env`, passwords in `.env.prod.local`.

```powershell
cd complaint-management-system
docker compose up -d
```

App server `dbhqcvmsproffesrv` runs the React Nginx container and the Spring Boot container. Nginx forwards `/api/` to `complaint-management-system:8080` on the `cms` network. Spring Boot connects to `172.24.17.113:3306`.

```powershell
cd "Complaint Management System Frontend"
docker compose up -d --build
```

- UI: port 8084 (HTTP) and 443 (HTTPS)
- API on the app server, direct: port 8082
- phpMyAdmin, database server only: `docker compose --profile tools up -d`, then port 8081

Do not run `docker compose down -v`. That deletes the MySQL data volume and uploaded files.

## Docker (developer machine)

From this directory the root Compose file starts MySQL, the API, and Nginx together:

```powershell
docker compose up -d --build
```

- UI: http://localhost
- API: http://localhost:8080
- phpMyAdmin: http://localhost:8081

## Run on the host (developer mode)

MySQL from the backend Compose file:

```powershell
cd complaint-management-system
docker compose up -d mysql
```

Backend:

```powershell
cd complaint-management-system
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd "Complaint Management System Frontend"
npm install
npm start
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
