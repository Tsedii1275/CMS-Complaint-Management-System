import React, { useState, useEffect } from 'react';
import { Dropdown, Modal, Input, Button, Tag, DatePicker, message as antMessage } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { BRAND_COLORS } from '../constants/theme';
import ApiService from '../services/api';
import { categoryNamesFromResponse } from '../constants/complaintCategories';
import { normalizeComplaintStatus, renderComplaintStatusTag } from '../utils/statusUtils';
import moment from 'moment';
import {
  sanitizePublicPhone,
  getCustomerNameError,
  getDescriptionError,
  getAccountNumberError,
  lettersOnlyPlaceName,
  getPlaceNameError,
  getEmailError,
  PUBLIC_COUNTRY_PHONES,
  publicPhoneRule,
  getPhoneError
} from '../utils/complaintFieldValidation';

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

function applyPublicFormInputChange(e, { setFormData, setErrors, language, errors, countryCode }) {
  const { name, value } = e.target;

  if (name === 'phone') {
    let cleanDigits = value.replace(/\D/g, '');
    const maxLen = publicPhoneRule(countryCode).maxDigits;
    if (cleanDigits.length > maxLen) {
      cleanDigits = cleanDigits.substring(0, maxLen);
    }
    setFormData(prev => ({
      ...prev,
      phone: cleanDigits
    }));

    const phoneErr = getPhoneError(cleanDigits, countryCode, language);
    setErrors(prev => {
      const next = { ...prev };
      if (phoneErr) {
        next.phone = phoneErr;
      } else {
        delete next.phone;
      }
      return next;
    });
    return;
  }

  const nextValue = name === 'district' || name === 'branch' ? lettersOnlyPlaceName(value) : value;

  setFormData(prev => ({
    ...prev,
    [name]: nextValue
  }));

  if (errors[name]) {
    setErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors[name];
      return newErrors;
    });
  }

  if (name === 'customerName') {
    const nameErr = getCustomerNameError(value, language);
    setErrors(prev => {
      const next = { ...prev };
      if (nameErr) next.customerName = nameErr;
      else delete next.customerName;
      return next;
    });
  }

  if (name === 'accountNumber') {
    const accountError = getAccountNumberError(value, language);
    setErrors(prev => {
      const next = { ...prev };
      if (accountError) next.accountNumber = accountError;
      else delete next.accountNumber;
      return next;
    });
  }

  if (name === 'complaintDescription') {
    const descErr = getDescriptionError(value, language);
    setErrors(prev => {
      const next = { ...prev };
      if (descErr) next.complaintDescription = descErr;
      else delete next.complaintDescription;
      return next;
    });
  }

  if (name === 'district' || name === 'branch') {
    const placeErr = getPlaceNameError(nextValue, language, name === 'district' ? 'Complaint District' : 'Complaint Branch');
    setErrors(prev => {
      const next = { ...prev };
      if (placeErr) next[name] = placeErr;
      else delete next[name];
      return next;
    });
  }

  if (name === 'email') {
    const emailErr = getEmailError(value, language);
    setErrors(prev => {
      const next = { ...prev };
      if (emailErr) next.email = emailErr;
      else delete next.email;
      return next;
    });
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
  preferredContactMethod: 'SMS'
};

function collectPublicSubmitErrors(formData, countryCode, language) {
  const next = {};
  const customerName = getCustomerNameError(formData.customerName, language);
  const complaintDescription = getDescriptionError(formData.complaintDescription, language);
  const accountNumber = getAccountNumberError(formData.accountNumber, language);
  const email = getEmailError(formData.email, language);
  const phone = getPhoneError(formData.phone, countryCode, language);
  const district = getPlaceNameError(formData.district, language, 'Complaint District');
  const branch = getPlaceNameError(formData.branch, language, 'Complaint Branch');
  if (customerName) next.customerName = customerName;
  if (complaintDescription) next.complaintDescription = complaintDescription;
  if (accountNumber) next.accountNumber = accountNumber;
  if (email) next.email = email;
  if (phone) next.phone = phone;
  if (district) next.district = district;
  if (branch) next.branch = branch;
  return next;
}

