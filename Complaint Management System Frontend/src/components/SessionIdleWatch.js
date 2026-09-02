import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { SESSION_MESSAGE_KEY, SESSION_TIMEOUT_MESSAGE, SESSION_TIMEOUT_MS } from '../constants/securityPolicy';

const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];

export default function SessionIdleWatch() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      return undefined;
    }

    let timer;
    const expireSession = () => {
      sessionStorage.setItem(SESSION_MESSAGE_KEY, SESSION_TIMEOUT_MESSAGE);
      logout();
      navigate('/staff-login');
    };

    const resetTimer = () => {
      clearTimeout(timer);
      timer = setTimeout(expireSession, SESSION_TIMEOUT_MS);
    };

    ACTIVITY_EVENTS.forEach((eventName) => window.addEventListener(eventName, resetTimer));
    resetTimer();

    return () => {
      clearTimeout(timer);
      ACTIVITY_EVENTS.forEach((eventName) => window.removeEventListener(eventName, resetTimer));
    };
  }, [isAuthenticated, logout, navigate]);

  return null;
}
