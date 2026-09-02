import React, { useEffect, useState } from 'react';
import { Layout, Row, Col, Form, Input, Button, Alert } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api';
import { getRouteForRole } from '../constants/authRoutes';
import { useAuth } from '../contexts/AuthContext';
import FeaturesSection from '../components/FeaturesSection';
import LoginForm from '../components/LoginForm';
import {
  PASSWORD_POLICY,
  PASSWORD_REQUIREMENTS_MESSAGE,
  SESSION_MESSAGE_KEY,
} from '../constants/securityPolicy';
import './Login.css';

const { Content } = Layout;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [expiredState, setExpiredState] = useState(null);
  const [passwordForm] = Form.useForm();
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    const sessionMessage = sessionStorage.getItem(SESSION_MESSAGE_KEY);
    if (sessionMessage) {
      setError(sessionMessage);
      sessionStorage.removeItem(SESSION_MESSAGE_KEY);
    }
  }, []);

  const onFinish = async (values) => {
    setLoading(true);
    setError('');

    try {
      const username = (values.username || '').trim().toLowerCase();
      const password = (values.password || '').trim();
      const response = await ApiService.login(username, password);

      const userData = {
        username: response.username,
        role: response.role,
        token: response.token,
        fullName: response.fullName,
        district: response.district,
        branch: response.branch,
        department: response.department,
        mustChangePassword: !!response.mustChangePassword
      };

      login(userData);
      navigate(getRouteForRole(response.role));
    } catch (err) {
      if (err?.code === 'PASSWORD_EXPIRED') {
        setExpiredState({
          username: err?.data?.username || values.username,
          passwordChangeToken: err?.data?.passwordChangeToken,
          currentPassword: (values.password || '').trim(),
        });
        setError(err.message);
      } else {
        setError(err?.message || 'Invalid username or password');
      }
    } finally {
      setLoading(false);
    }
  };

  const onExpiredPassword = async (values) => {
    if (!expiredState?.passwordChangeToken) return;
    setLoading(true);
    setError('');
    try {
      await ApiService.changeExpiredPassword({
        passwordChangeToken: expiredState.passwordChangeToken,
        currentPassword: expiredState.currentPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      setExpiredState(null);
      passwordForm.resetFields();
      setError('Password updated successfully. Please login again.');
    } catch (err) {
      setError(err?.message || PASSWORD_REQUIREMENTS_MESSAGE);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout className="login-layout">
      <Row style={{ minHeight: '100vh' }}>
        <Col xs={0} sm={0} md={11} lg={12} className="login-left-col">
          <Content className="sideContent">
            <FeaturesSection />
          </Content>
        </Col>

        <Col xs={24} sm={24} md={13} lg={12} className="login-right-col">
          <Content className="login-content-wrapper">
            <div className="login-form-container">
              {expiredState ? (
                <div style={{ width: '100%', maxWidth: 450 }}>
                  {error ? <Alert type="warning" showIcon message={error} style={{ marginBottom: 16 }} /> : null}
                  <Form form={passwordForm} layout="vertical" onFinish={onExpiredPassword}>
                    <Form.Item
                      name="newPassword"
                      label="New Password"
                      rules={[
                        { required: true, message: 'Please enter a new password.' },
                        {
                          validator: (_, value) => {
                            if (!value || PASSWORD_POLICY.test(value)) return Promise.resolve();
                            return Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE));
                          }
                        }
                      ]}
                    >
                      <Input.Password prefix={<LockOutlined />} placeholder="New Password" />
                    </Form.Item>
                    <Form.Item
                      name="confirmPassword"
                      label="Confirm New Password"
                      dependencies={['newPassword']}
                      rules={[
                        { required: true, message: 'Please confirm the new password.' },
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                            return Promise.reject(new Error('New password and confirmation do not match.'));
                          }
                        })
                      ]}
                    >
                      <Input.Password prefix={<LockOutlined />} placeholder="Confirm New Password" />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={loading} block>
                      Change Password
                    </Button>
                  </Form>
                </div>
              ) : (
                <LoginForm onFinish={onFinish} loading={loading} error={error} />
              )}
            </div>
          </Content>
        </Col>
      </Row>
    </Layout>
  );
};

export default Login;
