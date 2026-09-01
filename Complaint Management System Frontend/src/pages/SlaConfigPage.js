import React, { useState, useEffect } from 'react';
import { Card, Typography, Table, Tag, Button, Row, Col, Tabs, Modal, Input, message, Form, DatePicker, Popconfirm, Select, Tooltip } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, InfoCircleOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function SlaConfigPage() {
  const [slaConfigs, setSlaConfigs] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [operatingHours, setOperatingHours] = useState([]);
  const [loading, setLoading] = useState(true);

  const [editingConfig, setEditingConfig] = useState(null);
  const [newAllowedMins, setNewAllowedMins] = useState('');

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Add holiday modal state
  const [isAddHolidayModalOpen, setIsAddHolidayModalOpen] = useState(false);
  const [holidayForm] = Form.useForm();

  useEffect(() => {
    loadAllConfigs();
  }, []);

  const loadAllConfigs = async () => {
    try {
      setLoading(true);
      const [cfgs, hols, hours] = await Promise.all([
        ApiService.getSlaConfigs().catch(() => []),
        ApiService.getHolidays().catch(() => []),
        ApiService.getOperatingHours().catch(() => [])
      ]);
      setSlaConfigs(cfgs);
      setHolidays(hols);
      setOperatingHours(hours || []);
    } catch (err) {
      console.error('Failed to load SLA configs:', err);
      message.error('Failed to load SLA configurations.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetDefaults = async () => {
    if (window.confirm('Are you sure you want to reset all SLA configurations to Dashen Bank institutional baseline defaults?')) {
      try {
        await ApiService.resetSlaConfigs();
        message.success('SLA Configurations successfully reset to Dashen Bank defaults.');
        await loadAllConfigs();
      } catch (err) {
        console.error(err);
        message.error('Failed to reset SLA configurations.');
      }
    }
  };

  const handleSaveSlaConfig = async () => {
    if (!editingConfig) return;
    try {
      const mins = Number.parseInt(newAllowedMins, 10);
      if (Number.isNaN(mins) || mins <= 0) {
        message.error('Please enter a valid positive number of business minutes.');
        return;
      }
      await ApiService.updateSlaConfig(editingConfig.id, { allowedMinutes: mins });
      message.success(`SLA for '${editingConfig.displayName}' updated to ${mins} business minutes.`);
      setEditingConfig(null);
      await loadAllConfigs();
    } catch (err) {
      console.error(err);
      message.error('Failed to update SLA configuration.');
    }
  };

  const handleAddHoliday = async (values) => {
    try {
      const holidayDateStr = values.holidayDate.format('YYYY-MM-DD');
      const payload = {
        holidayName: values.holidayName,
        holidayDate: holidayDateStr,
        holidayType: values.holidayType || 'NATIONAL_HOLIDAY'
      };
      await ApiService.addHoliday(payload);
      message.success(`Official Bank Holiday '${values.holidayName}' added successfully.`);
      setIsAddHolidayModalOpen(false);
      holidayForm.resetFields();
      await loadAllConfigs();
    } catch (err) {
      console.error(err);
      message.error('Failed to add holiday to calendar.');
    }
  };

  const handleDeleteHoliday = async (id) => {
    try {
      await ApiService.deleteHoliday(id);
      message.success('Holiday removed from official calendar.');
      await loadAllConfigs();
    } catch (err) {
      console.error(err);
      message.error('Failed to remove holiday.');
    }
  };

  const GROUP_DISPLAY_MAP = {
    'INTAKE_SLA': { label: 'Intake & Triage SLA', color: 'cyan' },
    'STAGE_SLA': { label: 'Workflow Stage SLA', color: 'blue' },
    'WORKUNIT_RESOLUTION': { label: 'Work Unit Resolution SLA', color: 'geekblue' },
    'CUSTOMER_NOTIFICATION': { label: 'Final Dispatch SLA', color: 'gold' },
    'OVERALL_CASE_SLA': { label: 'Total Lifecycle SLA', color: 'purple' },
    'OVERALL_SLA': { label: 'Total Lifecycle SLA', color: 'purple' },
    'ESCALATION_SLA': { label: 'Executive Escalation SLA', color: 'volcano' }
  };

  const renderPriorityBadge = (p) => {
    if (p === 'HIGHLY_SENSITIVE' || p === 'HIGH_SENSITIVE' || (p || '').includes('HIGH')) {
      return <Tag color="red" style={{ fontWeight: 600 }}>High Sensitive</Tag>;
    }
    if (p === 'SENSITIVE' || (p || '').includes('SENSITIVE')) {
      return <Tag color="orange" style={{ fontWeight: 600 }}>Sensitive</Tag>;
    }
    return <Tag color="green" style={{ fontWeight: 600 }}>Normal</Tag>;
  };

  const formatOperatingTime = (mins) => {
    if (mins < 60) {
      return `${mins} Minutes`;
    }
    if (mins < 480) {
      return `${(mins / 60).toFixed(1)} Hours`;
    }
    return `${(mins / 480).toFixed(1)} Business Days (8h/day)`;
  };

  return (
    <DashboardLayout userRole="admin">
      <div style={{ padding: '24px', maxWidth: '1240px', margin: '0 auto' }}>

        {/* Title Header Bar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
          <div>
            <Title level={2} style={{ color: BRAND_COLORS.primary, margin: 0, fontWeight: 700 }}>
              SLA Governance & Policy Configuration
            </Title>
            <Text type="secondary" style={{ fontSize: '14px' }}>
              Institutional Service Level Agreement (SLA) policy administration, resolution matrix, multi-level escalation thresholds, and banking calendar management.
            </Text>
          </div>
          <div>
            <Button danger onClick={handleResetDefaults}>Reset to Policy Defaults</Button>
          </div>
        </div>

        <Tabs defaultActiveKey="matrix">

          {/* Tab 1: Stage & Priority SLA Matrix */}
          <Tabs.TabPane tab={`SLA Policy Matrix (${slaConfigs.length})`} key="matrix">
            <Card title="Dashen Bank Official Complaint Resolution SLA Matrix">
              <Table
                dataSource={slaConfigs.slice((currentPage - 1) * pageSize, currentPage * pageSize)}
                rowKey="id"
                loading={loading}
                pagination={false}
                columns={[
                  {
                    title: 'Governance Rule Name',
                    dataIndex: 'displayName',
                    width: 320,
                    render: (t, r) => (
                      <div>
                        <strong style={{ color: BRAND_COLORS.primary, fontSize: '14px' }}>{t}</strong>
                        <div style={{ fontSize: '11px', color: '#8c8c8c', fontFamily: 'monospace', marginTop: '2px' }}>{r.configKey}</div>
                        {r.description && <div style={{ fontSize: '12px', color: '#555', marginTop: '4px', lineHeight: 1.4 }}>{r.description}</div>}
                      </div>
                    )
                  },
                  {
                    title: 'Complaint Classification',
                    dataIndex: 'priority',
                    width: 170,
                    render: renderPriorityBadge
                  },
                  {
                    title: (
                      <span>
                        Allowed Operating Time{' '}
                        <Tooltip title="Business operating time calculated based on Dashen Bank 8-hour workday schedules (excluding Sundays & Bank Holidays).">
                          <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                        </Tooltip>
                      </span>
                    ),
                    dataIndex: 'allowedMinutes',
                    width: 200,
                    render: mins => (
                      <div>
                        <strong style={{ fontSize: '14px', color: '#111827' }}>{mins} Mins</strong>
                        <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '2px' }}>
                          {formatOperatingTime(mins)}
                        </div>
                      </div>
                    )
                  },
                  {
                    title: 'Policy Action',
                    key: 'action',
                    width: 120,
                    render: (_, r) => (
                      <Button
                        type="primary"
                        icon={<EditOutlined />}
                        size="small"
                        onClick={() => {
                          setEditingConfig(r);
                          setNewAllowedMins(r.allowedMinutes);
                        }}
                      >
                        Modify SLA
                      </Button>
                    )
                  }
                ]}
              />
              <Pagination
                currentPage={currentPage}
                pageSize={pageSize}
                totalRecords={slaConfigs.length}
                onPageChange={(page) => setCurrentPage(page)}
                onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }}
                itemUnit="governance rules"
              />
            </Card>
          </Tabs.TabPane>

          {/* Tab 2: Automated Alerts & Escalation Policy */}
          <Tabs.TabPane tab="Automated Alerts & Escalations" key="escalations">
            <Row gutter={[16, 16]}>
              <Col xs={24}>
                <Card title="SLA Breach Alert Thresholds & Notification Rules">
                  <Table
                    dataSource={[
                      { level: 'Reminder Level 1 (Approaching SLA)', threshold: '80% Business Time Elapsed', action: 'Automated notification & Dashboard SLA Warning indicator to assigned officer.' },
                      { level: 'Reminder Level 2 (SLA Breach)', threshold: '100% Business Time Elapsed', action: 'Automated breach alert dispatched to assigned Work Unit.' }
                    ]}
                    rowKey="level"
                    pagination={false}
                    columns={[
                      { title: 'Alert Level', dataIndex: 'level', render: l => <span style={{ fontWeight: 600, color: '#1e293b', fontSize: '13px' }}>{l}</span> },
                      { title: 'Trigger Threshold', dataIndex: 'threshold', render: t => <Tag color="default" style={{ fontWeight: 600, fontSize: '11px' }}>{t}</Tag> },
                      { title: 'System Notification Action', dataIndex: 'action', render: a => <span style={{ color: '#475569', fontSize: '12px', lineHeight: 1.4 }}>{a}</span> }
                    ]}
                  />
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>

          {/* Tab 3: Operating Schedule & Public Holidays */}
          <Tabs.TabPane tab="Operating Hours & Holiday Calendar" key="business_hours">
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={11}>
                <Card title="Dashen Bank Standard Operating Schedule">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '16px' }}>
                    {(operatingHours || []).map((row) => (
                      <div key={row.days} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#ffffff', padding: '10px 14px', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
                        <div>
                          <strong style={{ fontSize: '13px', color: '#1e293b' }}>{row.days}</strong>
                          <div style={{ fontSize: '12px', color: '#64748b' }}>{row.windows}</div>
                        </div>
                        <Tag color={row.closed ? 'default' : 'processing'} style={{ fontWeight: 600, fontSize: '11px', margin: 0 }}>
                          {row.closed ? 'Closed' : `${Number(row.hoursPerDay).toFixed(1)} Hours / Day`}
                        </Tag>
                      </div>
                    ))}
                  </div>
                </Card>
              </Col>

              <Col xs={24} lg={13}>
                <Card
                  title="Official Bank & Public Holidays Calendar"
                  extra={
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsAddHolidayModalOpen(true)}>
                      Register Bank Holiday
                    </Button>
                  }
                >
                  <Table
                    dataSource={holidays}
                    rowKey="id"
                    pagination={{
                      pageSize: 10,
                      showSizeChanger: true,
                      pageSizeOptions: [10, 25, 50, 100],
                      showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} holidays`
                    }}
                    columns={[
                      { title: 'Holiday Title', dataIndex: 'holidayName', render: n => <span style={{ fontWeight: 600, color: '#1e293b', fontSize: '13px' }}>{n}</span> },
                      { title: 'Official Date', dataIndex: 'holidayDate', render: d => <span style={{ fontFamily: 'monospace', fontSize: '12px', color: '#475569', fontWeight: 600 }}>{d}</span> },
                      { title: 'Category', dataIndex: 'holidayType', render: t => <Tag color="default" style={{ fontWeight: 500, fontSize: '11px' }}>{t || 'Public Holiday'}</Tag> },
                      {
                        title: 'Action',
                        key: 'act',
                        width: 70,
                        align: 'center',
                        render: (_, r) => (
                          <Popconfirm title="Remove holiday entry?" onConfirm={() => handleDeleteHoliday(r.id)}>
                            <Button danger size="small" icon={<DeleteOutlined />} />
                          </Popconfirm>
                        )
                      }
                    ]}
                  />
                </Card>
              </Col>
            </Row>
          </Tabs.TabPane>

        </Tabs>

        {/* Edit Single SLA Modal */}
        <Modal
          title={`Modify Governance Rule: ${editingConfig?.displayName || ''}`}
          open={!!editingConfig}
          onCancel={() => setEditingConfig(null)}
          onOk={handleSaveSlaConfig}
          okText="Save Policy Changes"
        >
          {editingConfig && (
            <div>
              <p><strong>System Key:</strong> <Text code>{editingConfig.configKey}</Text></p>
              <p>
                <strong>Governance Group:</strong>{' '}
                <Tag color={GROUP_DISPLAY_MAP[editingConfig.configGroup]?.color || 'default'}>
                  {GROUP_DISPLAY_MAP[editingConfig.configGroup]?.label || editingConfig.configGroup}
                </Tag>
              </p>
              <p><strong>Priority Tier:</strong> <Tag color="blue">{editingConfig.priority || 'GENERAL'}</Tag></p>
              <div style={{ marginTop: '16px' }}>
                <Text strong>Allowed Business Operating Minutes:</Text>
                <Input
                  type="number"
                  value={newAllowedMins}
                  onChange={e => setNewAllowedMins(e.target.value)}
                  style={{ marginTop: '8px' }}
                />
                <Text type="secondary" style={{ fontSize: '12px', marginTop: '6px', display: 'block' }}>
                  Reference Guide: 5 mins (Contact Center), 30 mins (Intake), 240 mins (4 Hours), 480 mins (1 Operating Day), 1,920 mins (4 Days), 13,440 mins (28 Days).
                </Text>
              </div>
            </div>
          )}
        </Modal>

        {/* Add Holiday Modal */}
        <Modal
          title="Register Official Bank / Public Holiday"
          open={isAddHolidayModalOpen}
          onCancel={() => setIsAddHolidayModalOpen(false)}
          onOk={() => holidayForm.submit()}
          okText="Register Holiday"
        >
          <Form form={holidayForm} layout="vertical" onFinish={handleAddHoliday}>
            <Form.Item name="holidayName" label="Holiday Title" rules={[{ required: true, message: 'Please enter official holiday title' }]}>
              <Input placeholder="e.g. Ethiopian New Year / Enkutatash" />
            </Form.Item>
            <Form.Item name="holidayDate" label="Official Date" rules={[{ required: true, message: 'Please select holiday date' }]}>
              <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
            </Form.Item>
            <Form.Item name="holidayType" label="Holiday Category" initialValue="NATIONAL_HOLIDAY">
              <Select>
                <Select.Option value="NATIONAL_HOLIDAY">National Public Holiday</Select.Option>
                <Select.Option value="RELIGIOUS_HOLIDAY">Religious Holiday</Select.Option>
                <Select.Option value="BANK_HOLIDAY">Bank Operating Holiday</Select.Option>
              </Select>
            </Form.Item>
          </Form>
        </Modal>

      </div>
    </DashboardLayout>
  );
}

export default SlaConfigPage;

