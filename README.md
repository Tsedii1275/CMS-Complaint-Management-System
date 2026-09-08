# CMS — Complaint Management System

Dashen Bank complaint workflow: Spring Boot + Flowable backend and React frontend.

Frontend and backend Docker stacks are independent. There is no root Compose file.

## Layout

- `complaint-management-system` — backend (Spring Boot, MySQL, optional phpMyAdmin)
- `Complaint Management System Frontend` — frontend (Nginx serving the production React build)

## Configuration

Each folder has `.env` (switch), `.env.dev` (localhost), and `.env.prod` (UAT). Set `APP_ENV=dev` or `APP_ENV=prod` in `.env`. Compose, Spring, and the frontend scripts load `.env.${APP_ENV}`.

- Backend: `APP_PUBLIC_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, DB, mail, JWT.
- Frontend: `CMS_BACKEND` is the Nginx `/api` upstream. `REACT_APP_API_BASE_URL` is for `npm start`; the Docker image uses same-origin `/api`.

Edit `.env` on the target machine, then:

```powershell
docker compose up -d --build
```

## Docker (decoupled)

Start backend first, then frontend.

```powershell
cd complaint-management-system
docker compose up -d --build
```

```powershell
cd "Complaint Management System Frontend"
docker compose up -d --build
```

- UI: http://localhost
- API: http://localhost:8080
- phpMyAdmin: `docker compose --profile tools up -d` from the backend folder, then http://localhost:8081

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
