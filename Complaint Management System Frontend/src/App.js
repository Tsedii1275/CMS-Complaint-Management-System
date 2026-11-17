import React, { Suspense, lazy } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import CustomerForm from './pages/CustomerForm';
import Login from './pages/Login';
import CustomerFeedbackPage from './pages/CustomerFeedbackPage';
import { RequireAuth, RequireRole } from './components/RoleGuard';
import { AuthProvider } from './contexts/AuthContext';
import SessionIdleWatch from './components/SessionIdleWatch';
import './index.css';

const BranchStaffDashboard = lazy(() => import('./pages/BranchStaffDashboard'));
const CMDDashboard = lazy(() => import('./pages/CMDDashboard'));
const AuditDashboard = lazy(() => import('./pages/AuditDashboard'));
const WorkUnitDashboard = lazy(() => import('./pages/WorkUnitDashboard'));
const ChiefCommitteeDashboard = lazy(() => import('./pages/ChiefCommitteeDashboard'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const NBEReports = lazy(() => import('./pages/NBEReports'));
const RcaDashboard = lazy(() => import('./pages/RcaDashboard'));
const CustomerExperienceDashboard = lazy(() => import('./pages/CustomerExperienceDashboard'));
const SlaConfigPage = lazy(() => import('./pages/SlaConfigPage'));
const ComplaintCategoriesPage = lazy(() => import('./pages/ComplaintCategoriesPage'));
const SlaMonitoringPage = lazy(() => import('./pages/SlaMonitoringPage'));
const ExecutiveDashboard = lazy(() => import('./pages/ExecutiveDashboard'));
const EthicsDashboard = lazy(() => import('./pages/EthicsDashboard'));
const UserManagementPage = lazy(() => import('./pages/UserManagementPage'));
const LdapMaintenancePage = lazy(() => import('./pages/LdapMaintenancePage'));
const SystemAdminDashboard = lazy(() => import('./pages/SystemAdminDashboard'));
const SystemSecurityLogsPage = lazy(() => import('./pages/SystemSecurityLogsPage'));
const SystemIntegrationsPage = lazy(() => import('./pages/SystemIntegrationsPage'));
const Unauthorized = lazy(() => import('./pages/Unauthorized'));
const PendingAccessPage = lazy(() => import('./pages/PendingAccessPage'));

function guarded(element) {
  return <RequireRole>{element}</RequireRole>;
}

function AppRoutes() {
  return (
    <Suspense fallback={<div style={{ padding: 48, textAlign: 'center' }}>Loading…</div>}>
    <Routes>
      <Route path="/" element={<CustomerForm />} />
      <Route path="/staff-login" element={<Login />} />
      <Route path="/pending-access" element={<RequireAuth><PendingAccessPage /></RequireAuth>} />
      <Route path="/customer-feedback" element={<CustomerFeedbackPage />} />
      <Route path="/unauthorized" element={<RequireAuth><Unauthorized /></RequireAuth>} />
      <Route path="/branch-staff" element={guarded(<BranchStaffDashboard />)} />
      <Route path="/cmd" element={guarded(<CMDDashboard />)} />
      <Route path="/audit" element={guarded(<AuditDashboard />)} />
      <Route path="/executive" element={guarded(<ExecutiveDashboard />)} />
      <Route path="/ethics" element={guarded(<EthicsDashboard />)} />
      <Route path="/work-unit" element={guarded(<WorkUnitDashboard />)} />
      <Route path="/chief-committee" element={guarded(<ChiefCommitteeDashboard />)} />
      <Route path="/admin" element={guarded(<AdminDashboard />)} />
      <Route path="/system-admin" element={guarded(<SystemAdminDashboard />)} />
      <Route path="/system-admin/users" element={guarded(<UserManagementPage />)} />
      <Route path="/system-admin/ldap" element={guarded(<LdapMaintenancePage />)} />
      <Route path="/system-admin/security-logs" element={guarded(<SystemSecurityLogsPage />)} />
      <Route path="/system-admin/integrations" element={guarded(<SystemIntegrationsPage />)} />
      <Route path="/admin/sla-monitoring" element={guarded(<SlaMonitoringPage />)} />
      <Route path="/admin/sla-config" element={guarded(<SlaConfigPage />)} />
      <Route path="/admin/complaint-categories" element={guarded(<ComplaintCategoriesPage />)} />
      <Route path="/admin/nbe-reports" element={guarded(<NBEReports />)} />
      <Route path="/admin/rca" element={guarded(<RcaDashboard />)} />
      <Route path="/admin/customer-experience" element={guarded(<CustomerExperienceDashboard />)} />
    </Routes>
    </Suspense>
  );
}

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#012169',
          fontFamily: "'Segoe UI', 'Inter', 'Cms Amharic', 'Noto Sans Ethiopic', sans-serif",
        },
      }}
    >
      <AuthProvider>
        <Router>
          <SessionIdleWatch />
          <AppRoutes />
        </Router>
      </AuthProvider>
    </ConfigProvider>
  );
}

export default App;
