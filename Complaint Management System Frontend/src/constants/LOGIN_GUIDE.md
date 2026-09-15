# Login Credentials and Redirects

## Staff Login URL
**http://localhost:3000/staff-login**

## Login Credentials and Dashboard Routes

| Role | Username | Password | Dashboard URL | Route |
|------|----------|----------|--------------|-------|
| **Branch Staff** | `branchstaff` | `branch123` | http://localhost:3000/branch-staff | `/branch-staff` |
| **CMD Officer** | `cmd` | `cmd123` | http://localhost:3000/cmd | `/cmd` |
| **Audit Team** | `audit` | `audit123` | http://localhost:3000/audit | `/audit` |
| **Work Unit** | `workunit` | `work123` | http://localhost:3000/work-unit | `/work-unit` |

## Authentication Flow

1. **Login**: User visits `/staff-login` and enters credentials
2. **Validation**: System validates username and password
3. **Storage**: User info stored in localStorage
4. **Redirect**: User redirected to their role-specific dashboard
5. **Logout**: User can logout and returns to `/staff-login`

## Dashboard Features

Each dashboard includes:
- Professional header with Dashen Bank branding
- Role-specific task management
- Logout functionality
- Responsive design

## Testing Instructions

1. Open browser and navigate to `http://localhost:3000/staff-login`
2. Use any of the credentials above to test different roles
3. Verify redirect to correct dashboard
4. Test logout functionality
5. Check browser console for debug messages
