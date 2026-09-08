# Complaint Management System - Frontend

React frontend for the Flowable-based Complaint Management System.

## Features

- **Customer Complaint Form**: Submit new complaints with automatic ticket generation
- **Role-based Task Management**: Each role has its own dedicated page
  - Branch Staff: First contact resolution or escalation
  - CMD Officer: Complaint screening and categorization
  - Audit Team: Investigation and approval/rejection
  - Work Unit: Final resolution implementation
  - CMD: Screening, assignment, and customer notification

## Workflow

1. Customer submits complaint → Ticket generated
2. Branch Staff handles → Resolves OR sends to CMD
3. CMD screens → Sets category/priority → Sends to Audit OR Work Unit
4. Audit investigates → Approves (to Work Unit) OR Rejects (close)
5. Work Unit resolves → Marks sensitivity
6. CMD notifies customer → Case closed

## Getting Started

### Prerequisites

- Node.js 14+
- Backend API available at `http://localhost:8080` (Docker or `mvnw spring-boot:run`)

### Installation

From this project folder (not inside the backend repo):

```bash
npm install
```

### Running the Application

```bash
npm start
```

The app will open at `http://localhost:3000`

## Docker Deployment

Frontend-only stack (Nginx + compiled React). The backend Compose stack must already be running on port 8080.

```powershell
cd "Complaint Management System Frontend"
docker compose up -d --build
```

UI: http://localhost

Nginx proxies `/api` using `CMS_BACKEND` in this folder's `.env` (default `host.docker.internal:8080`).

### Building for Production

```bash
npm run build
```

## API Integration

The frontend communicates with the Flowable backend via REST API:

- `POST /api/complaints/start` - Submit new complaint
- `GET /api/tasks/enriched?candidateGroup={group}` - Get tasks for role
- `POST /api/tasks/{id}/claim` - Claim a task
- `POST /api/tasks/{id}/complete` - Complete task with variables

## Role Pages

### Customer Form (`/`)
- Complaint submission form
- Automatic email/SMS notification on submission

### Branch Staff (`/branch-staff`)
- View pending complaints
- First Contact Resolution (FCR) option
- Escalate to CMD if not resolved

### CMD Officer (`/cmd`)
- Complaint screening
- Category assignment (HS/S/G)
- Priority setting (P1/P2/P3)
- Investigation requirement decision

### Audit Team (`/audit`)
- Investigation findings
- Approval/rejection decision
- Comments and recommendations

### Work Unit (`/work-unit`)
- Resolution details
- Action taken documentation
- Sensitivity marking

## Development Notes

- Simple, functional UI (no complex styling)
- Role-based navigation
- Real-time task updates
- Form validation
- Error handling and user feedback
