# CMS — Complaint Management System

Dashen Bank complaint workflow: Spring Boot + Flowable backend and React frontend.

## Layout

- `complaint-management-system` — Spring Boot backend (`pom.xml`, `src/`, Docker MySQL)
- `Complaint Management System Frontend` — React (Create React App)

## Run

MySQL:

```powershell
cd complaint-management-system
docker compose up -d
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
