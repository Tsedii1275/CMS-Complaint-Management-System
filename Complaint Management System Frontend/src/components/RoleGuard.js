import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { canAccessPath } from '../constants/authRoutes';

export function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/staff-login" replace />;
  }
  return children;
}

export function RequireRole({ children }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/staff-login" replace />;
  }

  const isPending = user && (user.approved === false || user.approvalStatus === 'PENDING_APPROVAL' || user.approvalStatus === 'REJECTED' || user.role === 'ROLE_PENDING');
  if (isPending) {
    if (location.pathname !== '/pending-access') {
      return <Navigate to="/pending-access" replace />;
    }
  }

  if (!isPending && !canAccessPath(location.pathname, user?.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}
