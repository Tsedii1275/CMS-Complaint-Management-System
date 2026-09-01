# Complaint Management System

A comprehensive, enterprise-grade Complaint Management System built to handle customer grievances efficiently through a structured, multi-departmental workflow. 

This system integrates a modern React frontend with a robust Spring Boot backend powered by the Flowable BPMN engine to ensure complaints are routed, tracked, and resolved within strictly monitored Service Level Agreements (SLAs).

## 🌟 Key Features

*   **BPMN Workflow Engine**: Built on top of Flowable to manage the complete lifecycle of a complaint from submission to resolution.
*   **Dynamic SLA Tracking**: Automatically calculates and tracks deadlines based on the complaint category (e.g., Fraud, ATM, Loan).
*   **Departmental Time Tracking**: Granular tracking of exactly how much time a complaint spends in each lane (Branch Staff, CMD, Audit, Work Unit, Service Quality).
*   **Role-Based Dashboards**: Tailored interfaces and permissions for different user roles:
    *   System Admin
    *   Branch Staff
    *   CMD Officer
    *   Audit Team
    *   Work Unit
    *   Service Quality
*   **Audit Logging**: Comprehensive, immutable audit trails recording every action taken on a complaint, with CSV export capabilities.
*   **Premium UI/UX**: A highly responsive, modern interface built with React and Ant Design, featuring glassmorphism elements, dynamic statistics, and smooth transitions.

## 🛠️ Technology Stack

### Backend
*   **Java 17+**
*   **Spring Boot**: REST APIs, Security, Data JPA
*   **Flowable**: Open-source BPMN 2.0 workflow engine
*   **Database**: H2 (In-memory for dev) / Mysql (Production ready)
*   **Security**: Spring Security with JWT Authentication

### Frontend
*   **React.js**
*   **Ant Design (antd)**: Component library for complex tables, statistics, and forms.
*   **Axios**: For API communication
*   **Moment.js**: For date and time formatting

## 🚀 Getting Started

### Prerequisites
*   Java Development Kit (JDK) 17 or higher
*   Node.js (v16+) and npm
*   Maven

### Backend Setup

1. Navigate to the root directory.
2. Run the Spring Boot application using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The backend runs on `http://localhost:8080` by default. Flowable database tables are automatically initialized on startup.*

### Frontend Setup

The React app is a separate project:

`D:\Complaint-projects\demo\CMS\Complaint Management System Frontend`

1. Navigate to that folder:
   ```bash
   cd "D:\Complaint-projects\demo\CMS\Complaint Management System Frontend"
   ```
2. Install the dependencies (first time):
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm start
   ```
   *The frontend runs on `http://localhost:3000` and talks to the backend at `http://localhost:8080`.*

## 🐳 Docker Deployment

The project includes a `docker-compose.yml` for easy containerized deployment. 

```bash
# Build and start the services
docker-compose up --build
```

## 🧑‍💻 Usage & Workflow

1.  **Authentication**: Users log in. The system routes them to their specific dashboard based on their JWT token role.
2.  **Complaint Creation**: Branch staff creates a new complaint. The Flowable engine starts a new process instance.
3.  **SLA Initialization**: The system reads the category and assigns a strict deadline.
4.  **Task Routing**: The complaint moves sequentially through the BPMN lanes (Branch -> CMD -> Work Unit -> Audit).
5.  **Monitoring**: The Admin dashboard provides a bird's-eye view of all complaints, highlighting ones that are `ON_TIME`, `APPROACHING` deadline, or `OVERDUE`.

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the issues page.

## 📝 License
This project is proprietary and confidential. Unauthorized copying of files, via any medium, is strictly prohibited.
