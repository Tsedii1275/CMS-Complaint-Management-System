import React, { useEffect, useMemo, useState } from 'react';
import { Button, Input, Modal, Select, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { categoryNamesFromResponse } from '../constants/complaintCategories';

const { Option } = Select;

function ComplaintCategorySelect({ value, onChange, style, allowAdd = false }) {
  const [categories, setCategories] = useState(() => categoryNamesFromResponse(null));
  const [adding, setAdding] = useState(false);
  const [draft, setDraft] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      const rows = await ApiService.getComplaintCategories();
      setCategories(categoryNamesFromResponse(rows));
    } catch {
      setCategories(categoryNamesFromResponse(null));
    }
  };

  useEffect(() => {
    load();
  }, []);

  const options = useMemo(() => {
    const names = [...categories];
    if (value && !names.includes(value)) {
      names.push(value);
    }
    return names;
  }, [categories, value]);

  const closeAdd = () => {
    setAdding(false);
    setDraft('');
    setSaving(false);
  };

  const saveCategory = async () => {
    const name = draft.trim();
    if (!name) {
      message.warning('Enter the new category name.');
      return;
    }
    setSaving(true);
    try {
      const saved = await ApiService.createComplaintCategory(name);
      const savedName = saved?.name || name;
      setCategories((current) => (current.includes(savedName) ? current : [...current, savedName]));
      onChange?.(savedName);
      message.success('Category added.');
      closeAdd();
    } catch (err) {
      message.error(err?.message || 'Could not add the category.');
      setSaving(false);
    }
  };

  return (
    <div>
      <Select value={value} style={style} onChange={onChange}>
        {options.map((name) => (
          <Option key={name} value={name}>{name}</Option>
        ))}
      </Select>
      {allowAdd && (
        <Button
          type="link"
          icon={<PlusOutlined />}
          onClick={() => setAdding(true)}
          style={{ paddingLeft: 0, height: 'auto', marginTop: 4 }}
        >
          Add Category
        </Button>
      )}
      <Modal
        title="Add Category"
        open={adding}
        onCancel={closeAdd}
        destroyOnClose
        footer={[
          <Button key="cancel" onClick={closeAdd} disabled={saving}>Cancel</Button>,
          <Button key="save" type="primary" loading={saving} onClick={saveCategory}>Save</Button>
        ]}
      >
        <Input
          value={draft}
          maxLength={100}
          placeholder="Category name"
          onChange={(event) => setDraft(event.target.value)}
          onPressEnter={saveCategory}
        />
      </Modal>
    </div>
  );
}

export default ComplaintCategorySelect;
