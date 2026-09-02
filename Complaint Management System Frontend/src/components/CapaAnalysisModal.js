import React, { useEffect, useState } from 'react';
import { Alert, Button, Col, Form, Input, Modal, Row, Space, Table, Tag, Typography, message } from 'antd';
import ExportDropdown from './ExportDropdown';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

const WHY_FIELDS = [
  { name: 'why1', label: 'Why 1', prompt: 'Why did this problem occur?' },
  { name: 'why2', label: 'Why 2', prompt: 'Why did that cause occur?' },
  { name: 'why3', label: 'Why 3', prompt: 'Why did that cause occur?' },
  { name: 'why4', label: 'Why 4', prompt: 'Why did that cause occur?' },
  { name: 'why5', label: 'Why 5', prompt: 'Why did that cause occur?' }
];

const sectionStyle = {
  marginBottom: 20,
  padding: 16,
  border: '1px solid #e2e8f0',
  borderRadius: 8,
  background: '#fff'
};

function hasSavedCapa(data) {
  return Boolean(data?.updatedAt);
}

function formValuesFromDetail(data) {
  return {
    problemStatement: data?.problemStatement || data?.nature,
    why1: data?.why1,
    why2: data?.why2,
    why3: data?.why3,
    why4: data?.why4,
    why5: data?.why5,
    rootCause: data?.rootCause,
    correctiveAction: data?.corrective?.action,
    correctiveDepartment: data?.corrective?.responsibleDepartment,
    correctiveOfficer: data?.corrective?.responsibleOfficer,
    preventiveAction: data?.preventive?.action,
    preventiveDepartment: data?.preventive?.responsibleDepartment,
    preventiveOfficer: data?.preventive?.responsibleOfficer
  };
}

