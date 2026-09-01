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

  if (!canAccessPath(location.pathname, user?.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}
