import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getRouteForRole } from '../constants/authRoutes';

function Unauthorized() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const home = getRouteForRole(user?.role);

  return (
    <Result
      status="403"
      title="403"
      subTitle="You are signed in, but you do not have access to this page."
      extra={
        <Button type="primary" onClick={() => navigate(home, { replace: true })}>
          Back to my workspace
        </Button>
      }
    />
  );
}

export default Unauthorized;
