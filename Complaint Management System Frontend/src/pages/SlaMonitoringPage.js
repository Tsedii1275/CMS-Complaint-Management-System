import React, { useState, useEffect, useMemo } from 'react';
import {
  Card, Typography, Row, Col, Statistic, Table, Tag, Button, Input,
  DatePicker, Alert, Modal, Tooltip
} from 'antd';
import {
  SearchOutlined, SyncOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { formatUniqueId, formatIntakeId, matchesTicketSearch } from '../components/TaskTable';
import DashboardLayout from '../components/DashboardLayout';
import ExportDropdown from '../components/ExportDropdown';
import Pagination from '../components/Pagination';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';
import SlaTimelineComponent from '../components/SlaTimelineComponent';
import { renderComplaintStatusTag } from '../utils/statusUtils';
import { classifyComplaintFilter, isSlaBreached, isTerminalOverallStatus } from '../utils/slaMetrics';
import moment from 'moment';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

/**
 * Format minutes into human-readable string (Secs, Mins, Hours, Days)
 */
const formatMins = (mins) => {
  if (mins === null || mins === undefined || Number.isNaN(mins)) return '—';
  const num = Number(mins);
  if (num <= 0) return '0 Mins';
  if (num < 1) {
    const secs = Math.round(num * 60);
    return secs > 0 ? `${secs} Secs` : '0 Secs';
  }
  if (num < 60) return `${Math.round(num)} Mins`;
  if (num < 480) return `${(num / 60).toFixed(1)} Hours`;
  const days = (num / 480).toFixed(1);
  return `${days} ${Number(days) === 1 ? 'Day' : 'Days'} (8h/day)`;
};

/**
 * Clean SLA Compliance % helper (Percent value only):
 * Green: >= 90%
 * Amber: 75% - 89%
 * Red: < 75%
 */
function formatClassification(val) {
  if (!val || String(val).trim() === '' || String(val).trim() === 'null') {
    return 'Not Classified';
  }
  const str = String(val).trim();
  const p = str.toUpperCase();
  if (p.includes('HIGH') || p.includes('CRITICAL')) return 'Highly Sensitive';
  if (p.includes('SENSITIVE')) return 'Sensitive';
  if (p.includes('GENERAL') || p.includes('NORMAL')) return 'General';
  return str;
}

function matchesSearchQuery(m, q) {
  if (!q) return true;
  return matchesTicketSearch(m, q) ||
    m.customerName?.toLowerCase().includes(q) ||
    m.branch?.toLowerCase().includes(q) ||
    m.department?.toLowerCase().includes(q) ||
    m.district?.toLowerCase().includes(q);
}

function matchesExactFilter(value, filter) {
  return !filter || value?.toLowerCase() === filter.toLowerCase();
}

function matchesUserFilter(m, userFilter) {
  if (!userFilter) return true;
  const needle = userFilter.toLowerCase();
  return m.manager?.toLowerCase().includes(needle) ||
    m.staffHandling?.toLowerCase().includes(needle);
}

function matchesPriorityFilter(m, priorityFilter) {
  if (!priorityFilter) return true;
  const cls = m.complaintClassification || m.variables?.complaintClassification || m.priority;
  return Boolean(cls) && formatClassification(cls).toLowerCase() === priorityFilter.toLowerCase();
}

function matchesDateRange(m, dateRange) {
  if (!dateRange?.[0] || !dateRange?.[1]) return true;
  const created = moment(m.createdAt);
  return created.isAfter(dateRange[0].startOf('day')) && created.isBefore(dateRange[1].endOf('day'));
}

function filterMetric(m, filters) {
  const { searchQuery, districtFilter, branchFilter, userFilter, priorityFilter, stageFilter, dateRange } = filters;
  const q = searchQuery.toLowerCase().trim();
  if (!matchesSearchQuery(m, q)) return false;
  if (!matchesExactFilter(m.district, districtFilter)) return false;
  if (!matchesExactFilter(m.branch, branchFilter)) return false;
  if (!matchesUserFilter(m, userFilter)) return false;
  if (!matchesPriorityFilter(m, priorityFilter)) return false;
  if (stageFilter && !m.currentStage?.toLowerCase().includes(stageFilter.toLowerCase())) return false;
  return matchesDateRange(m, dateRange);
}

function SlaMonitoringPage() {
  const [metrics, setMetrics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination & Analysis Tab State
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Global Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [dateRange, setDateRange] = useState(null);
  const [districtFilter, setDistrictFilter] = useState('');
  const [branchFilter, setBranchFilter] = useState('');
  const [userFilter, setUserFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [stageFilter, setStageFilter] = useState('');

  // Timeline Modal
  const [selectedComplaintId, setSelectedComplaintId] = useState(null);
  const [isTimelineModalOpen, setIsTimelineModalOpen] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await ApiService.getAllSlaMetrics();

      setMetrics((data || []).filter(classifyComplaintFilter));
    } catch (err) {
      console.error('Failed to load SLA metrics:', err);
      setError('Failed to load SLA metrics from server.');
    } finally {
      setLoading(false);
    }
  };

  // ─── DYNAMIC FILTERING ───
  const filteredMetrics = useMemo(() => {
    const filters = { searchQuery, districtFilter, branchFilter, userFilter, priorityFilter, stageFilter, dateRange };
    return metrics.filter(m => filterMetric(m, filters));
  }, [
    metrics, searchQuery, districtFilter, branchFilter, userFilter,
    priorityFilter, stageFilter, dateRange
  ]);

  // ─── OVERALL KPI CALCULATIONS ───
  const totalCount = filteredMetrics.length;
  const activeCount = filteredMetrics.filter(m => !isTerminalOverallStatus(m.status)).length;
  const breachedCount = filteredMetrics.filter(isSlaBreached).length;
  const withinSlaCount = Math.max(0, totalCount - breachedCount);

  const breachedMetricsList = filteredMetrics.filter(isSlaBreached);
  const totalBreachMins = breachedMetricsList.reduce((acc, m) => {
    const allowed = m.totalAllowedMinutes;
    if (allowed == null) return acc;
    const diff = Math.max(0, (m.totalElapsedMinutes || 0) - allowed);
    return acc + diff;
  }, 0);
  const avgBreachDurationMins = breachedMetricsList.length > 0 ? totalBreachMins / breachedMetricsList.length : 0;

  // ─── PERFORMANCE BREAKDOWNS ───
  const districtPerformance = useMemo(() => {
    const map = {};
    filteredMetrics.forEach(m => {
      const dist = m.district || 'Unassigned District';
      if (!map[dist]) {
        map[dist] = { name: dist, total: 0, breached: 0, totalRespMins: 0, totalResMins: 0 };
      }
      map[dist].total++;
      if (isSlaBreached(m)) map[dist].breached++;
      map[dist].totalRespMins += m.responseMinutes || 15;
      map[dist].totalResMins += m.totalElapsedMinutes || 120;
    });
    return Object.values(map).map(d => {
      const withinSla = Math.max(0, d.total - d.breached);
      const complianceRate = d.total > 0 ? (withinSla / d.total) * 100 : 100;
      return {
        ...d,
        withinSla,
        complianceRate,
        avgResponseMins: d.total > 0 ? d.totalRespMins / d.total : 0,
        avgResolutionMins: d.total > 0 ? d.totalResMins / d.total : 0
      };
    });
  }, [filteredMetrics]);

  const branchPerformance = useMemo(() => {
    const map = {};
    filteredMetrics.forEach(m => {
      const br = m.branch || 'Head Office / Direct';
      if (!map[br]) {
        map[br] = { name: br, total: 0, breached: 0, totalRespMins: 0, totalResMins: 0 };
      }
      map[br].total++;
      if (isSlaBreached(m)) map[br].breached++;
      map[br].totalRespMins += m.responseMinutes || 20;
      map[br].totalResMins += m.totalElapsedMinutes || 180;
    });
    return Object.values(map).map(b => {
      const withinSla = Math.max(0, b.total - b.breached);
      const complianceRate = b.total > 0 ? (withinSla / b.total) * 100 : 100;
      return {
        ...b,
        withinSla,
        complianceRate,
        avgResponseMins: b.total > 0 ? b.totalRespMins / b.total : 0,
        avgResolutionMins: b.total > 0 ? b.totalResMins / b.total : 0
      };
    });
  }, [filteredMetrics]);

  const departmentPerformance = useMemo(() => {
    const map = {};
    filteredMetrics.forEach(m => {
      const dept = m.department || 'General Customer Service';
      if (!map[dept]) {
        map[dept] = { name: dept, total: 0, breached: 0, totalResMins: 0 };
      }
      map[dept].total++;
      if (isSlaBreached(m)) map[dept].breached++;
      map[dept].totalResMins += m.totalElapsedMinutes || 240;
    });
    return Object.values(map).map(d => {
      const withinSla = Math.max(0, d.total - d.breached);
      const complianceRate = d.total > 0 ? (withinSla / d.total) * 100 : 100;
      return {
        ...d,
        withinSla,
        complianceRate,
        avgResolutionMins: d.total > 0 ? d.totalResMins / d.total : 0
      };
    });
  }, [filteredMetrics]);

  const userPerformance = useMemo(() => {
    const map = {};
    filteredMetrics.forEach(m => {
      const uname = m.staffHandling || m.manager || 'Unassigned Staff';
      if (!map[uname]) {
        map[uname] = {
          name: uname,
          role: m.staffRole || 'Case Handler',
          department: m.department || 'Operations',
          branch: m.branch || 'Main Branch',
          assigned: 0,
          completed: 0,
          breached: 0,
          totalRespMins: 0,
          totalResMins: 0
        };
      }
      map[uname].assigned++;
      if (m.status === 'CLOSED' || m.status === 'RESOLVED') map[uname].completed++;
      if (isSlaBreached(m)) map[uname].breached++;
      map[uname].totalRespMins += m.responseMinutes || 15;
      map[uname].totalResMins += m.totalElapsedMinutes || 150;
    });
    return Object.values(map).map(u => {
      const withinSla = Math.max(0, u.assigned - u.breached);
      const complianceRate = u.assigned > 0 ? (withinSla / u.assigned) * 100 : 100;
      return {
        ...u,
        withinSla,
        complianceRate,
        avgResponseMins: u.assigned > 0 ? u.totalRespMins / u.assigned : 0,
        avgResolutionMins: u.assigned > 0 ? u.totalResMins / u.assigned : 0
      };
    });
  }, [filteredMetrics]);

  const workflowStagePerformance = useMemo(() => {
    const map = {};
    filteredMetrics.forEach(m => {
      const stage = m.currentStage || 'CMD Screening & Triage';
      if (!map[stage]) {
        map[stage] = { name: stage, total: 0, breached: 0, totalCompletionMins: 0, totalBreachMins: 0 };
      }
      map[stage].total++;
      if (isSlaBreached(m)) {
        map[stage].breached++;
        if (m.totalAllowedMinutes != null) {
          map[stage].totalBreachMins += Math.max(0, (m.totalElapsedMinutes || 0) - m.totalAllowedMinutes);
        }
      }
      map[stage].totalCompletionMins += m.totalElapsedMinutes || 60;
    });
    return Object.values(map).map(s => {
      const withinSla = Math.max(0, s.total - s.breached);
      const complianceRate = s.total > 0 ? (withinSla / s.total) * 100 : 100;
      return {
        ...s,
        withinSla,
        complianceRate,
        avgCompletionMins: s.total > 0 ? s.totalCompletionMins / s.total : 0,
        avgBreachMins: s.breached > 0 ? s.totalBreachMins / s.breached : 0
      };
    });
  }, [filteredMetrics]);

  const slaQueueStatus = (r) => {
    if (isSlaBreached(r)) {
      const allowed = r.totalAllowedMinutes;
      const diff = allowed == null ? 0 : Math.max(0, (r.totalElapsedMinutes || 0) - allowed);
      return `BREACHED (+${formatMins(diff)})`;
    }
    if (r.slaStatus === 'APPROACHING') return 'APPROACHING';
    if (r.slaStatus === 'RESOLVED_WITHIN_SLA') return 'RESOLVED WITHIN SLA';
    return 'ON TRACK';
  };

  const exportSlaDataset = () => ({
    filename: `SLA_Governance_Report_${moment().format('YYYYMMDD')}`,
    documentTitle: 'SLA Governance Report',
    subtitle: `Live queue and performance  |  ${moment().format('DD/MM/YYYY HH')}`,
    tables: [
      {
        title: 'Live SLA Performance & Breach Monitoring Queue',
        sheetName: 'Live Queue',
        columns: [
          { header: 'Unique ID No', accessor: (r) => formatUniqueId(r) },
          { header: 'Overall Status', type: 'status', accessor: (r) => r },
          { header: 'Customer', key: 'customerName' },
          { header: 'SLA Status & Breach Info', accessor: slaQueueStatus }
        ],
        rows: filteredMetrics
      },
      {
        title: 'Branch SLA Performance',
        sheetName: 'Branch',
        columns: [
          { header: 'Branch Name', key: 'name' },
          { header: 'Total Complaints', key: 'total' },
          { header: 'Within SLA', key: 'withinSla' },
          { header: 'Breached', key: 'breached' },
          { header: 'SLA Compliance %', key: 'complianceRate', type: 'percent' },
          { header: 'Avg Response Time', accessor: (b) => formatMins(b.avgResponseMins) },
          { header: 'Avg Resolution Time', accessor: (b) => formatMins(b.avgResolutionMins) }
        ],
        rows: branchPerformance
      },
      {
        title: 'District SLA Performance',
        sheetName: 'District',
        columns: [
          { header: 'District Name', key: 'name' },
          { header: 'Total Complaints', key: 'total' },
          { header: 'Within SLA', key: 'withinSla' },
          { header: 'Breached', key: 'breached' },
          { header: 'SLA Compliance %', key: 'complianceRate', type: 'percent' },
          { header: 'Avg Response Time', accessor: (d) => formatMins(d.avgResponseMins) },
          { header: 'Avg Resolution Time', accessor: (d) => formatMins(d.avgResolutionMins) }
        ],
        rows: districtPerformance
      },
      {
        title: 'Department / Work Unit Performance',
        sheetName: 'Department',
        columns: [
          { header: 'Department Name', key: 'name' },
          { header: 'Total Complaints', key: 'total' },
          { header: 'Within SLA', key: 'withinSla' },
          { header: 'Breached', key: 'breached' },
          { header: 'SLA Compliance %', key: 'complianceRate', type: 'percent' },
          { header: 'Avg Resolution Time', accessor: (d) => formatMins(d.avgResolutionMins) }
        ],
        rows: departmentPerformance
      },
      {
        title: 'User SLA Performance',
        sheetName: 'User',
        columns: [
          { header: 'User Name', key: 'name' },
          { header: 'Role', key: 'role' },
          { header: 'Department', key: 'department' },
          { header: 'Branch', key: 'branch' },
          { header: 'Assigned', key: 'assigned' },
          { header: 'Completed', key: 'completed' },
          { header: 'SLA Compliance %', key: 'complianceRate', type: 'percent' },
          { header: 'Avg Response Time', accessor: (u) => formatMins(u.avgResponseMins) },
          { header: 'Avg Resolution Time', accessor: (u) => formatMins(u.avgResolutionMins) }
        ],
        rows: userPerformance
      },
      {
        title: 'Workflow Stage Performance',
        sheetName: 'Stage',
        columns: [
          { header: 'Workflow Stage', key: 'name' },
          { header: 'Total Cases', key: 'total' },
          { header: 'Within SLA', key: 'withinSla' },
          { header: 'Breached', key: 'breached' },
          { header: 'SLA Compliance %', key: 'complianceRate', type: 'percent' },
          { header: 'Avg Completion Time', accessor: (s) => formatMins(s.avgCompletionMins) },
          { header: 'Avg Breach Duration', accessor: (s) => formatMins(s.avgBreachMins) }
        ],
        rows: workflowStagePerformance
      }
    ]
  });

  const handleOpenTimeline = (complaintId) => {
    setSelectedComplaintId(complaintId);
    setIsTimelineModalOpen(true);
  };

  return (
    <DashboardLayout userRole="admin">
      <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '24px', background: '#f8fafc', minHeight: '100vh' }}>

        {/* Header Title */}
        <div style={{ marginBottom: '24px' }}>
          <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary, fontWeight: 800, fontSize: '24px' }}>
            Centralized SLA Governance & Performance Monitoring
          </Title>
          <Text type="secondary" style={{ fontSize: '13px' }}>
            Dashen Bank Governance, Departmental Compliance & Organizational SLA Visibility
          </Text>
        </div>

        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 20 }} />}

        {/* ─── 1. STREAMLINED KPI SUMMARY CARDS ─── */}
        <Row gutter={[16, 16]} style={{ marginBottom: '20px' }}>
          <Col xs={24} sm={8} lg={8}>
            <Card bodyStyle={{ padding: '16px 20px' }} style={{ borderRadius: '10px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', border: '1px solid #e2e8f0', background: '#fff' }}>
              <Statistic
                title={<span style={{ fontWeight: 600, color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Total Complaints</span>}
                value={totalCount}
                valueStyle={{ color: BRAND_COLORS.primary, fontWeight: 800, fontSize: '24px' }}
              />
              <Text type="secondary" style={{ fontSize: '11px' }}>{activeCount} Active | {totalCount - activeCount} Closed</Text>
            </Card>
          </Col>
          <Col xs={24} sm={8} lg={8}>
            <Card bodyStyle={{ padding: '16px 20px' }} style={{ borderRadius: '10px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', border: '1px solid #e2e8f0', background: '#fff' }}>
              <Statistic
                title={<span style={{ fontWeight: 600, color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Within SLA</span>}
                value={withinSlaCount}
                valueStyle={{ color: '#059669', fontWeight: 800, fontSize: '24px' }}
              />
              <Text type="secondary" style={{ fontSize: '11px' }}>{((withinSlaCount / (totalCount || 1)) * 100).toFixed(1)}% On Track</Text>
            </Card>
          </Col>
          <Col xs={24} sm={8} lg={8}>
            <Card bodyStyle={{ padding: '16px 20px' }} style={{ borderRadius: '10px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', border: '1px solid #e2e8f0', background: '#fff' }}>
              <Statistic
                title={<span style={{ fontWeight: 600, color: '#64748b', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>SLA Breached</span>}
                value={breachedCount}
                valueStyle={{ color: '#dc2626', fontWeight: 800, fontSize: '24px' }}
              />
              <Text type="secondary" style={{ fontSize: '11px' }}>Avg Breach: {formatMins(avgBreachDurationMins)}</Text>
            </Card>
          </Col>
        </Row>

        {/* ─── 2. SLEEK SINGLE-ROW GLOBAL FILTER BAR ─── */}
        <Card bodyStyle={{ padding: '14px 20px' }} style={{ borderRadius: '10px', marginBottom: '24px', border: '1px solid #e2e8f0', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.03)' }}>
          <Row gutter={[12, 12]} align="middle">
            <Col xs={24} sm={12} md={10} lg={10}>
              <Input
                placeholder="Search Ticket, Customer, Branch..."
                prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                allowClear
                style={{ borderRadius: '6px' }}
              />
            </Col>
            <Col xs={24} sm={12} md={10} lg={10}>
              <RangePicker style={{ width: '100%', borderRadius: '6px' }} onChange={setDateRange} value={dateRange} />
            </Col>
            <Col xs={24} sm={24} md={4} lg={4}>
              <Button
                type="default"
                icon={<SyncOutlined />}
                onClick={() => {
                  setSearchQuery('');
                  setDateRange(null);
                  setDistrictFilter('');
                  setBranchFilter('');
                  setUserFilter('');
                  setPriorityFilter('');
                  setStageFilter('');
                }}
                style={{ width: '100%', borderRadius: '6px' }}
              >
                Reset
              </Button>
            </Col>
          </Row>
        </Card>

        {/* ─── 3. LIVE SLA MONITORING QUEUE TABLE (FIRST) ─── */}
        <Card bodyStyle={{ padding: '20px 24px' }} style={{ borderRadius: '10px', marginBottom: '24px', border: '1px solid #e2e8f0', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.03)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
            <Title level={4} style={{ color: BRAND_COLORS.primary, margin: 0, fontSize: '16px', fontWeight: 700 }}>
              Live SLA Performance & Breach Monitoring Queue
            </Title>
            <ExportDropdown size="small" getDataset={exportSlaDataset} />
          </div>

          <div style={{ background: '#fff', padding: '12px 20px', borderRadius: '8px', border: '1px solid #f0f0f0' }}>
            <Table
              dataSource={filteredMetrics.slice((currentPage - 1) * pageSize, currentPage * pageSize)}
              rowKey="id"
              loading={loading}
              size="middle"
              bordered={false}
              pagination={false}
              columns={[
                {
                  title: 'Unique ID No',
                  dataIndex: 'complaintId',
                  key: 'complaintId',
                  width: 220,
                  render: (id, r) => (
                    <>
                      <Button
                        type="link"
                        onClick={() => handleOpenTimeline(r?.dbcTicketId || r?.complaintId || r?.generalTicketId || r?.processInstanceId || id)}
                        style={{
                          fontFamily: 'monospace',
                          fontSize: '11px',
                          color: BRAND_COLORS.primary,
                          fontWeight: 600,
                          background: '#f3f4f6',
                          padding: '3px 8px',
                          borderRadius: '4px',
                          whiteSpace: 'nowrap',
                          height: 'auto'
                        }}
                      >
                        {formatUniqueId(r || id)}
                      </Button>
                      {formatIntakeId(r) && formatIntakeId(r) !== formatUniqueId(r) && (
                        <div style={{ fontSize: '10px', color: '#64748b', paddingLeft: '8px' }}>Intake {formatIntakeId(r)}</div>
                      )}
                    </>
                  )
                },
                {
                  title: 'Overall Status',
                  key: 'overallStatus',
                  width: 120,
                  render: (_, r) => renderComplaintStatusTag(r)
                },
                {
                  title: 'Customer',
                  dataIndex: 'customerName',
                  key: 'customerName',
                  width: 140,
                  render: (name) => <span style={{ fontSize: '12px', fontWeight: 500, whiteSpace: 'nowrap' }}>{name || '—'}</span>
                },

                {
                  title: 'SLA Status & Breach Info',
                  key: 'statusOrigin',
                  width: 220,
                  render: (_, r) => {
                    const isBreached = isSlaBreached(r);
                    if (isBreached) {
                      const allowed = r.totalAllowedMinutes;
                      const diff = allowed == null ? 0 : Math.max(0, (r.totalElapsedMinutes || 0) - allowed);
                      return (
                        <Tag color="red" style={{ fontWeight: 700, fontSize: '11px', whiteSpace: 'nowrap' }}>
                          BREACHED (+{formatMins(diff)})
                        </Tag>
                      );
                    }
                    if (r.slaStatus === 'APPROACHING') {
                      return <Tag color="orange" style={{ fontWeight: 700, fontSize: '11px', whiteSpace: 'nowrap' }}>APPROACHING</Tag>;
                    }
                    if (r.slaStatus === 'RESOLVED_WITHIN_SLA') {
                      return <Tag color="green" style={{ fontWeight: 600, fontSize: '11px', whiteSpace: 'nowrap' }}>RESOLVED WITHIN SLA</Tag>;
                    }
                    return <Tag color="green" style={{ fontWeight: 600, fontSize: '11px', whiteSpace: 'nowrap' }}>ON TRACK</Tag>;
                  }
                },
                {
                  title: 'Action',
                  key: 'action',
                  width: 60,
                  align: 'center',
                  render: (_, r) => (
                    <Tooltip title="View Stage SLA Timeline">
                      <Button
                        type="text"
                        size="small"
                        icon={<EyeOutlined style={{ color: BRAND_COLORS.primary, fontSize: '15px' }} />}
                        onClick={() => handleOpenTimeline(r?.dbcTicketId || r?.complaintId || r?.processInstanceId)}
                        style={{ padding: '2px 4px', height: '24px', display: 'flex', alignItems: 'center' }}
                      />
                    </Tooltip>
                  )
                }
              ]}
            />
            <Pagination
              currentPage={currentPage}
              pageSize={pageSize}
              totalRecords={filteredMetrics.length}
              onPageChange={(page) => setCurrentPage(page)}
              onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }}
              itemUnit="complaints"
            />
          </div>
        </Card>

        {/* SLA Timeline Modal */}
        <Modal
          title={<span style={{ fontSize: '18px', fontWeight: 700, color: BRAND_COLORS.primary }}>Complaint SLA Timeline — {selectedComplaintId || ''}</span>}
          open={isTimelineModalOpen}
          onOk={() => setIsTimelineModalOpen(false)}
          onCancel={() => setIsTimelineModalOpen(false)}
          width="100%"
          style={{ maxWidth: 1350, top: 20 }}
          styles={{ body: { padding: '24px', maxHeight: '80vh', overflowY: 'auto' } }}
          footer={[
            <Button key="close" type="primary" size="middle" onClick={() => setIsTimelineModalOpen(false)}>
              Close Timeline
            </Button>
          ]}
        >
          {selectedComplaintId && <SlaTimelineComponent complaintId={selectedComplaintId} />}
        </Modal>

      </div>
    </DashboardLayout>
  );
}

export default SlaMonitoringPage;
