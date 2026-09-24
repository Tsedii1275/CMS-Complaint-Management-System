import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import CustomerForm from './pages/CustomerForm';
import Login from './pages/Login';
import BranchStaffDashboard from './pages/BranchStaffDashboard';
import CMDDashboard from './pages/CMDDashboard';
import AuditDashboard from './pages/AuditDashboard';
import WorkUnitDashboard from './pages/WorkUnitDashboard';
import ChiefCommitteeDashboard from './pages/ChiefCommitteeDashboard';
import AdminDashboard from './pages/AdminDashboard';
import NBEReports from './pages/NBEReports';
import RcaDashboard from './pages/RcaDashboard';
import CustomerFeedbackPage from './pages/CustomerFeedbackPage';
import CustomerExperienceDashboard from './pages/CustomerExperienceDashboard';
import SlaConfigPage from './pages/SlaConfigPage';
import SlaMonitoringPage from './pages/SlaMonitoringPage';
import ExecutiveDashboard from './pages/ExecutiveDashboard';
import UserManagementPage from './pages/UserManagementPage';
import LdapMaintenancePage from './pages/LdapMaintenancePage';
import Unauthorized from './pages/Unauthorized';
import { RequireAuth, RequireRole } from './components/RoleGuard';
import { AuthProvider } from './contexts/AuthContext';
import SessionIdleWatch from './components/SessionIdleWatch';
import './index.css';

function guarded(element) {
  return <RequireRole>{element}</RequireRole>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<CustomerForm />} />
      <Route path="/staff-login" element={<Login />} />
      <Route path="/customer-feedback" element={<CustomerFeedbackPage />} />
      <Route path="/unauthorized" element={<RequireAuth><Unauthorized /></RequireAuth>} />
      <Route path="/branch-staff" element={guarded(<BranchStaffDashboard />)} />
      <Route path="/cmd" element={guarded(<CMDDashboard />)} />
      <Route path="/audit" element={guarded(<AuditDashboard />)} />
      <Route path="/executive" element={guarded(<ExecutiveDashboard />)} />
      <Route path="/work-unit" element={guarded(<WorkUnitDashboard />)} />
      <Route path="/chief-committee" element={guarded(<ChiefCommitteeDashboard />)} />
      <Route path="/admin" element={guarded(<AdminDashboard />)} />
      <Route path="/admin/users" element={guarded(<UserManagementPage />)} />
      <Route path="/admin/ldap" element={guarded(<LdapMaintenancePage />)} />
      <Route path="/admin/sla-monitoring" element={guarded(<SlaMonitoringPage />)} />
      <Route path="/admin/sla-config" element={guarded(<SlaConfigPage />)} />
      <Route path="/admin/nbe-reports" element={guarded(<NBEReports />)} />
      <Route path="/admin/rca" element={guarded(<RcaDashboard />)} />
      <Route path="/admin/customer-experience" element={guarded(<CustomerExperienceDashboard />)} />
    </Routes>
  );
}

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#012169',
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
