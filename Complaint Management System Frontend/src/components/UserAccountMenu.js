import React from 'react';
import { Avatar, Dropdown, Typography } from 'antd';
import { LockOutlined, LogoutOutlined } from '@ant-design/icons';
import { BRAND_COLORS } from '../constants/theme';

function UserAccountMenu({ displayName, initial, onUpdatePassword, onLogout, canUpdatePassword }) {
  const items = [];
  if (canUpdatePassword) {
    items.push({
      key: 'update-password',
      label: 'Update Password',
      icon: <LockOutlined />,
      onClick: onUpdatePassword
    });
  }
  items.push({
    key: 'logout',
    label: 'Logout',
    icon: <LogoutOutlined />,
    danger: true,
    onClick: onLogout
  });
  return (
    <Dropdown
      trigger={['click']}
      menu={{ items }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
        <Avatar style={{ backgroundColor: BRAND_COLORS.accent, color: BRAND_COLORS.primary, fontWeight: 'bold' }}>
          {initial}
        </Avatar>
        <div style={{ display: 'flex', flexDirection: 'column', lineHeight: '1.2' }} className="hide-on-mobile">
          <Typography.Text style={{ color: BRAND_COLORS.white, fontWeight: 500 }}>
            {displayName}
          </Typography.Text>
        </div>
      </div>
    </Dropdown>
  );
}

export default UserAccountMenu;
