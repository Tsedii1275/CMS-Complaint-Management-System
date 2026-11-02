import React, { useEffect, useMemo, useState } from 'react';
import { Button, Input, Modal, Select, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import ApiService from '../services/api';
import { categoryLabel, categoryNamesFromResponse } from '../constants/complaintCategories';

const { Option } = Select;

function ComplaintCategorySelect({ value, onChange, style, allowAdd = false, language = 'english' }) {
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

  const amharic = language === 'amharic';

  const saveCategory = async () => {
    const name = draft.trim();
    if (!name) {
      message.warning(amharic ? 'የአዲሱን የቅሬታ አይነት ስም ያስገቡ።' : 'Enter the new category name.');
      return;
    }
    setSaving(true);
    try {
      const saved = await ApiService.createComplaintCategory(name);
      const savedName = saved?.name || name;
      setCategories((current) => (current.includes(savedName) ? current : [...current, savedName]));
      onChange?.(savedName);
      message.success(amharic ? 'የቅሬታ አይነት ተጨምሯል።' : 'Category added.');
      closeAdd();
    } catch (err) {
      message.error(err?.message || (amharic ? 'የቅሬታ አይነቱን መጨመር አልተቻለም።' : 'Could not add the category.'));
      setSaving(false);
    }
  };

  return (
    <div>
      <Select value={value} style={style} onChange={onChange}>
        {options.map((name) => (
          <Option key={name} value={name}>{categoryLabel(name, language)}</Option>
        ))}
      </Select>
      {allowAdd && (
        <Button
          type="link"
          icon={<PlusOutlined />}
          onClick={() => setAdding(true)}
          style={{ paddingLeft: 0, height: 'auto', marginTop: 4 }}
        >
          {amharic ? 'የቅሬታ አይነት ጨምር' : 'Add Category'}
        </Button>
      )}
      <Modal
        title={amharic ? 'የቅሬታ አይነት ጨምር' : 'Add Category'}
        open={adding}
        onCancel={closeAdd}
        destroyOnClose
        footer={[
          <Button key="cancel" onClick={closeAdd} disabled={saving}>{amharic ? 'ይቅር' : 'Cancel'}</Button>,
          <Button key="save" type="primary" loading={saving} onClick={saveCategory}>{amharic ? 'አስቀምጥ' : 'Save'}</Button>
        ]}
      >
        <Input
          value={draft}
          maxLength={100}
          placeholder={amharic ? 'የአይነቱ ስም' : 'Category name'}
          onChange={(event) => setDraft(event.target.value)}
          onPressEnter={saveCategory}
        />
      </Modal>
    </div>
  );
}

export default ComplaintCategorySelect;
