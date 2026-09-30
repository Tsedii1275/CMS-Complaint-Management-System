import React, { useState, useEffect } from 'react';
import { Table, Card, Typography, Button, Tag, Modal, Form, Input, Select, Switch, Space, Row, Col, Alert, Tooltip, Popconfirm, Tabs, Badge, message as antMessage } from 'antd';
import { UserOutlined, PlusOutlined, EditOutlined, KeyOutlined, SearchOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { PASSWORD_POLICY, PASSWORD_REQUIREMENTS_MESSAGE } from '../constants/securityPolicy';

const { Title, Text } = Typography;
const { Option } = Select;

const ROLE_OPTIONS = [
  { label: 'Customer Care Senior Manager', value: 'ROLE_CUSTOMER_CARE_SENIOR_MANAGER' },
  { label: 'Service Quality Director', value: 'ROLE_SERVICE_QUALITY_DIRECTOR' },
  { label: 'Customer Care Team Leader', value: 'ROLE_CUSTOMER_CARE_TEAM_LEADER' },
  { label: 'Customer Care Officer', value: 'ROLE_CUSTOMER_CARE_OFFICER' },
  { label: 'Chief Experience Officer', value: 'ROLE_CHIEF_EXPERIENCE_OFFICER' },
  { label: 'Branch Manager', value: 'ROLE_BRANCH_MANAGER' },
  { label: 'Contact Center Senior Manager', value: 'ROLE_CONTACT_CENTER_SENIOR_MANAGER' },
  { label: 'Digital Marketing Senior Manager', value: 'ROLE_DIGITAL_MARKETING_SENIOR_MANAGER' },
  { label: 'Customer Experience Partnership', value: 'ROLE_CUSTOMER_EXPERIENCE_PARTNERSHIP' },
  { label: 'Customer Service Manager', value: 'ROLE_CUSTOMER_SERVICE_MANAGER' },
  { label: 'Operation Audit Investigation Team', value: 'ROLE_AUDIT_INVESTIGATION_TEAM' },
  { label: 'Operational Audit Senior Manager', value: 'ROLE_OPERATIONAL_AUDIT_SENIOR_MANAGER' },
  { label: 'Operational Audit Director', value: 'ROLE_OPERATIONAL_AUDIT_DIRECTOR' },
  { label: 'Committee Secretary', value: 'ROLE_COMMITTEE_SECRETARY' },
  { label: 'Contact Center Agent', value: 'ROLE_CONTACT_CENTER_AGENT' },
  { label: 'Digital Marketing Officer', value: 'ROLE_DIGITAL_MARKETING_OFFICER' },
  { label: 'Work Unit Specialist', value: 'ROLE_DEPARTMENT_WORKUNIT' },
  { label: 'System Admin', value: 'ROLE_ADMIN' }
];

const RETIRED_ROLE_LABELS = {
  ROLE_SERVICE_QUALITY: 'Service Quality Officer',
  ROLE_CHIEF_COMMITTEE: 'Chief Committee',
  ROLE_BRANCH_STAFF: 'Branch Staff',
  ROLE_PENDING: 'Pending Approval'
};

function roleLabel(role) {
  const found = ROLE_OPTIONS.find(r => r.value === role);
  if (found) return found.label;
  return RETIRED_ROLE_LABELS[role] || role;
}

function assignableRoleOptions(currentRole) {
  if (currentRole && !ROLE_OPTIONS.some(r => r.value === currentRole)) {
    return [...ROLE_OPTIONS, { label: roleLabel(currentRole), value: currentRole }];
  }
  return ROLE_OPTIONS;
}

function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('approved');

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isResetPasswordModalOpen, setIsResetPasswordModalOpen] = useState(false);
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [resetForm] = Form.useForm();
  const [approveForm] = Form.useForm();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const usersData = await ApiService.getUsers().catch(err => {
        console.error('Error fetching users:', err);
        return [];
      });
      const list = Array.isArray(usersData) ? usersData : (usersData?.users || usersData?.content || []);
      setUsers(list);
    } catch (err) {
      antMessage.error('Failed to load user accounts.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (values) => {
    try {
      await ApiService.createUser(values);
      antMessage.success(`User '${values.username}' created successfully!`);
      setIsCreateModalOpen(false);
      createForm.resetFields();
      fetchData();
    } catch (err) {
      antMessage.error(err.message || 'Failed to create user');
    }
  };

  const handleEditUser = async (values) => {
    if (!selectedUser) return;
    try {
      await ApiService.updateUser(selectedUser.id, values);
      antMessage.success(`User '${selectedUser.username}' updated successfully!`);
      setIsEditModalOpen(false);
      editForm.resetFields();
      setSelectedUser(null);
      fetchData();
    } catch (err) {
      antMessage.error(err.message || 'Failed to update user');
    }
  };

  const handleApproveUser = async (values) => {
    if (!selectedUser) return;
    try {
      await ApiService.approveUser(selectedUser.id, values.role);
      antMessage.success(`User '${selectedUser.username}' approved successfully with role '${roleLabel(values.role)}'!`);
      setIsApproveModalOpen(false);
      approveForm.resetFields();
      setSelectedUser(null);
      fetchData();
    } catch (err) {
      antMessage.error(err.message || 'Failed to approve user access request');
    }
  };

  const handleRejectUser = async (record) => {
    try {
      await ApiService.rejectUser(record.id);
      antMessage.info(`User access request for '${record.username}' rejected.`);
      fetchData();
    } catch (err) {
      antMessage.error(err.message || 'Failed to reject user access request');
    }
  };

  const handleToggleStatus = async (userRecord) => {
    try {
      const response = await ApiService.toggleUserStatus(userRecord.id);
      antMessage.success(response.message || 'User status updated');
      fetchData();
    } catch (err) {
      antMessage.error('Failed to update status');
    }
  };

  const handleResetPassword = async (values) => {
    if (!selectedUser) return;
    try {
      const response = await ApiService.resetUserPassword(selectedUser.id, values.newPassword);
      antMessage.success(response.message || 'Password reset successfully');
      setIsResetPasswordModalOpen(false);
      resetForm.resetFields();
      setSelectedUser(null);
    } catch (err) {
      antMessage.error(err.message || 'Failed to reset password');
    }
  };

  const handleDeleteUser = async (id) => {
    try {
      await ApiService.deleteUser(id);
      antMessage.success('User account deleted successfully');
      fetchData();
    } catch (err) {
      antMessage.error(err.message || 'Failed to delete user');
    }
  };

  const openEditModal = (record) => {
    setSelectedUser(record);
    editForm.setFieldsValue({
      fullName: record.fullName,
      email: record.email,
      role: record.role,
      enabled: record.enabled
    });
    setIsEditModalOpen(true);
  };

  const openApproveModal = (record) => {
    setSelectedUser(record);
    approveForm.setFieldsValue({
      role: record.role !== 'ROLE_PENDING' ? record.role : undefined
    });
    setIsApproveModalOpen(true);
  };

  const openResetPasswordModal = (record) => {
    setSelectedUser(record);
    resetForm.setFieldsValue({ newPassword: 'Password@123' });
    setIsResetPasswordModalOpen(true);
  };

  // Group Users into Pending vs Approved
  const pendingUsers = users.filter(u => u.approved === false || u.approvalStatus === 'PENDING_APPROVAL' || u.role === 'ROLE_PENDING');
  const approvedUsers = users.filter(u => u.approved !== false && u.approvalStatus !== 'PENDING_APPROVAL' && u.role !== 'ROLE_PENDING');

  const activeUserList = activeTab === 'pending' ? pendingUsers : approvedUsers;

  const filteredUsers = activeUserList.filter(u => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      u.fullName?.toLowerCase().includes(q) ||
      u.username?.toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      u.role?.toLowerCase().includes(q) ||
      u.adJobTitle?.toLowerCase().includes(q) ||
      u.department?.toLowerCase().includes(q)
    );
  });

  const columnsApproved = [
    {
      title: 'Full Name / Username',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 600, color: BRAND_COLORS.primary }}>{text || record.username}</div>
          <Text type="secondary" style={{ fontSize: '12px' }}>@{record.username}</Text>
        </div>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: text => <Text copyable style={{ fontSize: '13px' }}>{text || 'N/A'}</Text>
    },
    {
      title: 'AD Job Title / Work Unit',
      key: 'jobInfo',
      render: (_, record) => (
        <div style={{ fontSize: '12px', color: '#475569' }}>
          <div><strong>Title:</strong> {record.adJobTitle || 'N/A'}</div>
          <div><strong>Unit:</strong> {record.department || 'N/A'}</div>
        </div>
      )
    },
    {
      title: 'Assigned Role',
      dataIndex: 'role',
      key: 'role',
      render: role => (
        <Tag color="geekblue" style={{ fontWeight: 500 }}>{roleLabel(role)}</Tag>
      )
    },
    {
      title: 'Source',
      dataIndex: 'authSource',
      key: 'authSource',
      render: source => <Tag>{source === 'AD' ? 'Active Directory' : 'Local'}</Tag>
    },
    {
      title: 'Status',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled, record) => (
        <Switch
          checked={enabled}
          checkedChildren="Active"
          unCheckedChildren="Inactive"
          onChange={() => handleToggleStatus(record)}
        />
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Edit User Profile">
            <Button
              type="outline"
              size="small"
              icon={<EditOutlined />}
              onClick={() => openEditModal(record)}
            />
          </Tooltip>
          <Tooltip title={record.authSource === 'AD' ? 'Password is managed in Active Directory' : 'Reset Password'}>
            <Button
              type="text"
              style={{ color: '#d97706' }}
              size="small"
              icon={<KeyOutlined />}
              disabled={record.authSource === 'AD'}
              onClick={() => openResetPasswordModal(record)}
            />
          </Tooltip>
          <Tooltip title="Delete User">
            <Popconfirm
              title="Delete User Account"
              description={`Are you sure you want to delete user "${record.username}"?`}
              onConfirm={() => handleDeleteUser(record.id)}
              okText="Yes, Delete"
              cancelText="Cancel"
              okButtonProps={{ danger: true }}
            >
              <Button
                type="text"
                danger
                size="small"
                icon={<DeleteOutlined />}
              />
            </Popconfirm>
          </Tooltip>
        </Space>
      )
    }
  ];

  const columnsPending = [
    {
      title: 'Username / Full Name',
      dataIndex: 'username',
      key: 'username',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 600, color: BRAND_COLORS.primary }}>{record.fullName || text}</div>
          <Text type="secondary" style={{ fontSize: '12px' }}>@{text}</Text>
        </div>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: text => <Text copyable style={{ fontSize: '13px' }}>{text || 'N/A'}</Text>
    },
    {
      title: 'Job Title',
      dataIndex: 'adJobTitle',
      key: 'adJobTitle',
      render: text => <Text style={{ fontSize: '13px', fontWeight: 500 }}>{text || 'N/A'}</Text>
    },
    {
      title: 'Work Unit / Department',
      dataIndex: 'department',
      key: 'department',
      render: text => <Text style={{ fontSize: '13px' }}>{text || 'N/A'}</Text>
    },
    {
      title: 'Account Status',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      render: (status) => {
        if (status === 'REJECTED') {
          return <Tag color="error" icon={<CloseCircleOutlined />}>Access Rejected</Tag>;
        }
        return <Tag color="warning" icon={<ClockCircleOutlined />}>Pending Approval</Tag>;
      }
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="primary"
            size="small"
            icon={<CheckCircleOutlined />}
            onClick={() => openApproveModal(record)}
            style={{ backgroundColor: '#16a34a', borderColor: '#16a34a', borderRadius: '6px' }}
          >
            Approve & Assign Role
          </Button>
          <Popconfirm
            title="Reject Access Request"
            description={`Are you sure you want to reject access for user "${record.username}"?`}
            onConfirm={() => handleRejectUser(record)}
            okText="Yes, Reject"
            cancelText="Cancel"
            okButtonProps={{ danger: true }}
          >
            <Button
              type="default"
              danger
              size="small"
              icon={<CloseCircleOutlined />}
            >
              Reject
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, activeTab]);

  const paginatedUsers = filteredUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <DashboardLayout userRole="admin">
      <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>
              User Management & Access Control
            </Title>
            <Text type="secondary" style={{ fontSize: '14px' }}>
              Manage Active Directory access approvals, application roles, and user account status.
            </Text>
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="large"
            onClick={() => setIsCreateModalOpen(true)}
            style={{ backgroundColor: BRAND_COLORS.primary, borderRadius: '6px' }}
          >
            Create New Local User
          </Button>
        </div>

        {/* Tab Selection Bar */}
        <Card bordered={false} style={{ borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.03)' }}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              {
                key: 'approved',
                label: (
                  <span>
                    <UserOutlined /> Active & Approved Users ({approvedUsers.length})
                  </span>
                )
              },
              {
                key: 'pending',
                label: (
                  <span>
                    <ClockCircleOutlined /> User Access Approvals{' '}
                    <Badge count={pendingUsers.length} offset={[6, -2]} style={{ backgroundColor: '#f59e0b' }} />
                  </span>
                )
              }
            ]}
          />

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px', marginTop: '12px' }}>
            <Input
              placeholder="Search users by name, username, role, job title..."
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              style={{ width: '100%', maxWidth: '360px', borderRadius: '6px' }}
              allowClear
            />
          </div>

          <Table
            columns={activeTab === 'pending' ? columnsPending : columnsApproved}
            dataSource={paginatedUsers}
            rowKey="id"
            loading={loading}
            pagination={false}
            scroll={{ x: 'max-content' }}
          />
          <Pagination
            currentPage={currentPage}
            pageSize={pageSize}
            totalRecords={filteredUsers.length}
            onPageChange={(page) => setCurrentPage(page)}
            onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }}
            itemUnit="users"
          />
        </Card>

        {/* Create User Modal */}
        <Modal
          title={<span style={{ color: BRAND_COLORS.primary, fontWeight: 'bold' }}><UserOutlined /> Create New User Account</span>}
          open={isCreateModalOpen}
          onCancel={() => { setIsCreateModalOpen(false); createForm.resetFields(); }}
          onOk={() => createForm.submit()}
          okText="Create User"
          width="100%"
          style={{ maxWidth: 650 }}
        >
          <Form form={createForm} layout="vertical" onFinish={handleCreateUser}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="fullName" label="Full Name" rules={[{ required: true, message: 'Please enter full name' }]}>
                  <Input placeholder="e.g. Abebe Kebede" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="username" label="Username" rules={[{ required: true, message: 'Please enter username' }]}>
                  <Input placeholder="e.g. abebe_kebede" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="email" label="Email Address" rules={[{ required: true, type: 'email', message: 'Valid email required' }]}>
                  <Input placeholder="e.g. abebe@dashenbank.com" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="password"
                  label="Initial Password"
                  rules={[
                    { required: true, message: 'Please enter initial password' },
                    {
                      validator: (_, value) => {
                        if (!value || PASSWORD_POLICY.test(value)) return Promise.resolve();
                        return Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE));
                      }
                    }
                  ]}
                  initialValue="Password@123"
                >
                  <Input.Password placeholder="Password" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item name="role" label="Assign System Role" rules={[{ required: true, message: 'Please select role' }]}>
              <Select placeholder="Select role">
                {assignableRoleOptions().map(r => (
                  <Option key={r.value} value={r.value}>{r.label}</Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>

        {/* Edit User Modal */}
        <Modal
          title={<span style={{ color: BRAND_COLORS.primary, fontWeight: 'bold' }}><EditOutlined /> Edit User Profile</span>}
          open={isEditModalOpen}
          onCancel={() => { setIsEditModalOpen(false); editForm.resetFields(); setSelectedUser(null); }}
          onOk={() => editForm.submit()}
          okText="Save Changes"
          width="100%"
          style={{ maxWidth: 650 }}
        >
          <Form form={editForm} layout="vertical" onFinish={handleEditUser}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="fullName" label="Full Name" rules={[{ required: true }]}>
                  <Input />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="email" label="Email Address" rules={[{ required: true, type: 'email' }]}>
                  <Input />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item name="role" label="Assign System Role" rules={[{ required: true }]}>
              <Select>
                {assignableRoleOptions(selectedUser?.role).map(r => (
                  <Option key={r.value} value={r.value}>{r.label}</Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>

        {/* Approve Access & Assign Role Modal */}
        <Modal
          title={<span style={{ color: '#16a34a', fontWeight: 'bold' }}><CheckCircleOutlined /> Approve User Access & Assign Role</span>}
          open={isApproveModalOpen}
          onCancel={() => { setIsApproveModalOpen(false); approveForm.resetFields(); setSelectedUser(null); }}
          onOk={() => approveForm.submit()}
          okText="Approve & Grant Access"
          okButtonProps={{ style: { backgroundColor: '#16a34a', borderColor: '#16a34a' } }}
          width="100%"
          style={{ maxWidth: 600 }}
        >
          <Alert
            message={`Approving access request for: ${selectedUser?.fullName || selectedUser?.username}`}
            description={
              <div style={{ fontSize: '13px', marginTop: '6px' }}>
                <div><strong>Username:</strong> @{selectedUser?.username}</div>
                <div><strong>Email:</strong> {selectedUser?.email || 'N/A'}</div>
                <div><strong>AD Job Title:</strong> {selectedUser?.adJobTitle || 'N/A'}</div>
                <div><strong>Work Unit:</strong> {selectedUser?.department || 'N/A'}</div>
              </div>
            }
            type="info"
            showIcon
            style={{ marginBottom: '20px' }}
          />
          <Form form={approveForm} layout="vertical" onFinish={handleApproveUser}>
            <Form.Item
              name="role"
              label="Select Application Role to Authorize"
              rules={[{ required: true, message: 'Please select an application role to authorize this user.' }]}
            >
              <Select placeholder="Select application role">
                {ROLE_OPTIONS.map(r => (
                  <Option key={r.value} value={r.value}>{r.label}</Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>

        {/* Reset Password Modal */}
        <Modal
          title={<span style={{ color: '#d97706', fontWeight: 'bold' }}><KeyOutlined /> Reset Password</span>}
          open={isResetPasswordModalOpen}
          onCancel={() => { setIsResetPasswordModalOpen(false); resetForm.resetFields(); setSelectedUser(null); }}
          onOk={() => resetForm.submit()}
          okText="Reset Password"
        >
          <Alert message={`Resetting password for user: ${selectedUser?.username}`} type="warning" showIcon style={{ marginBottom: '16px' }} />
          <Form form={resetForm} layout="vertical" onFinish={handleResetPassword}>
            <Form.Item
              name="newPassword"
              label="New Password"
              rules={[
                { required: true, message: 'Please enter new password' },
                {
                  validator: (_, value) => {
                    if (!value || PASSWORD_POLICY.test(value)) return Promise.resolve();
                    return Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE));
                  }
                }
              ]}
            >
              <Input.Password placeholder="Enter new password" />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}

export default UserManagementPage;
