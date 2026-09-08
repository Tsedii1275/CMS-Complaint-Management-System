# CMS — Complaint Management System

Dashen Bank complaint workflow: Spring Boot + Flowable backend and React frontend.

Frontend and backend Docker stacks are independent. There is no root Compose file.

## Layout

- `complaint-management-system` — backend (Spring Boot, MySQL, optional phpMyAdmin)
- `Complaint Management System Frontend` — frontend (Nginx serving the production React build)

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

Each folder has its own `.env`. The frontend proxies `/api` to the backend using `CMS_BACKEND` (default `host.docker.internal:8080`).

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
