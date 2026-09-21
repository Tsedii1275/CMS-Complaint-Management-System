import React, { useState, useEffect } from 'react';
import { Dropdown, Modal, Input, Button, Tag, DatePicker, message as antMessage } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { BRAND_COLORS } from '../constants/theme';
import ApiService from '../services/api';
import { normalizeComplaintStatus, renderComplaintStatusTag } from '../utils/statusUtils';
import moment from 'moment';

function sanitizePublicPhone(formPhone, countryCode) {
  let rawPhone = formPhone.trim();
  if (rawPhone.startsWith('0')) {
    rawPhone = rawPhone.substring(1);
  }
  if (rawPhone.startsWith(countryCode)) {
    rawPhone = rawPhone.substring(countryCode.length);
  } else if (rawPhone.startsWith('+')) {
    rawPhone = rawPhone.replace(/^\+\d+/, '');
  }
  return countryCode + rawPhone;
}

function getAccountNumberError(accountNumber, language) {
  if (!accountNumber?.trim()) {
    return language === 'english' ? 'Account Number is mandatory' : 'የአካውንት ቁጥር ግዴታ ነው';
  }
  if (accountNumber.length !== 13 || !/^\d+$/.test(accountNumber)) {
    return language === 'english' ? 'Account Number must be 13 digits' : 'አካውንት ቁጥር 13 አሃዝ መሆን አለበት';
  }
  return '';
}

function formatSubmissionDate(dateStr) {
  if (!dateStr) return '—';
  try {
    const d = moment(dateStr);
    return d.isValid() ? d.format('DD/MM/YY') : dateStr;
  } catch (e) {
    console.warn('Could not format submission date:', e);
    return dateStr;
  }
}

function getPublicStatusTag(action, language) {
  const norm = normalizeComplaintStatus(action);
  const AMHARIC_STATUS_MAP = {
    RECORDED: { label: 'ተመዝግቧል', color: 'purple' },
    ON_TRACK: { label: 'ሂደት ላይ ያለ', color: 'blue' },
    ESCALATED: { label: 'የተላለፈ', color: 'orange' },
    RESOLVED: { label: 'የተፈታ', color: 'green' },
    CLOSED: { label: 'የተዘጋ', color: 'default' },
    DECLINED: { label: 'ውድቅ የተደረገ', color: 'red' }
  };

  if (language === 'amharic') {
    const info = AMHARIC_STATUS_MAP[norm] || AMHARIC_STATUS_MAP.RECORDED;
    return (
      <Tag color={info.color} style={{ fontSize: '14px', padding: '4px 12px', borderRadius: '4px', border: 'none', fontWeight: 600 }}>
        {info.label}
      </Tag>
    );
  }

  return renderComplaintStatusTag(norm, { fontSize: '13px', padding: '4px 12px' });
}

function applyPublicFormInputChange(e, { setFormData, setErrors, language, errors }) {
  const { name, value } = e.target;
  setFormData(prev => ({
    ...prev,
    [name]: value
  }));

  if (errors[name]) {
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors[name];
      return newErrors;
    });
  }

  if (name !== 'accountNumber') {
    return;
  }
  const accountError = getAccountNumberError(value, language);
  if (accountError) {
    setErrors(prev => ({
      ...prev,
      accountNumber: accountError
    }));
  }
}

const EMPTY_PUBLIC_FORM = {
  customerName: '',
  email: '',
  phone: '',
  accountNumber: '',
  district: '',
  complaintCategory: 'Customer Service Issues',
  complaintDescription: '',
  branch: '',
  date: '',
  preferredContactMethod: 'Email'
};

