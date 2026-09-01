import React from 'react';
import { Typography, Divider, Carousel, Steps } from 'antd';
import {
  SecurityScanOutlined,
  ProjectOutlined,
  UsergroupAddOutlined,
  FilePdfOutlined,
  FieldTimeOutlined,
  GlobalOutlined,
  SendOutlined
} from '@ant-design/icons';
import logo from '../assets/Dashen-Bank-Logo-Addis-Ababa-Ethiopia.png';

const { Title } = Typography;

const features = [
  {
    title: 'Complaint Registration & Intake',
    description: 'Streamlined multi-channel intake for customer complaints across branch staff, digital banking, and customer portals.',
    icon: <SecurityScanOutlined />
  },
  {
    title: 'Branch & Department Routing',
    description: 'Automated multi-level routing to assigned district, branch, and department managers based on organizational hierarchy.',
    icon: <UsergroupAddOutlined />
  },
  {
    title: 'SLA & Escalation Tracking',
    description: 'Real-time SLA monitoring with color-coded warnings and supervisor escalations for overdue tickets.',
    icon: <FieldTimeOutlined />
  },
  {
    title: 'Investigation & Audit Workflows',
    description: 'Comprehensive audit trails, root cause analysis (RCA), and evidence gathering for sensitive cases.',
    icon: <ProjectOutlined />
  },
  {
    title: 'Multilingual Notifications',
    description: 'Automated SMS and email communication in the customer\'s preferred language (English & Amharic).',
    icon: <GlobalOutlined />
  },
  {
    title: 'Customer Feedback & Satisfaction',
    description: 'Post-resolution CSAT, NPS, and effort score measurement with automated dispute reopening logic.',
    icon: <SendOutlined />
  },
  {
    title: 'NBE & Executive Reporting',
    description: 'Automated National Bank of Ethiopia compliance reporting and executive analytics dashboards.',
    icon: <FilePdfOutlined />
  }
];

const FeaturesSection = () => {
  return (
    <div style={{ width: '100%', maxWidth: 600, padding: '30px 40px' }}>
      <div style={{ textAlign: 'center' }}>
        <img
          src={logo}
          alt="Dashen Bank Logo"
          style={{ height: 100, marginBottom: 40, cursor: 'pointer', display: 'block', margin: '0 auto 40px' }}
        />
        <Title level={3} style={{ textAlign: 'center', margin: 0, color: 'var(--primary-color, #012169)', fontWeight: 700 }}>
          Complaint Management Portal
        </Title>
      </div>

      <Divider dashed style={{ borderColor: '#eef2f6', margin: '24px 0' }} />

      <div style={{ minHeight: 320 }}>
        <Carousel autoplay dots={{ className: 'custom-dots' }} speed={400} autoplaySpeed={2500}>
          {features.map((feature) => (
            <div key={feature.title} style={{ height: 260, outline: 'none' }}>
              <div style={{
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '0 20px'
              }}>
                <Steps
                  direction="vertical"
                  current={0}
                  items={[
                    {
                      title: <span style={{ color: 'rgb(2, 90, 162)', fontWeight: 600 }}>{feature.title}</span>,
                      description: <p style={{ color: 'var(--text-muted, #636e72)', fontSize: 14 }}>{feature.description}</p>,
                      icon: <div style={{
                        fontSize: 32,
                        color: 'rgb(2, 90, 162)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        height: '100%'
                      }}>{feature.icon}</div>,
                    }
                  ]}
                />
              </div>
            </div>
          ))}
        </Carousel>
      </div>
    </div>
  );
};

export default FeaturesSection;
