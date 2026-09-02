import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Modal, Row, Space, Statistic, Table, Tooltip, Typography } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import ExportDropdown from '../components/ExportDropdown';
import CapaAnalysisModal from '../components/CapaAnalysisModal';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import { renderComplaintStatusTag } from '../utils/statusUtils';

const { Title, Text } = Typography;

const CATEGORY_COLORS = [
  '#012169',
  '#FFCB70',
  '#0f766e',
  '#2563eb',
  '#dc2626',
  '#7c3aed',
  '#d97706',
  '#059669',
  '#0891b2',
  '#be185d'
];

function hasDisplayValue(value) {
  const text = String(value || '').trim();
  return Boolean(text) && text !== '-' && text.toLowerCase() !== 'unspecified' && text.toLowerCase() !== 'null';
}

const cardStyle = {
  borderRadius: '10px',
  border: '1px solid #e2e8f0',
  background: '#fff',
  boxShadow: '0 1px 3px rgba(0,0,0,0.03)'
};

function BarRow({ label, value, max, color }) {
  const width = max > 0 ? Math.max(8, Math.round((value / max) * 100)) : 0;
  return (
    <div style={{ marginBottom: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
        <Text>{label}</Text>
        <Text strong>{value}</Text>
      </div>
      <div style={{ background: '#e2e8f0', height: 10, borderRadius: 6 }}>
        <div style={{ width: `${width}%`, height: 10, borderRadius: 6, background: color || BRAND_COLORS.primary }} />
      </div>
    </div>
  );
}

function polar(cx, cy, r, angle) {
  return {
    x: cx + r * Math.cos(angle),
    y: cy + r * Math.sin(angle)
  };
}

function slicePath(cx, cy, r, start, end) {
  const sweep = end - start;
  if (sweep >= Math.PI * 2 - 0.0001) {
    return `M ${cx} ${cy - r} A ${r} ${r} 0 1 1 ${cx} ${cy + r} A ${r} ${r} 0 1 1 ${cx} ${cy - r} Z`;
  }
  const from = polar(cx, cy, r, start);
  const to = polar(cx, cy, r, end);
  const largeArc = sweep > Math.PI ? 1 : 0;
  return `M ${cx} ${cy} L ${from.x} ${from.y} A ${r} ${r} 0 ${largeArc} 1 ${to.x} ${to.y} Z`;
}

function CategoryPieChart({ slices }) {
  const [hovered, setHovered] = useState(null);
  const size = 260;
  const cx = size / 2;
  const cy = size / 2;
  const r = 108;
  const total = slices.reduce((sum, item) => sum + item.value, 0);

  let angle = -Math.PI / 2;
  const drawn = slices.map((item) => {
    const sweep = total > 0 ? (item.value / total) * Math.PI * 2 : 0;
    const start = angle;
    const end = angle + sweep;
    angle = end;
    return { ...item, start, end, percent: total > 0 ? ((item.value / total) * 100).toFixed(1) : '0.0' };
  });

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          {drawn.length === 0 ? (
            <circle cx={cx} cy={cy} r={r} fill="#e2e8f0" />
          ) : drawn.map(slice => (
            <path
              key={slice.name}
              d={slicePath(cx, cy, r, slice.start, slice.end)}
              fill={slice.color}
              stroke="#fff"
              strokeWidth="2"
              style={{ cursor: 'pointer', opacity: hovered && hovered.name !== slice.name ? 0.55 : 1 }}
              onMouseEnter={() => setHovered(slice)}
              onMouseLeave={() => setHovered(null)}
            />
          ))}
        </svg>
        {hovered ? (
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              background: '#0f172a',
              color: '#fff',
              padding: '8px 12px',
              borderRadius: 8,
              fontSize: 12,
              pointerEvents: 'none',
              minWidth: 140,
              textAlign: 'center',
              boxShadow: '0 8px 20px rgba(15,23,42,0.25)'
            }}
          >
            <div style={{ fontWeight: 700 }}>{hovered.name}</div>
            <div>{hovered.value} complaint{hovered.value === 1 ? '' : 's'}</div>
            <div>{hovered.percent}% of total</div>
          </div>
        ) : null}
      </div>
      <div style={{ flex: 1, minWidth: 220 }}>
        <Text strong style={{ display: 'block', marginBottom: 10 }}>Color definition</Text>
        {drawn.length === 0 ? null : drawn.map(slice => (
          <div key={slice.name} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <span style={{ width: 12, height: 12, borderRadius: 3, background: slice.color, flexShrink: 0 }} />
            <Text style={{ flex: 1 }}>{slice.name}</Text>
            <Text type="secondary">{slice.value} ({slice.percent}%)</Text>
          </div>
        ))}
      </div>
    </div>
  );
}

function RcaDashboardPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [analysis, setAnalysis] = useState(null);
  const [selectedNature, setSelectedNature] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [capaNature, setCapaNature] = useState(null);
  const [isCapaModalOpen, setIsCapaModalOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    loadAnalysis();
  }, []);

  const loadAnalysis = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await ApiService.getRootCauseAnalysis();
      setAnalysis(data);
    } catch (err) {
      console.error(err);
      setError('Failed to load root cause analysis.');
    } finally {
      setLoading(false);
    }
  };

  const natures = (analysis?.natures || []).filter(item => hasDisplayValue(item.nature));
  const maxCount = natures.reduce((max, item) => Math.max(max, item.complaintCount || 0), 0);
  const categorySlices = (() => {
    const counts = {};
    natures.forEach(item => {
      (item.supportingComplaints || []).forEach(row => {
        if (!hasDisplayValue(row.category)) {
          return;
        }
        counts[row.category] = (counts[row.category] || 0) + 1;
      });
    });
    return Object.entries(counts)
      .sort((a, b) => b[1] - a[1])
      .map(([name, value], index) => ({
        name,
        value,
        color: CATEGORY_COLORS[index % CATEGORY_COLORS.length]
      }));
  })();
  const supportingComplaints = selectedNature?.supportingComplaints || [];
  const pagedComplaints = supportingComplaints.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const openNatureComplaints = (record) => {
    setSelectedNature(record);
    setCurrentPage(1);
    setIsDetailModalOpen(true);
  };

  const openCapaAnalysis = (record) => {
    setCapaNature(record?.nature);
    setIsCapaModalOpen(true);
  };

  const closeCapaAnalysis = () => {
    setIsCapaModalOpen(false);
    setCapaNature(null);
  };

  const closeNatureComplaints = () => {
    setIsDetailModalOpen(false);
    setSelectedNature(null);
  };

  const rcaExportDataset = () => ({
    filename: `Root_Cause_Analysis_${new Date().toISOString().slice(0, 10)}`,
    documentTitle: 'Root Cause Analysis',
    subtitle: 'Grouped by Nature of Complaint from Complainant Related Information',
    tables: [
      {
        title: 'Complaints by Nature',
        sheetName: 'By Nature',
        columns: [
          { header: 'Nature of Complaint', key: 'nature' },
          { header: 'Complaint Count', key: 'complaintCount' },
          { header: '% of Complaints', key: 'percentage', type: 'percent' }
        ],
        rows: natures
      },
      {
        title: 'Supporting Complaints',
        sheetName: 'Complaints',
        columns: [
          { header: 'Unique ID No', key: 'uniqueIdNo' },
          { header: 'Nature of Complaints', accessor: (row) => (hasDisplayValue(row.nature) ? row.nature : '-') },
          { header: 'Category', key: 'category' },
          { header: 'Status', type: 'status', accessor: (row) => row.status || row }
        ],
        rows: natures.flatMap(item => item.supportingComplaints || [])
      }
    ]
  });

  const summaryColumns = [
    { title: 'Nature of Complaint', dataIndex: 'nature', key: 'nature' },
    { title: 'Complaint Count', dataIndex: 'complaintCount', key: 'complaintCount', align: 'right', width: 160 },
    {
      title: '% of Complaints',
      dataIndex: 'percentage',
      key: 'percentage',
      align: 'right',
      width: 150,
      render: v => `${v}%`
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      width: 170,
      render: (_, record) => (
        <Space size={4} wrap style={{ justifyContent: 'center' }}>
          <Tooltip title="View Complaints">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '16px' }} />}
              onClick={() => openNatureComplaints(record)}
              style={{ padding: '2px 4px', height: '24px', display: 'flex', alignItems: 'center' }}
            />
          </Tooltip>
          <Button
            size="small"
            type="link"
            onClick={() => openCapaAnalysis(record)}
            style={{ padding: 0, height: 'auto', fontWeight: 600 }}
          >
            CAPA Analysis
          </Button>
        </Space>
      )
    }
  ];

  const complaintColumns = [
    {
      title: 'Unique ID No',
      dataIndex: 'uniqueIdNo',
      key: 'uniqueIdNo',
      width: 180,
      render: v => (
        <span
          style={{
            fontFamily: 'monospace',
            fontSize: '11px',
            color: BRAND_COLORS.primary,
            fontWeight: 600,
            background: '#f3f4f6',
            padding: '3px 8px',
            borderRadius: '4px',
            whiteSpace: 'nowrap'
          }}
        >
          {v || '—'}
        </span>
      )
    },
    {
      title: 'Nature of Complaints',
      dataIndex: 'nature',
      key: 'nature',
      render: v => (hasDisplayValue(v) ? v : '')
    },
    { title: 'Category', dataIndex: 'category', key: 'category' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: status => renderComplaintStatusTag(status)
    }
  ];

  return (
    <DashboardLayout>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <div>
          <Title level={3} style={{ color: BRAND_COLORS.primary, margin: 0 }}>Root Cause Analysis</Title>
          <Text type="secondary">Grouped by Nature of Complaint from Complainant Related Information</Text>
        </div>

        {error ? <Alert type="error" message={error} showIcon /> : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={6}><Card style={cardStyle}><Statistic title="Total Complaints" value={analysis?.totalComplaints || 0} /></Card></Col>
          <Col xs={24} sm={12} md={6}><Card style={cardStyle}><Statistic title="Complaint Natures" value={analysis?.totalNatures || 0} /></Card></Col>
          <Col xs={24} sm={12} md={6}><Card style={cardStyle}><Statistic title="Most Common Nature" value={analysis?.mostCommonNature || '—'} valueStyle={{ fontSize: 16 }} /></Card></Col>
          <Col xs={24} sm={12} md={6}><Card style={cardStyle}><Statistic title="Highest Escalation/Resolution" value={analysis?.highestEscalationResolutionNature || '—'} valueStyle={{ fontSize: 16 }} /></Card></Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title="Complaints by Nature" loading={loading} style={cardStyle}>
              {natures.length === 0 ? null : natures.map(item => (
                <BarRow key={item.nature} label={item.nature} value={item.complaintCount} max={maxCount} />
              ))}
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="Complaints by Category" loading={loading} style={cardStyle}>
              <CategoryPieChart slices={categorySlices} />
            </Card>
          </Col>
        </Row>

        <Card bodyStyle={{ padding: '20px 24px' }} style={cardStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
            <Title level={4} style={{ color: BRAND_COLORS.primary, margin: 0, fontSize: '16px', fontWeight: 700 }}>
              Complaint Nature Summary
            </Title>
            <ExportDropdown getDataset={rcaExportDataset} />
          </div>
          <div style={{ background: '#fff', padding: '12px 4px', borderRadius: '8px', border: '1px solid #f0f0f0' }}>
            <Table
              rowKey="nature"
              columns={summaryColumns}
              dataSource={natures}
              pagination={false}
              loading={loading}
              size="middle"
              locale={{ emptyText: <span /> }}
            />
          </div>
        </Card>
      </Space>

      <Modal
        title={(
          <span style={{ fontSize: '18px', fontWeight: 700, color: BRAND_COLORS.primary }}>
            Complaints — {selectedNature?.nature || ''}
          </span>
        )}
        open={isDetailModalOpen}
        onCancel={closeNatureComplaints}
        width="100%"
        style={{ maxWidth: 1100, top: 20 }}
        styles={{ body: { padding: '24px', maxHeight: '80vh', overflowY: 'auto' } }}
        footer={[
          <Button key="close" type="primary" onClick={closeNatureComplaints} style={{ background: BRAND_COLORS.primary }}>
            Close
          </Button>
        ]}
      >
        <Table
          rowKey={(row, index) => row.uniqueIdNo || `${selectedNature?.nature}-${index}`}
          columns={complaintColumns}
          dataSource={pagedComplaints}
          pagination={false}
          size="middle"
          locale={{ emptyText: 'No complaints found for this nature.' }}
        />
        <Pagination
          currentPage={currentPage}
          pageSize={pageSize}
          totalRecords={supportingComplaints.length}
          onPageChange={setCurrentPage}
          onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }}
          itemUnit="complaints"
        />
      </Modal>

      <CapaAnalysisModal
        open={isCapaModalOpen}
        nature={capaNature}
        onClose={closeCapaAnalysis}
      />
    </DashboardLayout>
  );
}

function RcaDashboard() {
  return <RcaDashboardPage key="rca-summary-v4" />;
}

export default RcaDashboard;
