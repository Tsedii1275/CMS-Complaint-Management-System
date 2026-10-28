import React, { useCallback, useState } from 'react';
import { Input, Typography } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import DashboardLayout from '../components/DashboardLayout';
import FeedbackPanel from '../components/FeedbackPanel';
import ApiService from '../services/api';
import { BRAND_COLORS } from '../constants/theme';

const { Title, Text } = Typography;

function EthicsDashboard() {
  const [searchQuery, setSearchQuery] = useState('');
  const loadItems = useCallback(() => ApiService.getEthicsSlaMetrics(), []);

  return (
    <DashboardLayout>
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        <Title level={2} style={{ margin: 0, color: BRAND_COLORS.primary }}>
          Ethics and Conduct
        </Title>
        <Text type="secondary" style={{ display: 'block', margin: '8px 0 16px' }}>
          Cases screened as Ethics and Conduct. This list is view only.
        </Text>
        <Input
          className="pill-search-input"
          allowClear
          prefix={<SearchOutlined />}
          placeholder="Search by Unique ID, customer, phone, or details"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ width: '100%', maxWidth: 360, marginBottom: 16 }}
        />
        <FeedbackPanel
          searchQuery={searchQuery}
          loadItems={loadItems}
          descriptionTitle="Details"
          detailsTitle="Ethics and Conduct"
          emptyDescription="No ethics and conduct cases yet."
          totalNoun="cases"
        />
      </div>
    </DashboardLayout>
  );
}

export default EthicsDashboard;