function isFutureComplaintDate(dateStr) {
  if (!dateStr) {
    return false;
  }
  const parsed = moment(dateStr, ['YYYY-MM-DD', 'DD/MM/YYYY']);
  return parsed.isValid() && parsed.isAfter(moment().endOf('day'));
}

function publicSubmitErrorMessage(error, language) {
  const fallback = language === 'english'
    ? 'Failed to submit complaint. Please try again.'
    : 'ቅሬታውን ማስገባት አልተሳካም። እባክዎ እንደገና ይሞክሩ።';
  const raw = String(error?.message || '');
  const lower = raw.toLowerCase();
  if (raw.length > 0 && raw.length < 180 && !lower.includes('jdbc') && !lower.includes('sql [')) {
    return `Error: ${raw}`;
  }
  return fallback;
}

function isComplaintDateDisabled(current) {
  return current?.isAfter(moment().endOf('day'));
}

async function submitPublicComplaint(e, ctx) {
  e?.preventDefault?.();
  const {
    formData, countryCode, language, consentChecked, t, evidenceFiles,
    setIsSubmitting, setMessageText, setMessageType, setErrors, setFormData, setConsentChecked,
    setEvidenceFiles, setEvidenceUrl, setEvidenceName
  } = ctx;
  setIsSubmitting(true);
  setMessageText('');

  const phone = sanitizePublicPhone(formData.phone, countryCode);
  const fieldErrors = collectPublicSubmitErrors(formData, countryCode, language);

  if (Object.keys(fieldErrors).length > 0) {
    setErrors(prev => ({ ...prev, ...fieldErrors }));
    antMessage.error(language === 'english' ? 'Please fix the errors in the form before submitting.' : 'እባክዎን ከማስገባትዎ በፊት በቅጹ ላይ ያሉትን ስህተቶች ያርሙ።');
    setIsSubmitting(false);
    return;
  }

  if (!consentChecked) {
    setErrors(prev => ({ ...prev, consent: t.consentError }));
    setIsSubmitting(false);
    return;
  }

  if (isFutureComplaintDate(formData.date)) {
    antMessage.error(language === 'english' ? 'Complaint date cannot be in the future' : 'የቅሬታ ቀን የወደፊት መሆን አይችልም');
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
        preferredLanguage: language,
        consent: true
      },
      complaint: {
        channel: 'web',
        category: formData.complaintCategory,
        description: formData.complaintDescription,
        complaintBranch: formData.branch,
        complaintDistrict: formData.district,
        date: formData.date,
        preferredContactMethod: formData.preferredContactMethod,
        evidenceUrl: evidenceFiles[0]?.url || undefined,
        evidenceName: evidenceFiles[0]?.name || undefined,
        evidenceFiles: evidenceFiles.length > 0 ? evidenceFiles : undefined
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
    setEvidenceFiles([]);

    console.log('Complaint submitted successfully:', response);
  } catch (error) {
    console.error('Error details:', error);
    setMessageText(publicSubmitErrorMessage(error, language));
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

const SUPPORTED_EVIDENCE_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png', 'docx', 'xlsx', 'mp3', 'wav', 'm4a'];

function evidenceFileExtension(fileName) {
  const parts = String(fileName || '').toLowerCase().split('.');
  return parts.length > 1 ? parts.pop() : '';
}

function unsupportedEvidenceMessage(language) {
  if (language === 'amharic') {
    return 'የሚደገፉ ፋይሎች፦ PDF, JPG, JPEG, PNG, DOCX, XLSX, MP3, WAV, እና M4A። ሰነድ እና ምስል እስከ 10 MB። ድምጽ እስከ 25 MB።';
  }
  return 'Supported files: PDF, JPG, JPEG, PNG, DOCX, XLSX, MP3, WAV, and M4A. Documents and images up to 10 MB. Audio up to 25 MB.';
}

function showUnsupportedEvidencePopup(language) {
  Modal.error({
    title: language === 'amharic' ? 'ይህ ፋይል አይደገፍም' : 'This file is unsupported',
    content: unsupportedEvidenceMessage(language),
    okText: language === 'amharic' ? 'እሺ' : 'OK'
  });
}

function evidenceUploadErrorMessage(err, language) {
  const serverMessage = err?.message || '';
  if (/unsupported|not permitted/i.test(serverMessage)) {
    return unsupportedEvidenceMessage(language);
  }
  if (serverMessage && !serverMessage.startsWith('Upload failed!')) {
    return serverMessage;
  }
  return language === 'amharic'
    ? 'ማስረጃውን መስቀል አልተቻለም። እባክዎ እንደገና ይሞክሩ።'
    : 'Failed to upload evidence file. Please try again.';
}

async function onPublicEvidenceSelected(e, { language, setIsUploadingEvidence, setEvidenceFiles, setEvidenceUrl, setEvidenceName, setMessageType, setMessageText }) {
  const selected = Array.from(e.target.files || []);
  e.target.value = '';
  if (selected.length === 0) {
    return;
  }
  const unsupported = selected.find((file) => !SUPPORTED_EVIDENCE_EXTENSIONS.includes(evidenceFileExtension(file.name)));
  if (unsupported) {
    setMessageType('');
    setMessageText('');
    showUnsupportedEvidencePopup(language);
    return;
  }
  setIsUploadingEvidence(true);
  try {
    const uploaded = [];
    for (const file of selected) {
      const result = await ApiService.uploadAttachment(file);
      uploaded.push({ name: result.fileName || file.name, url: result.url });
    }
    setEvidenceFiles((prev) => {
      const next = [...prev, ...uploaded];
      setEvidenceUrl(next[0]?.url || '');
      setEvidenceName(next.map((file) => file.name).join(', '));
      return next;
    });
    setMessageType('');
    setMessageText('');
  } catch (err) {
    console.error('Evidence upload failed:', err);
    const message = evidenceUploadErrorMessage(err, language);
    if (message === unsupportedEvidenceMessage(language)) {
      setMessageType('');
      setMessageText('');
      showUnsupportedEvidencePopup(language);
    } else {
      setMessageType('error');
      setMessageText(message);
    }
  } finally {
    setIsUploadingEvidence(false);
  }
}

function publicEvidenceButtonLabel(t, isUploadingEvidence, evidenceFiles) {
  if (isUploadingEvidence) {
    return t.evidenceUploading;
  }
  if (evidenceFiles.length > 0) {
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

function publicFieldBorder(hasError) {
  return `1px solid ${hasError ? '#ff4d4f' : '#dcdcdc'}`;
}

function publicMessageBannerStyle(messageType) {
  const ok = messageType === 'success';
  return {
    padding: '12px',
    marginBottom: '20px',
    backgroundColor: ok ? '#d4edda' : '#f8d7da',
    color: ok ? '#155724' : '#721c24',
    border: `1px solid ${ok ? '#c3e6cb' : '#f5c6cb'}`
  };
}

function bindPublicFormHandlers(ctx) {
  return {
    onSubmit: (e) => submitPublicComplaint(e, ctx),
    onInputChange: (e) => applyPublicFormInputChange(e, ctx),
    onCheckStatus: () => checkPublicComplaintStatus(ctx),
    statusTag: (action) => getPublicStatusTag(action, ctx.language),
    highlightEvidenceChooser: (e) => applyEvidenceChooserHover(e, ctx.isUploadingEvidence, true),
    resetEvidenceChooser: (e) => applyEvidenceChooserHover(e, ctx.isUploadingEvidence, false),
    highlightTrackButton: (e) => applyTrackButtonHover(e, true),
    resetTrackButton: (e) => applyTrackButtonHover(e, false)
  };
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
    preferredContactMethod: 'SMS'
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
  const [evidenceFiles, setEvidenceFiles] = useState([]);
  const [isUploadingEvidence, setIsUploadingEvidence] = useState(false);

  const countryCodes = PUBLIC_COUNTRY_PHONES;

  const [complaintCategories, setComplaintCategories] = useState(() => categoryNamesFromResponse(null));

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
      evidenceUploading: 'Uploading...',
      evidenceUploaded: 'File uploaded successfully',
      evidenceRemove: 'Remove',
      evidenceChoose: 'Choose Files',
      evidenceChange: 'Add Files',
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

  useEffect(() => {
    let cancelled = false;
    ApiService.getComplaintCategories()
      .then((rows) => {
        if (!cancelled) setComplaintCategories(categoryNamesFromResponse(rows));
      })
      .catch(() => {
        if (!cancelled) setComplaintCategories(categoryNamesFromResponse(null));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Synchronize Ethiopian date states to formData.date
  useEffect(() => {
    syncEthiopianFormDate(language, ethMonth, ethDay, ethYear, setFormData);
  }, [ethMonth, ethDay, ethYear, language]);

  const evidenceButtonLabel = publicEvidenceButtonLabel(t, isUploadingEvidence, evidenceFiles);
  const submitButtonLabel = publicSubmitButtonLabel(t, isSubmitting, language);
  const {
    onSubmit: handleSubmit,
    onInputChange: handleInputChange,
    onCheckStatus: handleCheckStatus,
    statusTag: getStatusTag,
    highlightEvidenceChooser,
    resetEvidenceChooser,
    highlightTrackButton,
    resetTrackButton
  } = bindPublicFormHandlers({
    formData, countryCode, language, consentChecked, t, evidenceFiles, evidenceUrl, evidenceName, errors,
    ticketSearch, isUploadingEvidence,
    setIsSubmitting, setMessageText, setMessageType, setErrors, setFormData, setConsentChecked,
    setEvidenceFiles, setEvidenceUrl, setEvidenceName, setSearchError, setIsSearching, setSearchResult
  });

  return (
    <div
      className={publicPageClassName(language)}
      style={{ minHeight: '100vh', backgroundColor: '#fcfcfc', display: 'flex', flexDirection: 'column' }}
    >
      {/* Header - Left Aligned */}
      <div className="cms-public-header">
        {/* Language Selector */}
        <div className="cms-public-lang">
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

        <div className="cms-public-brand">
          <img
            src="/download.png"
            alt="Dashen Bank Logo"
          />
          <div>
            <h1 className="cms-public-title">
              {t.subtitle}
            </h1>
          </div>
        </div>
      </div>

      {/* Main Content - Two Column Layout */}
      <div className="cms-public-body">
        {/* Left Column - Main Form */}
        <div className="cms-public-form-card">
          <h2 style={{ textAlign: 'left', marginBottom: '40px', color: BRAND_COLORS.primary, fontSize: '24px', fontWeight: 700, borderBottom: '1px solid #f0f0f0', paddingBottom: '15px' }}>
            {t.formTitle}
          </h2>

          {messageText && (
            <div style={{
              padding: '12px',
              marginBottom: '20px',
              ...publicMessageBannerStyle(messageType),
              borderRadius: '6px',
              wordBreak: 'break-word',
              overflowWrap: 'anywhere'
            }}>
              {messageText}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Row 1: Name and Email */}
            <div className="cms-form-row">
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
                    border: publicFieldBorder(errors.customerName),
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
                {errors.customerName && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.customerName}
                  </div>
                )}
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
                    border: publicFieldBorder(errors.email),
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    transition: 'border-color 0.2s'
                  }}
                />
                {errors.email && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.email}
                  </div>
                )}
              </div>
            </div>

            {/* Row 2: Phone and Date */}
            <div className="cms-form-row">
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.phoneNumber} <span style={{ color: 'red' }}>*</span>
                </label>
                <div style={{ display: 'flex' }}>
                  <select
                    value={countryCode}
                    onChange={(e) => {
                      const nextCode = e.target.value;
                      const phone = (formData.phone || '').replace(/\D/g, '').slice(0, publicPhoneRule(nextCode).maxDigits);
                      setCountryCode(nextCode);
                      setFormData(prev => ({ ...prev, phone }));
                      const phoneErr = getPhoneError(phone, nextCode, language);
                      setErrors(prev => {
                        const next = { ...prev };
                        if (phone && phoneErr) next.phone = phoneErr;
                        else delete next.phone;
                        return next;
                      });
                    }}
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
                    maxLength={publicPhoneRule(countryCode).maxDigits}
                    required
                    style={{
                      flex: 1,
                      padding: '12px 16px',
                      border: publicFieldBorder(errors.phone),
                      borderRadius: '0 4px 4px 0',
                      fontSize: '15px',
                      outline: 'none',
                      transition: 'border-color 0.2s'
                    }}
                  />
                </div>
                {errors.phone && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.phone}
                  </div>
                )}
              </div>

              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '10px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                  {t.complaintDate} <span style={{ color: 'red' }}>*</span>
                </label>
                {language === 'english' ? (
                  <DatePicker
                    format="DD/MM/YYYY"
                    placeholder="DD/MM/YYYY"
                    disabledDate={isComplaintDateDisabled}
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
            <div className="cms-form-row">
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
                    border: publicFieldBorder(errors.accountNumber),
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
                  maxLength={100}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: publicFieldBorder(errors.district),
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    backgroundColor: 'white',
                    transition: 'border-color 0.2s'
                  }}
                />
                {errors.district && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.district}
                  </div>
                )}
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
                  maxLength={100}
                  style={{
                    width: '100%',
                    padding: '12px 16px',
                    border: publicFieldBorder(errors.branch),
                    borderRadius: '4px',
                    fontSize: '15px',
                    outline: 'none',
                    backgroundColor: 'white',
                    transition: 'border-color 0.2s'
                  }}
                />
                {errors.branch && (
                  <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                    {errors.branch}
                  </div>
                )}
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
                {complaintCategories.map(cat => (
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
                  border: publicFieldBorder(errors.complaintDescription),
                  borderRadius: '4px',
                  fontSize: '15px',
                  resize: 'vertical',
                  outline: 'none',
                  transition: 'border-color 0.2s'
                }}
              />
              {errors.complaintDescription && (
                <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                  {errors.complaintDescription}
                </div>
              )}
            </div>

            {/* Row 6: Evidence Attachment (Optional) */}
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#444', fontSize: '14px' }}>
                {t.evidenceLabel}
              </label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <input
                  id="evidence-file-input"
                  type="file"
                  multiple
                  accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx,.mp3,.wav,.m4a"
                  style={{ display: 'none' }}
                  onChange={(e) => onPublicEvidenceSelected(e, {
                    language, setIsUploadingEvidence, setEvidenceFiles, setEvidenceUrl, setEvidenceName, setMessageType, setMessageText
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
                {evidenceFiles.map((file) => (
                  <span key={file.url} style={{ color: '#166534', fontSize: '14px', fontWeight: 500, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                    {file.name}
                    <button
                      type="button"
                      onClick={() => {
                        setEvidenceFiles((prev) => {
                          const next = prev.filter((item) => item.url !== file.url);
                          setEvidenceUrl(next[0]?.url || '');
                          setEvidenceName(next.map((item) => item.name).join(', '));
                          return next;
                        });
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#dc2626',
                        cursor: 'pointer',
                        fontSize: '18px',
                        padding: '0 4px'
                      }}
                      title={t.evidenceRemove}
                    >
                      ×
                    </button>
                  </span>
                ))}
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
        <div className="cms-public-side-card">
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
            <div className="cms-form-row cms-form-row-tight">
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