async function submitPublicComplaint(e, ctx) {
  e?.preventDefault?.();
  const {
    formData, countryCode, language, consentChecked, t, evidenceUrl, evidenceName,
    setIsSubmitting, setMessageText, setMessageType, setErrors, setFormData, setConsentChecked,
    setEvidenceUrl, setEvidenceName
  } = ctx;
  setIsSubmitting(true);
  setMessageText('');

  const phone = sanitizePublicPhone(formData.phone, countryCode);

  const accountError = getAccountNumberError(formData.accountNumber, language);
  if (accountError) {
    setErrors(prev => ({
      ...prev,
      accountNumber: accountError
    }));
    setIsSubmitting(false);
    return;
  }

  if (!consentChecked) {
    setErrors(prev => ({ ...prev, consent: t.consentError }));
    setIsSubmitting(false);
    return;
  }

  try {
    const payload = {
      customer: {
        name: formData.customerName,
        email: formData.email,
        phone: phone,
        currentContactPhone: phone,
        accountNumber: formData.accountNumber,
        preferredLanguage: language
      },
      complaint: {
        channel: 'web',
        category: formData.complaintCategory,
        description: formData.complaintDescription,
        complaintBranch: formData.branch,
        complaintDistrict: formData.district,
        date: formData.date,
        preferredContactMethod: formData.preferredContactMethod,
        evidenceUrl: evidenceUrl || undefined,
        evidenceName: evidenceName || undefined
      }
    };

    console.log('Submitting complaint with payload:', payload);
    const response = await ApiService.submitComplaint(payload);

    const ticketId = response.ticketId || response.ticketNumber;
    const succText = language === 'english'
      ? `Your complaint has been submitted successfully. Unique ID No: ${ticketId}`
      : `ቅሬታዎ በተሳካ ሁኔታ ገብቷል። ልዩ መለያ ቁጥር: ${ticketId}`;
    antMessage.success(succText, 6);
    setMessageText(succText);
    setMessageType('success');

    setTimeout(() => {
      setMessageText('');
      setMessageType('');
    }, 5000);

    setFormData({ ...EMPTY_PUBLIC_FORM });
    setConsentChecked(false);
    setErrors({});
    setEvidenceUrl('');
    setEvidenceName('');

    console.log('Complaint submitted successfully:', response);
  } catch (error) {
    console.error('Error details:', error);
    let errorMessage = 'Failed to submit complaint. Please try again.';

    if (error.message) {
      errorMessage = `Error: ${error.message}`;
    }

    setMessageText(errorMessage);
    setMessageType('error');

    setTimeout(() => {
      setMessageText('');
      setMessageType('');
    }, 5000);
  } finally {
    setIsSubmitting(false);
  }
}

async function checkPublicComplaintStatus({ ticketSearch, language, t, setSearchError, setIsSearching, setSearchResult }) {
  if (!ticketSearch.trim()) {
    setSearchError(language === 'english' ? 'Please enter a ticket number' : 'እባክዎ የቲኬት ቁጥር ያስገቡ');
    return;
  }
  setIsSearching(true);
  setSearchError('');
  setSearchResult(null);

  try {
    const response = await ApiService.checkComplaintStatus(ticketSearch.trim());
    setSearchResult(response);
  } catch (error) {
    console.error('Status check error:', error);
    if (error.message?.includes('404')) {
      setSearchError(t.noTicketFound);
    } else {
      setSearchError(language === 'english' ? 'Failed to fetch status. Please try again.' : 'የቅሬታውን ሁኔታ ለማምጣት አልተቻለም። እባክዎ እንደገና ይሞክሩ።');
    }
  } finally {
    setIsSearching(false);
  }
}

async function onPublicEvidenceSelected(e, { setIsUploadingEvidence, setEvidenceUrl, setEvidenceName, setMessageType, setMessageText }) {
  const file = e.target.files[0];
  if (!file) {
    return;
  }
  setIsUploadingEvidence(true);
  try {
    const result = await ApiService.uploadEvidence(file);
    setEvidenceUrl(result.url);
    setEvidenceName(result.fileName);
  } catch (err) {
    console.error('Evidence upload failed:', err);
    setMessageType('error');
    setMessageText('Failed to upload evidence file. Please try again.');
  } finally {
    setIsUploadingEvidence(false);
  }
}

function publicEvidenceButtonLabel(t, isUploadingEvidence, evidenceUrl) {
  if (isUploadingEvidence) {
    return t.evidenceUploading;
  }
  if (evidenceUrl) {
    return t.evidenceChange;
  }
  return t.evidenceChoose;
}

function publicSubmitButtonLabel(t, isSubmitting, language) {
  if (isSubmitting) {
    return language === 'english' ? 'Submitting...' : 'በመልክያል...';
  }
  return t.submitButton;
}

function publicPageClassName(language) {
  return language === 'amharic' ? 'cms-public-page amharic-lightweight' : 'cms-public-page';
}

function syncEthiopianFormDate(language, ethMonth, ethDay, ethYear, setFormData) {
  if (language !== 'amharic') {
    return;
  }
  setFormData(prev => ({
    ...prev,
    date: `${ethMonth} ${ethDay}, ${ethYear} (Ethiopian)`
  }));
}

function applyEvidenceChooserHover(e, isUploadingEvidence, isHighlight) {
  if (isUploadingEvidence) {
    return;
  }
  e.currentTarget.style.borderColor = isHighlight ? BRAND_COLORS.primary : '#dcdcdc';
  e.currentTarget.style.color = isHighlight ? BRAND_COLORS.primary : '#444';
}

function applyTrackButtonHover(e, isHighlight) {
  e.currentTarget.style.backgroundColor = isHighlight ? BRAND_COLORS.primary : 'white';
  e.currentTarget.style.color = isHighlight ? 'white' : BRAND_COLORS.primary;
}

function applyConsentCheckedChange(checked, setConsentChecked, setErrors) {
  setConsentChecked(checked);
  if (!checked) {
    return;
  }
  setErrors(prev => {
    const next = { ...prev };
    delete next.consent;
    return next;
  });
}

