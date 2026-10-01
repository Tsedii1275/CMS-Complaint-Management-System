import React from 'react';
import { Card, Typography, Button, Tag, Row, Col, Alert, Divider, Space } from 'antd';
import {
    ClockCircleOutlined,
    LogoutOutlined,
    UserOutlined,
    MailOutlined,
    IdcardOutlined,
    ClusterOutlined,
    CheckCircleFilled
} from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
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
                backgroundColor: '#f1f5f9',
                backgroundImage: 'radial-gradient(#cbd5e1 1px, transparent 1px)',
                backgroundSize: '24px 24px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '32px 16px',
                fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
            }}
        >
            <div style={{ width: '100%', maxWidth: '620px' }}>
                {/* Clean Top Logo & Title Container */}
                <div style={{ textAlign: 'center', marginBottom: '28px' }}>
                    <div
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: '12px 24px',
                            background: '#ffffff',
                            borderRadius: '12px',
                            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.05)',
                            marginBottom: '16px',
                            border: '1px solid #e2e8f0'
                        }}
                    >
                        <img
                            src="/download.png"
                            alt="Dashen Bank Logo"
                            style={{ height: '36px', width: 'auto', objectFit: 'contain' }}
                        />
                    </div>
                    <Title
                        level={3}
                        style={{
                            color: BRAND_COLORS.primary || '#002855',
                            margin: 0,
                            fontWeight: 700,
                            letterSpacing: '-0.3px'
                        }}
                    >
                        Dashen Bank Complaint Management System
                    </Title>
                    <Text type="secondary" style={{ fontSize: '14px' }}>
                        Enterprise Identity & Role Access Portal
                    </Text>
                </div>

                {/* Primary Card */}
                <Card
                    bordered={false}
                    style={{
                        borderRadius: '16px',
                        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04)',
                        background: '#ffffff',
                        overflow: 'hidden'
                    }}
                    bodyStyle={{ padding: '32px 36px' }}
                >
                    {/* Status Ribbon */}
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            paddingBottom: '20px',
                            borderBottom: '1px solid #f1f5f9',
                            marginBottom: '24px'
                        }}
                    >
                        <Space align="center" size={8}>
                            <CheckCircleFilled style={{ color: '#10b981', fontSize: '18px' }} />
                            <Text strong style={{ color: '#0f172a', fontSize: '14px' }}>
                                AD Identity Verified
                            </Text>
                        </Space>
                        <Tag
                            icon={<ClockCircleOutlined />}
                            style={{
                                color: '#b45309',
                                backgroundColor: '#fef3c7',
                                borderColor: '#fde68a',
                                fontSize: '12px',
                                padding: '4px 12px',
                                borderRadius: '12px',
                                fontWeight: 600,
                                margin: 0
                            }}
                        >
                            Pending Role Approval
                        </Tag>
                    </div>

                    {/* Greeting */}
                    <div style={{ marginBottom: '20px' }}>
                        <Title level={4} style={{ color: '#0f172a', margin: '0 0 6px 0', fontWeight: 600 }}>
                            Hello, {storedUser.fullName || storedUser.username || 'User'}
                        </Title>
                        <Text style={{ color: '#64748b', fontSize: '14px' }}>
                            Your credentials have been authenticated via Active Directory. Your account registration is complete and is currently awaiting administrator role assignment.
                        </Text>
                    </div>

                    {/* User Profile Card */}
                    <div
                        style={{
                            background: '#f8fafc',
                            border: '1px solid #e2e8f0',
                            borderRadius: '12px',
                            padding: '18px 20px',
                            marginBottom: '24px'
                        }}
                    >
                        <Row gutter={[16, 16]}>
                            <Col span={12}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <UserOutlined style={{ color: BRAND_COLORS.primary || '#002855', fontSize: '16px' }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '11px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Username</Text>
                                        <Text strong style={{ fontSize: '13px', color: '#1e293b' }}>{storedUser.username || 'N/A'}</Text>
                                    </div>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <MailOutlined style={{ color: BRAND_COLORS.primary || '#002855', fontSize: '16px' }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '11px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Email</Text>
                                        <Text strong style={{ fontSize: '13px', color: '#1e293b', wordBreak: 'break-all' }}>{storedUser.email || 'N/A'}</Text>
                                    </div>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <IdcardOutlined style={{ color: BRAND_COLORS.primary || '#002855', fontSize: '16px' }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '11px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.5px' }}>AD Job Title</Text>
                                        <Text strong style={{ fontSize: '13px', color: '#1e293b' }}>{storedUser.adJobTitle || storedUser.title || 'N/A'}</Text>
                                    </div>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <ClusterOutlined style={{ color: BRAND_COLORS.primary || '#002855', fontSize: '16px' }} />
                                    <div>
                                        <Text type="secondary" style={{ fontSize: '11px', display: 'block', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Work Unit</Text>
                                        <Text strong style={{ fontSize: '13px', color: '#1e293b' }}>{storedUser.department || storedUser.workUnit || 'N/A'}</Text>
                                    </div>
                                </div>
                            </Col>
                        </Row>
                    </div>

                    {/* Notice Callout */}
                    <Alert
                        type="info"
                        showIcon
                        message={<Text strong style={{ color: '#0369a1', fontSize: '14px' }}>What happens next?</Text>}
                        description={
                            <Paragraph style={{ margin: '4px 0 0 0', color: '#334155', fontSize: '13px', lineHeight: '1.5' }}>
                                An administrator will review your account and assign the appropriate application role (e.g., Customer Care Officer, Work Unit Resolver, Branch Manager). Once approved, you will gain full access upon your next login.
                            </Paragraph>
                        }
                        style={{
                            borderRadius: '10px',
                            border: '1px solid #bae6fd',
                            backgroundColor: '#f0f9ff',
                            padding: '14px 16px',
                            marginBottom: '24px'
                        }}
                    />

                    <Divider style={{ margin: '20px 0' }} />

                    {/* Action */}
                    <div style={{ textAlign: 'center' }}>
                        <Button
                            type="default"
                            icon={<LogoutOutlined />}
                            size="large"
                            onClick={handleLogout}
                            style={{
                                borderRadius: '8px',
                                fontWeight: 600,
                                padding: '0 28px',
                                height: '42px',
                                borderColor: '#cbd5e1',
                                color: '#334155'
                            }}
                        >
                            Sign Out
                        </Button>
                    </div>
                </Card>

                {/* Footer */}
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                    <Text style={{ color: '#94a3b8', fontSize: '12px' }}>
                        © {new Date().getFullYear()} Dashen Bank S.C. All rights reserved.
                    </Text>
                </div>
            </div>
        </div>
    );
}

export default PendingAccessPage;