function CapaAnalysisModal({ open, nature, onClose }) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [editing, setEditing] = useState(true);

  useEffect(() => {
    if (!open || !nature) {
      return undefined;
    }
    let cancelled = false;
    const load = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await ApiService.getNatureCapa(nature);
        if (cancelled) {
          return;
        }
        setDetail(data);
        form.setFieldsValue(formValuesFromDetail(data));
        setEditing(!hasSavedCapa(data));
      } catch (err) {
        console.error(err);
        if (!cancelled) {
          setError('Failed to load CAPA analysis for this complaint nature.');
          setEditing(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [open, nature, form]);

  const saved = hasSavedCapa(detail);
  const readOnly = loading || (saved && !editing);

  const startEdit = () => {
    form.setFieldsValue(formValuesFromDetail(detail));
    setEditing(true);
  };

  const cancelEdit = () => {
    form.setFieldsValue(formValuesFromDetail(detail));
    setEditing(false);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      setSaving(true);
      const savedRecord = await ApiService.saveNatureCapa({
        nature: detail?.nature || nature,
        problemStatement: values.problemStatement,
        why1: values.why1,
        why2: values.why2,
        why3: values.why3,
        why4: values.why4,
        why5: values.why5,
        rootCause: values.rootCause,
        corrective: {
          action: values.correctiveAction,
          responsibleDepartment: values.correctiveDepartment,
          responsibleOfficer: values.correctiveOfficer
        },
        preventive: {
          action: values.preventiveAction,
          responsibleDepartment: values.preventiveDepartment,
          responsibleOfficer: values.preventiveOfficer
        }
      });
      setDetail(savedRecord);
      form.setFieldsValue(formValuesFromDetail(savedRecord));
      setEditing(false);
      message.success(saved ? 'CAPA analysis updated.' : 'CAPA analysis saved.');
    } catch (err) {
      console.error(err);
      message.error(err?.message || 'Failed to save CAPA analysis.');
    } finally {
      setSaving(false);
    }
  };

  const categories = detail?.complaintCategories || [];
  const descriptions = detail?.complaintDescriptions || [];

  const capaExportDataset = () => {
    const values = form.getFieldsValue();
    const row = {
      nature: detail?.nature || nature,
      complaintCount: detail?.complaintCount ?? '',
      complaintCategories: categories.join('; '),
      problemStatement: values.problemStatement || '',
      why1: values.why1 || '',
      why2: values.why2 || '',
      why3: values.why3 || '',
      why4: values.why4 || '',
      why5: values.why5 || '',
      rootCause: values.rootCause || '',
      correctiveAction: values.correctiveAction || '',
      preventiveAction: values.preventiveAction || '',
      responsibleDepartment: [
        values.correctiveDepartment ? `Corrective: ${values.correctiveDepartment}` : null,
        values.preventiveDepartment ? `Preventive: ${values.preventiveDepartment}` : null
      ].filter(Boolean).join(' | '),
      responsibleOfficer: [
        values.correctiveOfficer ? `Corrective: ${values.correctiveOfficer}` : null,
        values.preventiveOfficer ? `Preventive: ${values.preventiveOfficer}` : null
      ].filter(Boolean).join(' | ')
    };
    return {
      filename: `CAPA_Analysis_${String(row.nature || 'nature').replace(/\s+/g, '_')}_${new Date().toISOString().slice(0, 10)}`,
      documentTitle: 'CAPA Analysis',
      subtitle: `Nature of Complaint: ${row.nature}`,
      tables: [
        {
          title: 'CAPA Analysis',
          sheetName: 'CAPA',
          columns: [
            { header: 'Complaint Nature', key: 'nature' },
            { header: 'Complaint Count', key: 'complaintCount' },
            { header: 'Complaint Categories', key: 'complaintCategories' },
            { header: 'Problem Statement', key: 'problemStatement' },
            { header: 'Why 1', key: 'why1' },
            { header: 'Why 2', key: 'why2' },
            { header: 'Why 3', key: 'why3' },
            { header: 'Why 4', key: 'why4' },
            { header: 'Why 5', key: 'why5' },
            { header: 'Root Cause', key: 'rootCause' },
            { header: 'Corrective Action', key: 'correctiveAction' },
            { header: 'Preventive Action', key: 'preventiveAction' },
            { header: 'Responsible Department/Team', key: 'responsibleDepartment' },
            { header: 'Responsible Officer', key: 'responsibleOfficer' }
          ],
          rows: [row]
        },
        {
          title: 'Corrective and Preventive Detail',
          sheetName: 'Actions',
          columns: [
            { header: 'Type', key: 'type' },
            { header: 'Action', key: 'action' },
            { header: 'Responsible Department/Team', key: 'department' },
            { header: 'Responsible Officer', key: 'officer' }
          ],
          rows: [
            {
              type: 'Corrective',
              action: values.correctiveAction || '',
              department: values.correctiveDepartment || '',
              officer: values.correctiveOfficer || ''
            },
            {
              type: 'Preventive',
              action: values.preventiveAction || '',
              department: values.preventiveDepartment || '',
              officer: values.preventiveOfficer || ''
            }
          ]
        }
      ]
    };
  };

  return (
    <Modal
      title={(
        <span style={{ fontSize: 18, fontWeight: 700, color: BRAND_COLORS.primary }}>
          CAPA Analysis — {detail?.nature || nature || ''}
        </span>
      )}
      open={open}
      onCancel={onClose}
      width="100%"
      style={{ maxWidth: 980, top: 16 }}
      styles={{ body: { padding: 24, maxHeight: '80vh', overflowY: 'auto' } }}
      footer={(
        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <ExportDropdown buttonText="Export CAPA" getDataset={capaExportDataset} disabled={!detail} />
          <Space wrap>
            <Button onClick={onClose}>Close</Button>
            {saved && editing ? (
              <Button onClick={cancelEdit} disabled={saving}>Cancel</Button>
            ) : null}
            {saved && !editing ? (
              <Button type="primary" onClick={startEdit} style={{ background: BRAND_COLORS.primary }}>
                Edit CAPA
              </Button>
            ) : (
              <Button type="primary" loading={saving} onClick={handleSave} style={{ background: BRAND_COLORS.primary }}>
                {saved ? 'Update CAPA' : 'Save CAPA'}
              </Button>
            )}
          </Space>
        </div>
      )}
      destroyOnClose
    >
      {error ? <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} /> : null}

      <Form form={form} layout="vertical" disabled={readOnly}>
        <div style={sectionStyle}>
          <Text strong style={{ display: 'block', marginBottom: 8, color: BRAND_COLORS.primary }}>Complaint Nature</Text>
          <Paragraph style={{ marginBottom: 4 }}>
            <Text type="secondary">Nature of Complaint: </Text>
            <Text strong>{detail?.nature || nature || '—'}</Text>
          </Paragraph>
          <Paragraph style={{ marginBottom: 8 }}>
            <Text type="secondary">Total complaints: </Text>
            <Text strong>{detail?.complaintCount ?? '—'}</Text>
            {detail?.percentage != null ? <Text type="secondary"> ({detail.percentage}% of classified complaints)</Text> : null}
          </Paragraph>
          <Text type="secondary">Complaint categories</Text>
          <div style={{ marginTop: 6, marginBottom: 12 }}>
            {categories.length === 0
              ? <Text>—</Text>
              : categories.map(item => <Tag key={item} color="blue">{item}</Tag>)}
          </div>
          <Text type="secondary">Relevant complaint descriptions</Text>
          <Table
            style={{ marginTop: 8 }}
            size="small"
            pagination={false}
            rowKey={(row, index) => row.uniqueIdNo || `${index}`}
            dataSource={descriptions}
            locale={{ emptyText: 'No complaint descriptions for this nature.' }}
            columns={[
              { title: 'Unique ID', dataIndex: 'uniqueIdNo', key: 'uniqueIdNo', width: 160 },
              { title: 'Category', dataIndex: 'category', key: 'category', width: 140 },
              { title: 'Description', dataIndex: 'description', key: 'description' }
            ]}
          />
        </div>

        <div style={sectionStyle}>
          <Text strong style={{ display: 'block', marginBottom: 12, color: BRAND_COLORS.primary }}>5 Why Root Cause Analysis</Text>
          <Form.Item label="Problem Statement" name="problemStatement">
            <Input readOnly />
          </Form.Item>
          {WHY_FIELDS.map(item => (
            <Form.Item key={item.name} label={`${item.label}: ${item.prompt}`} name={item.name}>
              <TextArea rows={2} placeholder={item.prompt} />
            </Form.Item>
          ))}
          <Form.Item label="Root Cause Identified" name="rootCause">
            <TextArea rows={3} placeholder="Root Cause Identified" />
          </Form.Item>
        </div>

        <div style={sectionStyle}>
          <Text strong style={{ display: 'block', marginBottom: 4, color: BRAND_COLORS.primary }}>Corrective Action</Text>
          <Paragraph type="secondary">What action will correct the existing problem?</Paragraph>
          <Form.Item label="Corrective Action" name="correctiveAction">
            <TextArea rows={3} />
          </Form.Item>
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item label="Responsible Department/Team" name="correctiveDepartment">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Responsible Officer" name="correctiveOfficer">
                <Input />
              </Form.Item>
            </Col>
          </Row>
        </div>

        <div style={sectionStyle}>
          <Text strong style={{ display: 'block', marginBottom: 4, color: BRAND_COLORS.primary }}>Preventive Action</Text>
          <Paragraph type="secondary">What action will prevent the problem from happening again?</Paragraph>
          <Form.Item label="Preventive Action" name="preventiveAction">
            <TextArea rows={3} />
          </Form.Item>
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item label="Responsible Department/Team" name="preventiveDepartment">
                <Input />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Responsible Officer" name="preventiveOfficer">
                <Input />
              </Form.Item>
            </Col>
          </Row>
        </div>
      </Form>
    </Modal>
  );
}

export default CapaAnalysisModal;
