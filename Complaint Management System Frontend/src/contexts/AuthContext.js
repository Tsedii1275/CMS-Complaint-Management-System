import React, { createContext, useState, useEffect, useContext, useMemo, useCallback } from 'react';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
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

  // Check authentication on mount (sync check in case of external changes)
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
    console.log('AuthContext - Logging in user:', userData);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
  }, []);

  const logout = useCallback(() => {
    console.log('AuthContext - Logging out user');
    localStorage.removeItem('user');
    setUser(null);
    setIsAuthenticated(false);
  }, []);

  const value = useMemo(() => ({
    isAuthenticated,
    user,
    login,
    logout
  }), [isAuthenticated, user, login, logout]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