function CustomerForm() {
  const [formData, setFormData] = useState({
    customerName: '',
    email: '',
    phone: '',
    accountNumber: '',
    district: '',
    complaintCategory: 'Customer Service Issues',
    complaintDescription: '',
    branch: '',
    date: '',
    preferredContactMethod: ''
  });
  const [consentChecked, setConsentChecked] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [messageText, setMessageText] = useState('');
  const [messageType, setMessageType] = useState('');
  const [language, setLanguage] = useState('english');
  const [errors, setErrors] = useState({});
  const [countryCode, setCountryCode] = useState('+251');

  // Check Status States
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [ticketSearch, setTicketSearch] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [searchError, setSearchError] = useState('');

  // Ethiopian Date states
  const [ethMonth, setEthMonth] = useState('መስከረም');
  const [ethDay, setEthDay] = useState('1');
  const [ethYear, setEthYear] = useState('2018');

  // Evidence file attachment states
  const [evidenceUrl, setEvidenceUrl] = useState('');
  const [evidenceName, setEvidenceName] = useState('');
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);

  const countryCodes = [
    { code: '+251', flag: '🇪🇹', name: 'Ethiopia' },
    { code: '+1', flag: '🇺🇸', name: 'USA' },
    { code: '+44', flag: '🇬🇧', name: 'UK' },
    { code: '+254', flag: '🇰🇪', name: 'Kenya' },
    { code: '+253', flag: '🇩ጂ', name: 'Djibouti' },
    { code: '+971', flag: '🇦🇪', name: 'UAE' },
    { code: '+966', flag: '🇸🇦', name: 'Saudi Arabia' }
  ];

  // Standardized 11 Categories List
  const STANDARDIZED_CATEGORIES = [
    'Customer Service Issues',
    'Transaction Error',
    'Account Management',
    'Banking App Issues',
    'Credit/Financing Concerns',
    'Fraud & Security Risk',
    'Information Disclosure',
    'ATM & Card Banking Issues',
    'Policy & Compliance Disputes',
    'System Failure',
    'Branch Operation'
  ];

  // Translations
  const translations = {
    english: {
      title: 'Complaint Management System',
      subtitle: 'Submit your complaint and we will handle it promptly',
      formTitle: 'Customer Complaint Form',
      customerName: 'Customer Name',
      email: 'Email Address',
      phoneNumber: 'Mobile number you currently use',
      phoneHelper: 'Please provide the phone number currently in use so we can contact you regarding your complaint.',
      accountNumber: 'Account Number',
      district: 'Complaint District',
      selectDistrict: '— Select or enter Complaint District —',
      complaintDescription: 'Details of the Complaint',
      complaintCategory: 'Complaint Category',
      branch: 'Complaint Branch',
      selectBranch: 'Complaint Branch name',

      complaintDate: 'Complaint Date',
      submitButton: 'Submit Complaint',
      checkStatusTitle: 'Check Your Complaint Status',
      checkStatusDesc: 'Have you already submitted a complaint? Enter your ticket number to track its current progress.',
      trackButton: 'Track Complaint',
      modalTitle: 'Check Complaint Status',
      ticketLabel: 'Complaint Ticket Number',
      checkStatusBtn: 'Check Status',
      noTicketFound: 'No complaint found with the provided ticket number.',
      ticketNumber: 'Ticket Number',
      statusCustomerName: 'Customer Name',
      submissionDate: 'Submission Date',
      currentStatus: 'Current Status',
      contactMethod: 'Preferred Contact Method',
      contactMethodOptions: [
        { value: 'SMS', label: 'SMS' },
        { value: 'Email', label: 'Email' },
        { value: 'Both', label: 'Both' }
      ],
      selectContactMethod: '— Select preferred contact method —',
      consentText: 'I confirm that the information provided is accurate and complete to the best of my knowledge. I consent to the use of this information for the purpose of investigating and resolving my complaint.',
      consentError: 'You must agree before submitting your complaint.',
      evidenceLabel: 'Evidence Attachment',
      evidenceHelper: 'Upload any relevant documents, screenshots, or files to support your complaint (PDF, images, etc.).',
      evidenceUploading: 'Uploading...',
      evidenceUploaded: 'File uploaded successfully',
      evidenceRemove: 'Remove',
      evidenceChoose: 'Choose File',
      evidenceChange: 'Change File',
      evidenceDragDrop: 'or drag and drop your file here'
    },
    amharic: {
      title: 'የደንበኞች የቅሬታ ማቅረቢያ ቅጽ',
      subtitle: 'ቅሬታዎን እዚህ ያቅርቡ፤ በፍጥነት መፍትሄ ለመስጠት እንጥራለን።',
      formTitle: 'የደንበኞች የቅሬታ ማቅረቢያ ቅጽ',
      customerName: 'የደንበኛው ሙሉ ስም',
      email: 'የኢሜይል አድራሻ',
      phoneNumber: 'አሁን የሚጠቀሙበት የሞባይል ስልክ ቁጥር',
      phoneHelper: 'ቅሬታዎን በተመለከተ እንድንገናኝዎት እባክዎ በአሁኑ ጊዜ አገልግሎት ላይ ያለውን ስልክ ቁጥር ያቅርቡ።',
      accountNumber: 'የአካውንት ቁጥር',
      district: 'የቅሬታ ዲስትሪክት',
      selectDistrict: 'የቅሬታ ዲስትሪክት ይምረጡ ወይም ያስገቡ',
      complaintDescription: 'የቅሬታው ዝርዝር መግለጫ',
      complaintCategory: 'የቅሬታ አይነት',
      branch: 'የቅሬታ ቅርንጫፍ',
      selectBranch: 'የቅሬታ ቅርንጫፍ ይምረጡ ወይም ያስገቡ',
      complaintDate: 'የቅሬታ ቀን',
      submitButton: 'ቅሬታውን ያስገቡ',
      checkStatusTitle: 'የቅሬታዎን ሁኔታ ያረጋግጡ',
      checkStatusDesc: 'ቅሬታ አስገብተዋል? የቅሬታዎን ሂደት ለመከታተል የቲኬት ቁጥርዎን ያስገቡ።',
      trackButton: 'ቅሬታ ይከታተሉ',
      modalTitle: 'የቅሬታ ሁኔታ ማረጋገጫ',
      ticketLabel: 'የቅሬታ ቲኬት ቁጥር',
      checkStatusBtn: 'ሁኔታውን እይ',
      noTicketFound: 'በተጠቀሰው የቲኬት ቁጥር የተመዘገበ ቅሬታ አልተገኘም።',
      ticketNumber: 'የቲኬት ቁጥር',
      statusCustomerName: 'የደንበኛው ስም',
      submissionDate: 'የገባበት ቀን',
      currentStatus: 'የአሁኑ ሁኔታ',
      contactMethod: 'ተመራጭ የመገናኛ ዘዴ',
      contactMethodOptions: [
        { value: 'SMS', label: 'ኤስኤምኤስ' },
        { value: 'Email', label: 'ኢሜይል ' },
        { value: 'Both', label: 'ሁለቱም' }
      ],
      selectContactMethod: '— ተመራጭ የመገናኛ ዘዴ ይምረጡ —',
      consentText: 'እኔ የቀረበው መረጃ ለእኔ እውቀት ትክክለኛ እና የተሟላ መሆኑን አረጋግጣለሁ። ይህ መረጃ ቅሬታዬን ለመመርመር እና ለመፍታት ጥቅም ላይ እንዲውል እስማማለሁ።',
      consentError: 'ቅሬታዎን ከማስገባትዎ በፊት መስማማት አለብዎት።',
      evidenceLabel: 'ማስረጃ ማያያዣ (አማራጭ)',
      evidenceHelper: 'ቅሬታዎን የሚደግፉ ማናቸውንም ሰነዶች፣ ምስሎች ወይም ፋይሎች ያያይዙ።',
      evidenceUploading: 'በመስቀል ላይ...',
      evidenceUploaded: 'ፋይል በተሳካ ሁኔታ ተሰቅሏል',
      evidenceRemove: 'አስወግድ',
      evidenceChoose: 'ፋይል ይምረጡ',
      evidenceChange: 'ፋይል ይቀይሩ',
      evidenceDragDrop: 'ወይም ፋይልዎን እዚህ ይጣሉ'
    }
  };

  const t = translations[language];

  // Language menu items
  const languageMenuItems = [
    {
      key: 'english',
      label: 'English',
      icon: <span>🇺🇸</span>,
      onClick: () => setLanguage('english')
    },
    {
      key: 'amharic',
      label: 'አማርኛ',
      icon: <span>🇪🇹</span>,
      onClick: () => setLanguage('amharic')
    }
  ];

  // Prefetch hierarchy so district/branch data is available to the public form.
  useEffect(() => {
    ApiService.getHierarchy().catch(err => console.error('Failed to load hierarchy:', err));
  }, []);

  // Synchronize Ethiopian date states to formData.date
  useEffect(() => {
    syncEthiopianFormDate(language, ethMonth, ethDay, ethYear, setFormData);
  }, [ethMonth, ethDay, ethYear, language]);

  const handleInputChange = (e) => {
    applyPublicFormInputChange(e, { setFormData, setErrors, language, errors });
  };

  const handleSubmit = async (e) => {
    await submitPublicComplaint(e, {
      formData, countryCode, language, consentChecked, t, evidenceUrl, evidenceName,
      setIsSubmitting, setMessageText, setMessageType, setErrors, setFormData, setConsentChecked,
      setEvidenceUrl, setEvidenceName
    });
  };

  const handleCheckStatus = async () => {
    await checkPublicComplaintStatus({
      ticketSearch, language, t, setSearchError, setIsSearching, setSearchResult
    });
  };

  const getStatusTag = (action) => getPublicStatusTag(action, language);

  const evidenceButtonLabel = publicEvidenceButtonLabel(t, isUploadingEvidence, evidenceUrl);
  const submitButtonLabel = publicSubmitButtonLabel(t, isSubmitting, language);

  const highlightEvidenceChooser = (e) => applyEvidenceChooserHover(e, isUploadingEvidence, true);
  const resetEvidenceChooser = (e) => applyEvidenceChooserHover(e, isUploadingEvidence, false);
  const highlightTrackButton = (e) => applyTrackButtonHover(e, true);
  const resetTrackButton = (e) => applyTrackButtonHover(e, false);

  return (
    <div
      className={publicPageClassName(language)}
      style={{ minHeight: '100vh', backgroundColor: '#fcfcfc', display: 'flex', flexDirection: 'column' }}
    >
      {/* Header - Left Aligned */}
      <div className="cms-public-header" style={{
        backgroundColor: '#fff',
        color: BRAND_COLORS.primary,
        padding: '40px 80px 20px 80px',
        position: 'relative',
        borderBottom: '2px solid #012169'
      }}>
        {/* Language Selector */}
        <div className="cms-public-lang" style={{
          position: 'absolute',
          top: '40px',
          right: '80px',
          zIndex: 10
        }}>
          <Dropdown
            menu={{ items: languageMenuItems }}
            placement="bottomRight"
            trigger={['click']}
          >
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 12px',
              backgroundColor: 'rgba(0,0,0,0.05)',
              borderRadius: '4px',
              cursor: 'pointer',
              border: '1px solid rgba(0,0,0,0.1)',
              transition: 'all 0.3s ease'
            }}>
              <GlobalOutlined style={{ fontSize: '16px', color: BRAND_COLORS.primary }} />
              <span style={{ fontSize: '14px', color: BRAND_COLORS.primary, fontWeight: 500 }}>
                {language === 'english' ? 'English' : 'አማርኛ'}
              </span>
            </div>
          </Dropdown>
        </div>

        <div className="cms-public-brand" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', gap: '20px', marginBottom: '16px' }}>
          <img
            src="/download.png"
            alt="Dashen Bank Logo"
            style={{ height: '60px', width: 'auto' }}
          />
          <div>
            <h1 className="cms-public-title" style={{ margin: 0, fontSize: '28px', fontWeight: '800', color: BRAND_COLORS.primary, letterSpacing: '-0.5px' }}>
              {t.subtitle}
            </h1>
          </div>
        </div>
      </div>

      {/* Main Content - Two Column Layout */}
      <div className="cms-public-body" style={{
        flex: 1,
        padding: '40px 80px',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'flex-start',
        gap: '40px',
        flexWrap: 'wrap',
        maxWidth: '1300px',
        margin: '0 auto',
        width: '100%'
      }}>
        {/* Left Column - Main Form */}
        <div className="cms-public-form-card" style={{
          flex: '3 1 600px',
          backgroundColor: '#fff',
          padding: '40px',
          border: '1px solid #e0e0e0',
          borderRadius: '2px',
          boxShadow: 'none',
          boxSizing: 'border-box'
        }}>
          <h2 style={{ textAlign: 'left', marginBottom: '40px', color: BRAND_COLORS.primary, fontSize: '24px', fontWeight: 700, borderBottom: '1px solid #f0f0f0', paddingBottom: '15px' }}>
            {t.formTitle}
          </h2>

          {messageText && (
            <div style={{
              padding: '12px',
              marginBottom: '20px',
              backgroundColor: messageType === 'success' ? '#d4edda' : '#f8d7da',
              color: messageType === 'success' ? '#155724' : '#721c24',
              border: `1px solid ${messageType === 'success' ? '#c3e6cb' : '#f5c6cb'}`,
              borderRadius: '6px'
            }}>
              {messageText}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Row 1: Name and Email */}
            <div className="cms-form-row" style={{ display: 'flex', gap: '32px', marginBottom: '32px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.customerName} <span style={{ color: 'red' }}>*</span>
                </label>
                <input
                  type="text"
                  name="customerName"
                  value={formData.customerName}
                  onChange={handleInputChange}
                  required
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '1px solid #dcdcdc',
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.email}
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '1px solid #dcdcdc',
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>
            </div>

            {/* Row 2: Phone and Date */}
            <div className="cms-form-row" style={{ display: 'flex', gap: '32px', marginBottom: '32px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.phoneNumber} <span style={{ color: 'red' }}>*</span>
                </label>
                <div style={{ display: 'flex' }}>
                  <select
                    value={countryCode}
                    onChange={(e) => setCountryCode(e.target.value)}
                    style={{
                      padding: '12px',
                      border: '1px solid #dcdcdc',
                      borderRight: 'none',
                      borderRadius: '4px 0 0 4px',
                      fontSize: '15px',
                      backgroundColor: '#f8f9fa',
                      outline: 'none',
                      cursor: 'pointer',
                      width: '110px',
                      color: BRAND_COLORS.primary,
                      fontWeight: 500
                    }}
                  >
                    {countryCodes.map((c) => (
                      <option key={c.code} value={c.code}>
                        {c.flag} {c.code}
                      </option>
                    ))}
                  </select>
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    required
                    style={{
                      flex: 1,
                      padding: '12px 16px',
                      border: '1px solid #dcdcdc',
                      borderRadius: '0 4px 4px 0',
                      fontSize: '15px',
                      outline: 'none',
                      transition: 'border-color 0.2s'
                    }}
                  />
                </div>
              </div>

              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.complaintDate} <span style={{ color: 'red' }}>*</span>
                </label>
                {language === 'english' ? (
                  <DatePicker
                    format="DD/MM/YYYY"
                    placeholder="DD/MM/YYYY"
                    value={formData.date ? moment(formData.date, 'YYYY-MM-DD') : null}
                    onChange={(date) => {
                      setFormData(prev => ({
                        ...prev,
                        date: date ? date.format('YYYY-MM-DD') : ''
                      }));
                    }}
                    style={{
                      width: '100%',
                      height: '45px',
                      borderRadius: '4px',
                      fontSize: '15px'
                    }}
                  />
                ) : (
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <select
                      value={ethMonth}
                      onChange={(e) => setEthMonth(e.target.value)}
                      style={{
                        flex: 2,
                        padding: '12px 8px',
                        border: '1px solid #dcdcdc',
                        borderRadius: '4px',
                        fontSize: '14px',
                        backgroundColor: 'white',
                        outline: 'none'
                      }}
                    >
                      <option value="መስከረም">መስከረም</option>
                      <option value="ጥቅምት">ጥቅምት</option>
                      <option value="ኅዳር">ኅዳር</option>
                      <option value="ታኅሣሥ">ታኅሣሥ</option>
                      <option value="ጥር">ጥር</option>
                      <option value="የካቲት">የካቲት</option>
                      <option value="መጋቢት">መጋቢት</option>
                      <option value="ሚያዝያ">ሚያዝያ</option>
                      <option value="ግንቦት">ግንቦት</option>
                      <option value="ሰኔ">ሰኔ</option>
                      <option value="ሐምሌ">ሐምሌ</option>
                      <option value="ነሐሴ">ነሐሴ</option>
                      <option value="ጳጉሜ">ጳጉሜ</option>
                    </select>
                    <select
                      value={ethDay}
                      onChange={(e) => setEthDay(e.target.value)}
                      style={{
                        flex: 1.2,
                        padding: '12px 8px',
                        border: '1px solid #dcdcdc',
                        borderRadius: '4px',
                        fontSize: '14px',
                        backgroundColor: 'white',
                        outline: 'none'
                      }}
                    >
                      {Array.from({ length: ethMonth === 'ጳጉሜ' ? 6 : 30 }, (_, i) => String(i + 1)).map(day => (
                        <option key={day} value={day}>{day}</option>
                      ))}
                    </select>
                    <select
                      value={ethYear}
                      onChange={(e) => setEthYear(e.target.value)}
                      style={{
                        flex: 1.5,
                        padding: '12px 8px',
                        border: '1px solid #dcdcdc',
                        borderRadius: '4px',
                        fontSize: '14px',
                        backgroundColor: 'white',
                        outline: 'none'
                      }}
                    >
                      <option value="2018">2018</option>
                      <option value="2017">2017</option>
                      <option value="2016">2016</option>
                      <option value="2015">2015</option>
                    </select>
                  </div>
                )}
              </div>
            </div>

            {/* Row 3: Account Number, District, Branch */}
            <div className="cms-form-row" style={{ display: 'flex', gap: '32px', marginBottom: '32px', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 180px' }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.accountNumber} <span style={{ color: 'red' }}>*</span>
                </label>
                <input
                  type="text"
                  name="accountNumber"
                  value={formData.accountNumber}
                  onChange={handleInputChange}
                  required
                  maxLength={13}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: `1px solid ${errors.accountNumber ? '#ff4d4f' : '#dcdcdc'}`,
                    borderRadius: '4px',
                    fontSize: '15px',
                    transition: 'border-color 0.2s',
                    outline: 'none'
                  }}
                />
                {errors.accountNumber && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.accountNumber}
                  </div>
                )}
              </div>

              <div style={{ flex: '1 1 180px' }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.district}
                </label>
                <input
                  type="text"
                  name="district"
                  value={formData.district}
                  onChange={handleInputChange}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '1px solid #dcdcdc',
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    backgroundColor: 'white',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>

              <div style={{ flex: '1 1 180px' }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.branch}
                </label>
                <input
                  type="text"
                  name="branch"
                  value={formData.branch}
                  onChange={handleInputChange}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: '1px solid #dcdcdc',
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    backgroundColor: 'white',
                    transition: 'border-color 0.2s'
                  }}
                />
              </div>
            </div>

            {/* Row 4: Category */}
            <div style={{ marginBottom: '32px' }}>
              <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                {t.complaintCategory} <span style={{ color: 'red' }}>*</span>
              </label>
              <select
                name="complaintCategory"
                value={formData.complaintCategory}
                onChange={handleInputChange}
                required
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '1px solid #dcdcdc',
                  borderRadius: '4px',
                  fontSize: '15px',
                  backgroundColor: 'white',
                  outline: 'none',
                  cursor: 'pointer'
                }}
              >
                {STANDARDIZED_CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            {/* Row 5: Description */}
            <div style={{ marginBottom: '32px' }}>
              <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                {t.complaintDescription} <span style={{ color: 'red' }}>*</span>
              </label>
              <textarea
                name="complaintDescription"
                value={formData.complaintDescription}
                onChange={handleInputChange}
                required
                rows={5}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '1px solid #dcdcdc',
                  borderRadius: '4px',
                  fontSize: '15px',
                  resize: 'vertical',
                  outline: 'none',
                  transition: 'border-color 0.2s'
                }}
              />
            </div>

            {/* Row 6: Evidence Attachment (Optional) */}
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                {t.evidenceLabel}
              </label>
              <p style={{ margin: '0 0 12px 0', fontSize: '13px', color: '#888' }}>
                {t.evidenceHelper}
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <input
                  id="evidence-file-input"
                  type="file"
                  accept=".pdf,.png,.jpg,.jpeg,.gif,.doc,.docx,.xls,.xlsx,.txt,.zip"
                  style={{ display: 'none' }}
                  onChange={(e) => onPublicEvidenceSelected(e, {
                    setIsUploadingEvidence, setEvidenceUrl, setEvidenceName, setMessageType, setMessageText
                  })}
                />
                <button
                  type="button"
                  onClick={() => document.getElementById('evidence-file-input').click()}
                  disabled={isUploadingEvidence}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#fff',
                    color: '#444',
                    border: '1px solid #dcdcdc',
                    borderRadius: '4px',
                    cursor: isUploadingEvidence ? 'not-allowed' : 'pointer',
                    opacity: isUploadingEvidence ? 0.7 : 1,
                    fontSize: '14px',
                    fontWeight: 500,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    transition: 'all 0.2s'
                  }}
                  onMouseOver={highlightEvidenceChooser}
                  onFocus={highlightEvidenceChooser}
                  onMouseOut={resetEvidenceChooser}
                  onBlur={resetEvidenceChooser}
                >
                  <span style={{ fontSize: '16px' }}>📎</span>
                  {evidenceButtonLabel}
                </button>
                {evidenceName && (
                  <span style={{ color: '#166534', fontSize: '14px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '4px' }}>
                    {evidenceName}
                  </span>
                )}
                {evidenceUrl && (
                  <button
                    type="button"
                    onClick={() => {
                      setEvidenceUrl('');
                      setEvidenceName('');
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#dc2626',
                      cursor: 'pointer',
                      fontSize: '18px',
                      padding: '4px'
                    }}
                    title={t.evidenceRemove}
                  >
                    ×
                  </button>
                )}
              </div>

            </div>

            {/* Preferred Contact Method */}
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                {t.contactMethod} <span style={{ color: 'red' }}>*</span>
              </label>
              <select
                name="preferredContactMethod"
                value={formData.preferredContactMethod}
                onChange={handleInputChange}
                required
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '1px solid #dcdcdc',
                  borderRadius: '4px',
                  fontSize: '15px',
                  backgroundColor: 'white',
                  outline: 'none',
                  cursor: 'pointer',
                  color: formData.preferredContactMethod ? '#222' : '#aaa',
                  appearance: 'auto'
                }}
              >
                <option value="" disabled>{t.selectContactMethod}</option>
                {t.contactMethodOptions.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>

            {/* Consent Checkbox */}
            <div style={{ marginBottom: '32px', padding: '16px', backgroundColor: '#f8f9fa', borderRadius: '6px', border: '1px solid #e9ecef' }}>
              <label style={{ display: 'flex', gap: '12px', alignItems: 'flex-start', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={consentChecked}
                  onChange={(e) => applyConsentCheckedChange(e.target.checked, setConsentChecked, setErrors)}
                  style={{ accentColor: '#012169', width: '18px', height: '18px', marginTop: '2px', cursor: 'pointer', flexShrink: 0 }}
                />
                <span style={{ fontSize: '13px', color: '#444', lineHeight: 1.6 }}>
                  {t.consentText} <span style={{ color: 'red' }}>*</span>
                </span>
              </label>
              {errors.consent && (
                <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '8px', marginLeft: '30px' }}>
                  {errors.consent}
                </div>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                backgroundColor: BRAND_COLORS.primary,
                color: 'white',
                padding: '12px 24px',
                border: 'none',
                borderRadius: '6px',
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                opacity: isSubmitting ? 0.7 : 1,
                fontSize: '16px',
                fontWeight: '500',
                width: '100%',
                transition: 'all 0.3s ease'
              }}
            >
              {submitButtonLabel}
            </button>
          </form>
        </div>

        {/* Right Column - Status Checker Card */}
        <div className="cms-public-side-card" style={{
          flex: '1 1 300px',
          backgroundColor: '#fff',
          padding: '30px',
          border: '1px solid #e0e0e0',
          borderRadius: '2px',
          boxSizing: 'border-box',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: BRAND_COLORS.primary }}>
            {t.checkStatusTitle}
          </h3>
          <p style={{ margin: 0, color: '#666', fontSize: '14px', lineHeight: 1.6 }}>
            {t.checkStatusDesc}
          </p>
          <button
            type="button"
            onClick={() => setIsStatusModalOpen(true)}
            style={{
              backgroundColor: 'white',
              color: BRAND_COLORS.primary,
              border: `2px solid ${BRAND_COLORS.primary}`,
              padding: '12px 20px',
              borderRadius: '4px',
              fontSize: '15px',
              fontWeight: '600',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              width: '100%',
              textAlign: 'center'
            }}
            onMouseOver={highlightTrackButton}
            onFocus={highlightTrackButton}
            onMouseOut={resetTrackButton}
            onBlur={resetTrackButton}
          >
            {t.trackButton}
          </button>
        </div>

        {/* Modal for checking status */}
        <Modal
          title={
            <div style={{ fontSize: '20px', fontWeight: 700, color: BRAND_COLORS.primary, paddingBottom: '10px', borderBottom: '1px solid #f0f0f0' }}>
              {t.modalTitle}
            </div>
          }
          open={isStatusModalOpen}
          onCancel={() => {
            setIsStatusModalOpen(false);
            setTicketSearch('');
            setSearchResult(null);
            setSearchError('');
          }}
          footer={null}
          width="100%"
          style={{ maxWidth: 500 }}
          centered
        >
          <div style={{ marginTop: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
              {t.ticketLabel}
            </label>
            <div className="cms-form-row" style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
              <Input
                placeholder="e.g. DBC-202606170001-AB12CD34"
                value={ticketSearch}
                onChange={(e) => setTicketSearch(e.target.value)}
                onPressEnter={handleCheckStatus}
                style={{ flex: 1, padding: '10px 16px', fontSize: '15px', borderRadius: '4px' }}
              />
              <Button
                type="primary"
                onClick={handleCheckStatus}
                loading={isSearching}
                style={{
                  backgroundColor: BRAND_COLORS.primary,
                  borderColor: BRAND_COLORS.primary,
                  height: 'auto',
                  padding: '10px 24px',
                  fontSize: '15px',
                  fontWeight: '600',
                  borderRadius: '4px'
                }}
              >
                {t.checkStatusBtn}
              </Button>
            </div>

            {searchError && (
              <div style={{
                padding: '12px',
                backgroundColor: '#f8d7da',
                color: '#721c24',
                border: '1px solid #f5c6cb',
                borderRadius: '4px',
                fontSize: '14px',
                marginBottom: '20px',
                fontWeight: 500
              }}>
                {searchError}
              </div>
            )}

            {searchResult && (
              <div style={{
                padding: '24px',
                backgroundColor: '#f8f9fa',
                border: '1px solid #e9ecef',
                borderRadius: '6px',
                display: 'flex',
                flexDirection: 'column',
                gap: '16px'
              }}>
                <div>
                  <span style={{ display: 'block', color: '#888', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    {t.ticketNumber}
                  </span>
                  <span style={{ fontSize: '15px', fontWeight: 700, color: BRAND_COLORS.primary }}>
                    {searchResult.ticketNumber}
                  </span>
                </div>

                <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
                  <div style={{ flex: '1 1 180px' }}>
                    <span style={{ display: 'block', color: '#888', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                      {t.statusCustomerName}
                    </span>
                    <span style={{ fontSize: '15px', fontWeight: 500, color: '#333' }}>
                      {searchResult.customerName}
                    </span>
                  </div>
                  <div style={{ flex: '1 1 180px' }}>
                    <span style={{ display: 'block', color: '#888', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                      {t.submissionDate}
                    </span>
                    <span style={{ fontSize: '15px', fontWeight: 500, color: '#333' }}>
                      {formatSubmissionDate(searchResult.submissionDate)}
                    </span>
                  </div>
                </div>

                <div>
                  <span style={{ display: 'block', color: '#888', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '6px' }}>
                    {t.currentStatus}
                  </span>
                  {getStatusTag(searchResult.currentStatus)}
                </div>
              </div>
            )}
          </div>
        </Modal>
      </div>
    </div>
  );
}

export default CustomerForm;
