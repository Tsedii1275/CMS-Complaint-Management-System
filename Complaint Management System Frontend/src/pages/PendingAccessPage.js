import React from 'react';
import { Card, Typography, Button, Tag, Space, Row, Col, Alert } from 'antd';
import { ClockCircleOutlined, LogoutOutlined, UserOutlined, MailOutlined, IdcardOutlined, ClusterOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text, Paragraph } = Typography;

function PendingAccessPage({ user }) {
    const { logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/staff-login');
    };

    const storedUser = user || JSON.parse(localStorage.getItem('user') || '{}');

    return (
        <div
            style={{
                minHeight: '100vh',
                background: `linear-gradient(135deg, ${BRAND_COLORS.primary} 0%, #0b1936 100%)`,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '24px',
                fontFamily: "'Inter', sans-serif"
            }}
        >
            <div style={{ width: '100%', maxWidth: '640px' }}>
                {/* Header Branding */}
                <div style={{ textAlign: 'center', marginBottom: '32px' }}>
                    <div
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: '64px',
                            height: '64px',
                            borderRadius: '16px',
                            background: 'rgba(255, 255, 255, 0.12)',
                            backdropFilter: 'blur(10px)',
                            marginBottom: '16px',
                            border: '1px solid rgba(255, 255, 255, 0.2)',
                            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.2)'
                        }}
                    >
                        <SafetyCertificateOutlined style={{ fontSize: '32px', color: BRAND_COLORS.accent }} />
                    </div>
                    <Title level={2} style={{ color: '#ffffff', margin: 0, fontWeight: 700, letterSpacing: '-0.5px' }}>
                        Dashen Bank CMS
                    </Title>
                    <Text style={{ color: 'rgba(255, 255, 255, 0.75)', fontSize: '15px' }}>
                        Complaint Management System • Identity & Access Control
                    </Text>
                </div>

                {/* Main Clean Card Layout */}
                <Card
                    bordered={false}
                    style={{
                        borderRadius: '16px',
                        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.3)',
                        background: '#ffffff',
                        overflow: 'hidden'
                    }}
                    bodyStyle={{ padding: '36px' }}
                >
                    {/* Status Badge */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                        <Tag
                            icon={<ClockCircleOutlined />}
                            color="warning"
                            style={{
                                fontSize: '13px',
                                padding: '6px 16px',
                                borderRadius: '20px',
                                fontWeight: 600,
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px'
                            }}
                        >
                            PENDING ADMINISTRATOR APPROVAL
                        </Tag>
                        <Text type="secondary" style={{ fontSize: '13px' }}>
                            Auth Source: <Tag color="blue" style={{ margin: 0 }}>Active Directory</Tag>
                        </Text>
                    </div>

                    {/* Welcome Message */}
                    <Title level={3} style={{ color: BRAND_COLORS.primary, marginTop: 0, marginBottom: '8px', fontWeight: 700 }}>
                        Welcome, {storedUser.fullName || storedUser.username || 'User'}!
                    </Title>
                    <Paragraph type="secondary" style={{ fontSize: '14px', marginBottom: '24px' }}>
                        Your identity has been authenticated successfully.
                    </Paragraph>

                    {/* User Information Section */}
                    <div
                        style={{
                            background: '#f8fafc',
                            border: '1px solid #e2e8f0',
                            borderRadius: '12px',
                            padding: '20px',
                            marginBottom: '28px'
                        }}
                    >
                        <Text strong style={{ display: 'block', color: BRAND_COLORS.primary, marginBottom: '16px', fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                            Identified Profile Details
                        </Text>
                        <Row gutter={[16, 16]}>
                            <Col span={12}>
                                <Space size="small">
                                    <UserOutlined style={{ color: BRAND_COLORS.primary }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Full Name</Text>
                                        <Text strong style={{ fontSize: '14px' }}>{storedUser.fullName || 'N/A'}</Text>
                                    </div>
                                </Space>
                            </Col>
                            <Col span={12}>
                                <Space size="small">
                                    <MailOutlined style={{ color: BRAND_COLORS.primary }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Email Address</Text>
                                        <Text strong style={{ fontSize: '14px' }}>{storedUser.email || 'N/A'}</Text>
                                    </div>
                                </Space>
                            </Col>
                            <Col span={12}>
                                <Space size="small">
                                    <IdcardOutlined style={{ color: BRAND_COLORS.primary }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>AD Job Title</Text>
                                        <Text strong style={{ fontSize: '14px' }}>{storedUser.adJobTitle || storedUser.title || 'N/A'}</Text>
                                    </div>
                                </Space>
                            </Col>
                            <Col span={12}>
                                <Space size="small">
                                    <ClusterOutlined style={{ color: BRAND_COLORS.primary }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '12px', display: 'block' }}>Work Unit / Department</Text>
                                        <Text strong style={{ fontSize: '14px' }}>{storedUser.department || storedUser.workUnit || 'N/A'}</Text>
                                    </div>
                                </Space>
                            </Col>
                        </Row>
                    </div>

                    {/* Pending Approval Message Box */}
                    <Alert
                        type="info"
                        showIcon
                        message={<Text strong style={{ color: '#0369a1', fontSize: '15px' }}>Access Pending Approval</Text>}
                        description={
                            <Paragraph style={{ margin: '8px 0 0 0', color: '#334155', lineHeight: '1.6', fontSize: '14px' }}>
                                Your account has been successfully identified through Active Directory.
                                <br /><br />
                                You currently do not have access to the Complaint Management System.
                                <br /><br />
                                Your access request is pending administrator approval.
                                Once an administrator assigns the appropriate role and approves your account, you will be able to access the system.
                            </Paragraph>
                        }
                        style={{
                            borderRadius: '12px',
                            border: '1px solid #bae6fd',
                            backgroundColor: '#f0f9ff',
                            padding: '16px 20px',
                            marginBottom: '28px'
                        }}
                    />

                    {/* Action Buttons (Only Logout - No Menus/Nav) */}
                    <div style={{ textAlign: 'center' }}>
                        <Button
                            type="default"
                            icon={<LogoutOutlined />}
                            size="large"
                            onClick={handleLogout}
                            style={{
                                borderRadius: '8px',
                                fontWeight: 600,
                                borderColor: '#cbd5e1',
                                padding: '0 32px'
                            }}
                        >
                            Sign Out
                        </Button>
                    </div>
                </Card>

                {/* Footer */}
                <div style={{ textAlign: 'center', marginTop: '24px' }}>
                    <Text style={{ color: 'rgba(255, 255, 255, 0.5)', fontSize: '12px' }}>
                        © {new Date().getFullYear()} Dashen Bank S.C. All rights reserved.
                    </Text>
                </div>
            </div>
        </div>
    );
}

export default PendingAccessPage;
