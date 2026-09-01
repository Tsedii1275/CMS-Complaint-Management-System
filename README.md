# CMS — Complaint Management System

Dashen Bank complaint workflow: Spring Boot + Flowable backend and React frontend.

## Layout

- `complaint-management-system` — Spring Boot (`Dockerfile` in this folder)
- `Complaint Management System Frontend` — React (`Dockerfile` and `nginx.conf` in this folder)
- `docker-compose.yml` — the only Compose file (Nginx + Spring Boot + MySQL + phpMyAdmin)

## Run the full stack (Docker)

```powershell
cd D:\Complaint-projects\demo\CMS
docker compose up -d --build
```

- Application: http://localhost
- phpMyAdmin: http://localhost:8081

Spring Boot port 8080 stays on the Compose network only.

## Run on the host (developer mode)

MySQL + phpMyAdmin from the same Compose file:

```powershell
cd D:\Complaint-projects\demo\CMS
docker compose up -d mysql phpmyadmin
```

Backend:

```powershell
cd complaint-management-system
copy src\main\resources\application.properties.example src\main\resources\application.properties
# edit local secrets, then:
.\mvnw.cmd spring-boot:run
```

Frontend (quotes required because of spaces):

```powershell
cd "Complaint Management System Frontend"
npm install
npm start
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
