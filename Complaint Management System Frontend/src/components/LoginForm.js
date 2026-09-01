import React from 'react';
import { Form, Input, Button, Typography, Divider, Alert } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined } from '@ant-design/icons';
import logo from '../assets/Dashen-Bank-Logo-Addis-Ababa-Ethiopia.png';

const { Title, Text } = Typography;

const LoginForm = ({ onFinish, loading, error }) => {
  return (
    <div style={{ width: '100%', maxWidth: 450 }}>
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        {/* Mobile-only logo (visible when left panel is hidden) */}
        <img
          src={logo}
          alt="Dashen Bank Logo"
          className="mobile-login-logo"
          style={{ height: 60, marginBottom: 20, display: 'none' }}
        />
        <Title level={3} style={{ 
          color: 'var(--primary-color, #012169)', 
          fontWeight: 700,
          marginBottom: 10
        }}>
          Dashen Bank - Always One Step Ahead
        </Title>
        
        <div style={{ margin: '30px 0', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <h1 className="animate-typewriter" style={{ 
            textAlign: 'center',
            fontWeight: 700,
            marginBottom: '1rem',
            fontSize: '1.125rem',
            lineHeight: '1.75rem',
            color: 'rgba(17,24,39,1)',
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            width: '66.6667%',
            margin: '0 auto 1rem auto'
          }}>
            Welcome back!
          </h1>
          <Text style={{ fontSize: 18, color: '#666', marginTop: 10, display: 'block' }}>
            Log in to your account
          </Text>
        </div>
      </div>

      <Divider dashed style={{ margin: '24px 0', borderColor: '#eef2f6' }} />

      {error && (
        <Alert
          message={error}
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
      )}

      <Form
        name="normal_login"
        className="login-form"
        onFinish={onFinish}
        size="large"
        style={{
          fontFamily: 'Arial, Helvetica, sans-serif',
          fontWeight: 500,
          color: '#2d3142',
          fontSize: 14
        }}
      >
        <Form.Item
          name="username"
          rules={[{ required: true, message: 'Please input your Username!' }]}
        >
          <Input 
            prefix={<UserOutlined className="site-form-item-icon" style={{ color: '#bfbfbf' }} />} 
            placeholder="Username" 
          />
        </Form.Item>
        <Form.Item
          name="password"
          rules={[{ required: true, message: 'Please input your Password!' }]}
        >
          <Input.Password
            prefix={<LockOutlined className="site-form-item-icon" style={{ color: '#bfbfbf' }} />}
            placeholder="Password"
          />
        </Form.Item>

        <Form.Item>
          <Button 
            type="primary" 
            htmlType="submit" 
            loading={loading}
            className="login-form-button" 
            style={{ width: '100%' }} 
            icon={<LoginOutlined />}
          >
            Log in
          </Button>
        </Form.Item>
      </Form>

      <div 
        className="login-footer ant-space ant-space-horizontal ant-space-align-center" 
        style={{
          color: '#2d3142',
          fontSize: 14,
          fontFamily: 'Arial, Helvetica, sans-serif',
          fontWeight: 500,
          flexWrap: 'wrap', 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center',
          position: 'absolute', 
          bottom: -40, 
          left: 0,
          width: '100%',
          gap: 8
        }}
      >
        <div className="ant-space-item">
          <div style={{ 
            borderLeft: '1px solid #9ca3af', 
            paddingLeft: 16, 
            display: 'flex', 
            alignItems: 'center',
            flexWrap: 'wrap',
            justifyContent: 'center'
          }}>
            <span style={{ fontSize: 14, color: '#4b5563', marginRight: 8 }}>
              © 2026 Dashen Bank S.C. | © All Rights Reserved.
            </span>
            <span style={{ fontSize: 14, color: '#4b5563' }}>
              CMS - Developed by IT Modernization Program Management
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginForm;
