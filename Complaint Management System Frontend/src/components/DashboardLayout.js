import React, { useState, useEffect } from 'react';
import { Layout, Typography, Button, Menu, Grid, Drawer, Popover, Badge, List, Modal, Form, Input, Alert, message, Tag } from 'antd';
import {
  UserOutlined,
  BellOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  HomeOutlined,
  WarningFilled,
  CloseOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SettingOutlined,
  PieChartOutlined,
  ClockCircleOutlined,
  LockOutlined
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { BRAND_COLORS } from '../constants/theme';
import { useAuth } from '../contexts/AuthContext';
import ApiService from '../services/api';
import UserAccountMenu from './UserAccountMenu';
import '../index.css';

const PASSWORD_POLICY = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
const PASSWORD_REQUIREMENTS_MESSAGE = 'New password does not meet the password requirements.';

function SidebarMenu({ collapsed, userRole, user, onNavigate }) {
  const location = useLocation();
  const navigate = useNavigate();
  const role = user?.role || userRole || '';

  const menuItems = [];

  if (role === 'ROLE_ADMIN' || userRole === 'admin') {
    menuItems.push(
      {
        key: '/admin',
        icon: <DashboardOutlined />,
        label: 'Analytics Dashboard',
      },
      {
        key: '/admin/sla-monitoring',
        icon: <ClockCircleOutlined />,
        label: 'SLA Performance & Monitoring',
      },
      {
        key: '/admin/nbe-reports',
        icon: <FileTextOutlined />,
        label: 'NBE Reports',
      },
      {
        key: '/admin/rca',
        icon: <SettingOutlined />,
        label: 'Root Cause Analysis',
      },
      {
        key: '/admin/customer-experience',
        icon: <PieChartOutlined />,
        label: 'Customer Experience',
      },
      {
        key: '/admin/users',
        icon: <UserOutlined />,
        label: 'User & Role Management',
      },
      {
        key: '/admin/sla-config',
        icon: <SettingOutlined />,
        label: 'SLA Governance & Policy Configuration',
      }
    );
  } else if (role.includes('AUDIT')) {
    menuItems.push({
      key: '/audit',
      icon: <HomeOutlined />,
      label: 'Audit Workspace',
    });
  } else if (role.includes('COMMITTEE')) {
    menuItems.push({
      key: '/chief-committee',
      icon: <HomeOutlined />,
      label: 'Committee Workspace',
    });
  } else if (role.includes('CHIEF_EXPERIENCE')) {
    menuItems.push({
      key: '/executive',
      icon: <HomeOutlined />,
      label: 'Executive Monitoring Workspace',
    });
  } else if (role === 'ROLE_CUSTOMER_CARE_OFFICER' || role === 'ROLE_CUSTOMER_CARE_TEAM_LEADER' || role.includes('CUSTOMER_CARE_OFFICER')) {
    menuItems.push(
      {
        key: '/branch-staff',
        icon: <FileTextOutlined />,
        label: 'Complaint Registration',
      },
      {
        key: '/cmd',
        icon: <DashboardOutlined />,
        label: 'Complaint Management (CMD)',
      }
    );
  } else if (role === 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER' || role === 'ROLE_SERVICE_QUALITY_DIRECTOR') {
    menuItems.push(
      {
        key: '/branch-staff',
        icon: <FileTextOutlined />,
        label: 'Complaint Registration',
      },
      {
        key: '/cmd',
        icon: <DashboardOutlined />,
        label: 'Operational Dashboard',
      }
    );
  } else if (
    role.startsWith('ROLE_BRANCH_MANAGER') ||
    role.startsWith('ROLE_CONTACT_CENTER') ||
    role.startsWith('ROLE_DIGITAL_MARKETING') ||
    role === 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP' ||
    role.startsWith('ROLE_CUSTOMER_SERVICE') ||
    role.startsWith('ROLE_CUSTOMER_CARE')
  ) {
    menuItems.push({
      key: '/cmd',
      icon: <HomeOutlined />,
      label: 'Operational Dashboard',
    });
  } else {
    menuItems.push({
      key: location.pathname || '/cmd',
      icon: <HomeOutlined />,
      label: 'Dashboard',
    });
  }

  return (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      items={menuItems}
      style={{ borderRight: 'none', padding: '0 8px' }}
      onClick={({ key }) => {
        navigate(key);
        if (onNavigate) onNavigate();
      }}
    />
  );
}

function AppHeader({ collapsed, isMobile, onMenuClick, userRole, user, onLogout, onUpdatePassword }) {
  const [tasks, setTasks] = useState([]);
  const [popoverOpen, setPopoverOpen] = useState(false);

  const fetchTasks = async () => {
    try {
      if (!user || userRole === 'admin') return;
      const data = await ApiService.getEnrichedTasks();
      setTasks(data || []);
    } catch (err) {
      console.error('Failed to fetch tasks in AppHeader:', err);
    }
  };

  useEffect(() => {
    fetchTasks();
    const interval = setInterval(fetchTasks, 15000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const slaWarningTasks = tasks.filter(t =>
    t.slaStatus === 'OVERDUE' || t.slaStatus === 'BREACHED' || t.slaStatus === 'APPROACHING'
  );

  const titleContent = (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', maxWidth: '320px' }}>
      <span style={{ fontWeight: 'bold', color: BRAND_COLORS.primary }}>
        {userRole === 'admin' ? 'Notifications' : 'Stage SLA Alerts'}
      </span>
      <Button
        type="text"
        icon={<CloseOutlined style={{ fontSize: '12px' }} />}
        size="small"
        onClick={() => setPopoverOpen(false)}
        style={{ width: 20, height: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0 }}
      />
    </div>
  );

  let popoverBody;
  if (userRole === 'admin') {
    popoverBody = (
        <div style={{ padding: '16px', textAlign: 'center', color: '#8c8c8c' }}>
          No unread system notifications.
        </div>
    );
  } else if (slaWarningTasks.length === 0) {
    popoverBody = (
        <div style={{ padding: '16px', textAlign: 'center', color: '#8c8c8c' }}>
          No active SLA warning or breach alerts for your unit.
        </div>
    );
  } else {
    popoverBody = (
        <List
          size="small"
          dataSource={slaWarningTasks}
          renderItem={(task) => {
            const isBreached = task.slaStatus === 'OVERDUE' || task.slaStatus === 'BREACHED';
            const statusLabel = isBreached ? 'Breached' : 'Warning';
            const statusColor = isBreached ? '#dc2626' : '#d97706';
            const badgeTagColor = isBreached ? 'error' : 'warning';

            const dbcTicketNumber = task.dbcTicketId || task.variables?.dbcTicketId || task.complaintId || 'N/A';
            const workflowStage = task.name || task.taskDefinitionKey || 'Workflow Processing';
            const slaDeadline = task.dueDate
              ? new Date(task.dueDate).toLocaleString()
              : (task.variables?.expectedResolutionDate || 'Standard SLA Target');
            const currentDateTime = new Date().toLocaleString();

            return (
              <List.Item style={{ padding: '10px 4px', borderBottom: '1px solid #f1f5f9' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', width: '100%' }}>
                  <WarningFilled style={{ color: statusColor, marginTop: '4px', fontSize: '16px' }} />
                  <div style={{ flex: 1, fontSize: '12px', lineHeight: 1.5 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                      <span style={{ fontWeight: 700, color: '#0f172a', fontFamily: 'monospace' }}>
                        DBC Ticket: {dbcTicketNumber}
                      </span>
                      <Tag color={badgeTagColor} style={{ fontSize: '10px', fontWeight: 700, margin: 0 }}>
                        {statusLabel}
                      </Tag>
                    </div>
                    <div><strong style={{ color: '#475569' }}>Workflow Stage:</strong> {workflowStage}</div>
                    <div><strong style={{ color: '#475569' }}>SLA Deadline:</strong> {slaDeadline}</div>
                    <div style={{ color: '#94a3b8', fontSize: '11px', marginTop: '2px' }}>Alert Time: {currentDateTime}</div>
                  </div>
                </div>
              </List.Item>
            );
          }}
        />
    );
  }

  const popoverContent = (
    <div style={{ width: '100%', maxWidth: '330px', maxHeight: '420px', overflowY: 'auto' }}>
      {popoverBody}
    </div>
  );

  const displayName = user?.fullName || user?.username || 'User';
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <Layout.Header className="cms-app-header" style={{
      padding: isMobile ? '0 12px' : '0 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      zIndex: 10,
      boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
      height: '64px',
      backgroundColor: BRAND_COLORS.primary
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: isMobile ? '8px' : '12px', minWidth: 0 }}>
        <Button
          type="text"
          icon={collapsed && !isMobile ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={onMenuClick}
          style={{
            fontSize: '18px',
            color: 'white',
            display: isMobile ? 'flex' : 'none',
            padding: 0,
            width: 40,
            height: 40,
            alignItems: 'center',
            justifyContent: 'center'
          }}
          className="sidebar-toggle"
        />
        <img
          src="/download.png"
          alt="Dashen Bank Logo"
          style={{ height: isMobile ? '26px' : '32px', width: 'auto', objectFit: 'contain', flexShrink: 0 }}
        />
        <Typography.Title level={4} className="cms-header-title" style={{ color: BRAND_COLORS.white, margin: 0, letterSpacing: '0.5px', whiteSpace: 'nowrap' }}>
          Complaint Management
        </Typography.Title>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: isMobile ? '12px' : '20px', flexShrink: 0 }}>
        <Popover
          content={popoverContent}
          title={titleContent}
          trigger="click"
          open={popoverOpen}
          onOpenChange={(visible) => setPopoverOpen(visible)}
          placement="bottomRight"
          overlayStyle={{ zIndex: 1050, maxWidth: 'calc(100vw - 16px)' }}
        >
          <Badge
            count={slaWarningTasks.length}
            offset={[8, -2]}
          >
            <BellOutlined style={{ fontSize: '18px', cursor: 'pointer', color: BRAND_COLORS.white }} />
          </Badge>
        </Popover>
        <UserAccountMenu
          displayName={displayName}
          initial={initial}
          onUpdatePassword={onUpdatePassword}
          onLogout={onLogout}
        />
      </div>
    </Layout.Header>
  );
}

const DashboardLayout = ({ children, userRole }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordForm] = Form.useForm();
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.lg;
  const effectiveRole = user?.role || userRole || '';

  const handleLogout = () => {
    console.log('DashboardLayout - Logging out user');
    logout();
    console.log('DashboardLayout - User logged out via AuthContext, redirecting to /staff-login');
    navigate('/staff-login');
  };

  const closePasswordModal = () => {
    if (passwordSubmitting) return;
    setIsPasswordModalOpen(false);
    setPasswordError('');
    passwordForm.resetFields();
  };

  const handleUpdatePassword = async (values) => {
    if (passwordSubmitting) return;
    setPasswordError('');
    setPasswordSubmitting(true);
    try {
      const result = await ApiService.updatePassword(
        values.currentPassword,
        values.newPassword,
        values.confirmPassword
      );
      message.success(result?.message || 'Password updated successfully.');
      passwordForm.resetFields();
      setIsPasswordModalOpen(false);
    } catch (err) {
      setPasswordError(err?.message || 'Unable to update password. Please try again.');
    } finally {
      setPasswordSubmitting(false);
    }
  };

  return (
    <Layout style={{ height: '100vh', width: '100%', maxWidth: '100%', overflow: 'hidden' }}>
      <AppHeader
        collapsed={collapsed}
        isMobile={isMobile}
        onMenuClick={() => {
          if (isMobile) {
            setMobileDrawerOpen((open) => !open);
          } else {
            setCollapsed(!collapsed);
          }
        }}
        userRole={effectiveRole}
        user={user}
        onLogout={handleLogout}
        onUpdatePassword={() => {
          setPasswordError('');
          passwordForm.resetFields();
          setIsPasswordModalOpen(true);
        }}
      />
      <Layout style={{ overflow: 'hidden', minWidth: 0 }}>
        {/* Desktop Sidebar */}
        {!isMobile && (
          <Layout.Sider
            trigger={null}
            collapsible
            collapsed={collapsed}
            width={260}
            theme="light"
            style={{
              borderRight: `1px solid ${BRAND_COLORS.sidebarBorder}`,
              boxShadow: '2px 0 8px rgba(0,0,0,0.02)',
              zIndex: 5,
              height: 'calc(100vh - 64px)',
              position: 'relative'
            }}
          >
            <div style={{ padding: '16px', display: 'flex', justifyContent: collapsed ? 'center' : 'flex-end' }}>
              <Button
                type="text"
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)}
                style={{ fontSize: '16px' }}
              />
            </div>
            <SidebarMenu collapsed={collapsed} userRole={effectiveRole} user={user} />
          </Layout.Sider>
        )}

        {/* Mobile Sidebar */}
        <Drawer
          title="Complaint Management"
          placement="left"
          onClose={() => setMobileDrawerOpen(false)}
          open={isMobile && mobileDrawerOpen}
          width={Math.min(280, typeof window !== 'undefined' ? window.innerWidth - 24 : 280)}
          styles={{ body: { padding: 0 } }}
        >
          <SidebarMenu
            collapsed={false}
            userRole={effectiveRole}
            user={user}
            onNavigate={() => setMobileDrawerOpen(false)}
          />
        </Drawer>

        <Layout style={{ display: 'flex', flexDirection: 'column' }}>
          <Layout.Content
            className="main-content-area"
            style={{
              background: BRAND_COLORS.background,
              overflowY: 'auto',
              overflowX: 'hidden',
              flex: '1 1 auto',
              minWidth: 0,
            }}
          >
            <div className="cms-page">{children}</div>
          </Layout.Content>
        </Layout>
      </Layout>

      <Modal
        width="auto"
        style={{ maxWidth: 520 }}
        title={(
          <span style={{ color: BRAND_COLORS.primary, fontWeight: 'bold', fontSize: '18px' }}>
            <LockOutlined style={{ marginRight: '8px' }} /> Update Password
          </span>
        )}
        open={isPasswordModalOpen}
        onCancel={closePasswordModal}
        destroyOnClose
        maskClosable={!passwordSubmitting}
        footer={[
          <Button key="cancel" onClick={closePasswordModal} disabled={passwordSubmitting}>
            Cancel
          </Button>,
          <Button
            key="update"
            type="primary"
            loading={passwordSubmitting}
            disabled={passwordSubmitting}
            onClick={() => passwordForm.submit()}
            style={{ background: BRAND_COLORS.primary }}
          >
            Update Password
          </Button>
        ]}
      >
        <div style={{ padding: '12px 0' }}>
          {passwordError ? (
            <Alert type="error" showIcon message={passwordError} style={{ marginBottom: 16 }} />
          ) : null}
          <Form
            form={passwordForm}
            layout="vertical"
            onFinish={handleUpdatePassword}
            autoComplete="off"
          >
            <Form.Item
              name="currentPassword"
              label="Current Password"
              rules={[{ required: true, message: 'Please enter your current password.' }]}
            >
              <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Current Password" />
            </Form.Item>
            <Form.Item
              name="newPassword"
              label="New Password"
              rules={[
                { required: true, message: 'Please enter a new password.' },
                {
                  validator: (_, value) => {
                    if (!value) return Promise.resolve();
                    if (!PASSWORD_POLICY.test(value)) {
                      return Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE));
                    }
                    if (value === passwordForm.getFieldValue('currentPassword')) {
                      return Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE));
                    }
                    return Promise.resolve();
                  }
                }
              ]}
            >
              <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="New Password" />
            </Form.Item>
            <Form.Item
              name="confirmPassword"
              label="Confirm New Password"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: 'Please confirm the new password.' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('New password and confirmation do not match.'));
                  }
                })
              ]}
            >
              <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Confirm New Password" />
            </Form.Item>
          </Form>
        </div>
      </Modal>
    </Layout>
  );
};

function DashboardLayoutRoot(props) {
  return <DashboardLayout key="layout-password-v4" {...props} />;
}

export default DashboardLayoutRoot;
