import React, { useCallback, useEffect, useState } from 'react';
import ApiService from '../services/api';

const AuthContext = React.createContext();

export const useAuth = () => {
  const context = React.useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (error) {
        console.error('AuthContext - Error parsing user data:', error);
        localStorage.removeItem('user');
      }
    }
    return null;
  });

  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    return !!localStorage.getItem('user');
  });

  useEffect(() => {
    const checkAuth = () => {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          const userData = JSON.parse(userStr);
          setUser(userData);
          setIsAuthenticated(true);
        } catch (error) {
          console.error('AuthContext - Error parsing user data:', error);
          localStorage.removeItem('user');
          setUser(null);
          setIsAuthenticated(false);
        }
      } else {
        setUser(null);
        setIsAuthenticated(false);
      }
    };

    checkAuth();
  }, []);

  const login = useCallback((userData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
  }, []);

  const updateUser = useCallback((partial) => {
    setUser((current) => {
      const next = { ...(current ?? {}), ...partial };
      localStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  }, []);

  const logout = useCallback(() => {
    ApiService.logout().catch(() => {}).finally(() => {
      localStorage.removeItem('user');
      setUser(null);
      setIsAuthenticated(false);
    });
  }, []);

  const value = React.useMemo(() => ({
    isAuthenticated,
    user,
    login,
    logout,
    updateUser
  }), [isAuthenticated, user, login, logout, updateUser]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
