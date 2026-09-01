import React, { useState } from 'react';
import { Layout, Row, Col } from 'antd';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api';
import { getRouteForRole } from '../constants/authRoutes';
import { useAuth } from '../contexts/AuthContext';
import FeaturesSection from '../components/FeaturesSection';
import LoginForm from '../components/LoginForm';
import './Login.css';

const { Content } = Layout;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { login } = useAuth();

  const onFinish = async (values) => {
    setLoading(true);
    setError('');

    try {
      const username = values.username.toLowerCase();
      const response = await ApiService.login(username, values.password);
      
      // response should have { token, username, role, fullName, district, branch, department }
      const userData = {
        username: response.username,
        role: response.role,
        token: response.token,
        fullName: response.fullName,
        district: response.district,
        branch: response.branch,
        department: response.department
      };

      login(userData);
      
      // Redirect based on role
      const targetRoute = getRouteForRole(response.role);
      navigate(targetRoute);
      
    } catch (err) {
      console.error('Login error:', err);
      setError('Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout className="login-layout">
      <Row style={{ minHeight: '100vh' }}>
        {/* Left Section - Branding & Features */}
        <Col xs={0} sm={0} md={11} lg={12} className="login-left-col">
          <Content className="sideContent">
            <FeaturesSection />
          </Content>
        </Col>

        {/* Right Section - Login Form */}
        <Col xs={24} sm={24} md={13} lg={12} className="login-right-col">
          <Content className="login-content-wrapper">
            <div className="login-form-container">
              <LoginForm onFinish={onFinish} loading={loading} error={error} />
            </div>
          </Content>
        </Col>
      </Row>
    </Layout>
  );
};

export default Login;