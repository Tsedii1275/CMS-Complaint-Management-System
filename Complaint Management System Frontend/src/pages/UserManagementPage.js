import React, { useState, useEffect } from 'react';
import { Table, Card, Typography, Button, Tag, Modal, Form, Input, Select, Switch, Space, Row, Col, Alert, Tooltip, Popconfirm, message as antMessage } from 'antd';
import { UserOutlined, PlusOutlined, EditOutlined, KeyOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons';
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
  ROLE_BRANCH_STAFF: 'Branch Staff'
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
  const [hierarchy, setHierarchy] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isResetPasswordModalOpen, setIsResetPasswordModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [resetForm] = Form.useForm();

  // Dynamic dropdown selections for creation
  const [selectedDistrict, setSelectedDistrict] = useState('');
  const [selectedBranch, setSelectedBranch] = useState('');

  // Dynamic dropdown selections for edit
  const [editDistrict, setEditDistrict] = useState('');
  const [editBranch, setEditBranch] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [usersData, hierarchyData] = await Promise.all([
        ApiService.getUsers().catch(err => {
          console.error('Error fetching users:', err);
          return [];
        }),
        ApiService.getHierarchy().catch(err => {
          console.error('Error fetching hierarchy:', err);
          return [];
        })
      ]);
      const list = Array.isArray(usersData) ? usersData : (usersData?.users || usersData?.content || []);
      setUsers(list);
      setHierarchy(Array.isArray(hierarchyData) ? hierarchyData : []);
    } catch (err) {
      antMessage.error('Failed to load user accounts or organizational hierarchy.');
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
      setSelectedDistrict('');
      setSelectedBranch('');
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
    setEditDistrict(record.district || '');
    setEditBranch(record.branch || '');
    editForm.setFieldsValue({
      fullName: record.fullName,
      email: record.email,
      role: record.role,
      district: record.district,
      branch: record.branch,
      department: record.department,
      enabled: record.enabled
    });
    setIsEditModalOpen(true);
  };

  const openResetPasswordModal = (record) => {
    setSelectedUser(record);
    resetForm.setFieldsValue({ newPassword: 'Password@123' });
    setIsResetPasswordModalOpen(true);
  };

  // Helper arrays for hierarchy
  const availableBranchesForDistrict = (distName) => {
    const distObj = hierarchy.find(d => d.name === distName);
    return distObj ? distObj.branches : [];
  };

  const availableDepartmentsForBranch = (distName, branchName) => {
    const branches = availableBranchesForDistrict(distName);
    const branchObj = branches.find(b => b.name === branchName);
    return (branchObj ? branchObj.departments : []).filter(
      dept => !/service\s*quality/i.test(dept.name || '')
    );
  };

  const filteredUsers = users.filter(u => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      u.fullName?.toLowerCase().includes(q) ||
      u.username?.toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      u.role?.toLowerCase().includes(q) ||
      u.branch?.toLowerCase().includes(q) ||
      u.department?.toLowerCase().includes(q)
    );
  });

  const columns = [
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
      render: text => <Text copyable style={{ fontSize: '13px' }}>{text}</Text>
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
      title: 'District / Branch / Department',
      key: 'location',
      render: (_, record) => (
        <div style={{ fontSize: '12px', color: '#475569' }}>
          <div><strong>District:</strong> {record.district || 'Enterprise Wide'}</div>
          <div><strong>Branch:</strong> {record.branch || 'N/A'}</div>
          <div><strong>Department:</strong> {record.department || 'N/A'}</div>
        </div>
      )
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
          <Tooltip title="Reset Password">
            <Button
              type="text"
              style={{ color: '#d97706' }}
              size="small"
              icon={<KeyOutlined />}
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

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery]);

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
              Manage system users, roles, organizational mapping (District, Branch, Department), and account status.
            </Text>
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="large"
            onClick={() => setIsCreateModalOpen(true)}
            style={{ backgroundColor: BRAND_COLORS.primary, borderRadius: '6px' }}
          >
            Create New User
          </Button>
        </div>

        <Card bordered={false} style={{ borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.03)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
            <Input
              placeholder="Search users by name, username, role, branch, department..."
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              style={{ width: '100%', maxWidth: '360px', borderRadius: '6px' }}
              allowClear
            />
          </div>

          <Table
            columns={columns}
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

            <Title level={5} style={{ marginTop: '12px', color: BRAND_COLORS.primary }}>Organizational Hierarchy Mapping</Title>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="district" label="District">
                  <Select
                    placeholder="Select District"
                    allowClear
                    onChange={val => {
                      setSelectedDistrict(val);
                      createForm.setFieldsValue({ branch: undefined, department: undefined });
                      setSelectedBranch('');
                    }}
                  >
                    {hierarchy.map(d => (
                      <Option key={d.name} value={d.name}>{d.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="branch" label="Branch">
                  <Select
                    placeholder="Select Branch"
                    allowClear
                    disabled={!selectedDistrict}
                    onChange={val => {
                      setSelectedBranch(val);
                      createForm.setFieldsValue({ department: undefined });
                    }}
                  >
                    {availableBranchesForDistrict(selectedDistrict).map(b => (
                      <Option key={b.name} value={b.name}>{b.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="department" label="Department">
                  <Select
                    placeholder="Select Department"
                    allowClear
                    disabled={!selectedBranch}
                  >
                    {availableDepartmentsForBranch(selectedDistrict, selectedBranch).map(dept => (
                      <Option key={dept.name} value={dept.name}>{dept.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
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

            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="district" label="District">
                  <Select
                    placeholder="Select District"
                    allowClear
                    onChange={val => {
                      setEditDistrict(val);
                      editForm.setFieldsValue({ branch: undefined, department: undefined });
                      setEditBranch('');
                    }}
                  >
                    {hierarchy.map(d => (
                      <Option key={d.name} value={d.name}>{d.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="branch" label="Branch">
                  <Select
                    placeholder="Select Branch"
                    allowClear
                    onChange={val => {
                      setEditBranch(val);
                      editForm.setFieldsValue({ department: undefined });
                    }}
                  >
                    {availableBranchesForDistrict(editDistrict).map(b => (
                      <Option key={b.name} value={b.name}>{b.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="department" label="Department">
                  <Select placeholder="Select Department" allowClear>
                    {availableDepartmentsForBranch(editDistrict, editBranch).map(dept => (
                      <Option key={dept.name} value={dept.name}>{dept.name}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
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
