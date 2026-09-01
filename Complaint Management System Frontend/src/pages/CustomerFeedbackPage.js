import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { BRAND_COLORS } from '../constants/theme';
import ApiService from '../services/api';

function scoreForOption(options, selected) {
  if (!selected) {
    return null;
  }
  const match = options.find(o => o.value === selected || o.label === selected);
  return match ? match.score : null;
}

function yesNoBorderColor(isSelected, isYes) {
  if (!isSelected) {
    return '#cbd5e1';
  }
  return isYes ? '#10b981' : '#ef4444';
}

function yesNoBackground(isSelected, isYes, hoveredYesNo) {
  if (isSelected) {
    return isYes ? '#ecfdf5' : '#fef2f2';
  }
  return hoveredYesNo === isYes ? '#f8fafc' : '#ffffff';
}

function yesNoTextColor(isSelected, isYes) {
  if (!isSelected) {
    return '#475569';
  }
  return isYes ? '#047857' : '#b91c1c';
}

function buildCustomerFeedbackPayload({
  token,
  satisfied,
  csatOption,
  speedOption,
  easeOption,
  npsScore,
  additionalComments,
  q2Options,
  q4Options
}) {
  return {
    token,
    satisfied,
    csatScore: satisfied ? scoreForOption(q2Options, csatOption) : null,
    resolutionSpeedRating: satisfied ? (speedOption || '') : '',
    easeRating: satisfied ? (easeOption || '') : '',
    cesScore: satisfied ? scoreForOption(q4Options, easeOption) : null,
    npsScore: satisfied ? npsScore : null,
    additionalComments
  };
}

function CustomerFeedbackPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [ticketNumber, setTicketNumber] = useState('');
  const [validating, setValidating] = useState(true);
  const [validationError, setValidationError] = useState('');

  // Language state
  const [language, setLanguage] = useState('english');

  const translations = {
    english: {
      documentTitle: 'Resolution Feedback – Dashen Bank',
      title: 'Resolution Feedback Survey',
      subtitle: 'Help us measure and improve our customer satisfaction by completing this quick survey.',
      ticketNumber: 'TICKET NUMBER',
      validating: 'Securing connection...',
      pleaseWait: 'Verifying unique survey link security token...',

      // Q1: Has your complaint been resolved?
      q1: '1. Has your complaint been resolved? *',
      q1Yes: 'Yes',
      q1No: 'No',

      // Q2: Satisfaction (4 options)
      q2: '2. If you answered "Yes" to Question 1, how satisfied are you with how your complaint was resolved?',
      q2Options: [
        { label: 'Very satisfied', value: 'Very satisfied', score: 5 },
        { label: 'Satisfied', value: 'Satisfied', score: 4 },
        { label: 'Dissatisfied', value: 'Dissatisfied', score: 2 },
        { label: 'Very dissatisfied', value: 'Very dissatisfied', score: 1 },
      ],

      // Q3: Speed Rating (4 options)
      q3: '3. How would you rate the speed with which your complaint was resolved?',
      q3Options: ['Very fast', 'Fast', 'Slow', 'Very slow'],

      // Q4: Ease Rating (4 options)
      q4: '4. How easy was it to submit your complaint and track its progress?',
      q4Options: [
        { label: 'Very easy', value: 'Very easy', score: 4 },
        { label: 'Easy', value: 'Easy', score: 3 },
        { label: 'Difficult', value: 'Difficult', score: 2 },
        { label: 'Very difficult', value: 'Very difficult', score: 1 },
      ],

      // Q5: NPS 0-10
      q5: '5. On a scale of 0–10, how likely are you to recommend Dashen Bank to your family, friends, and people you know?',
      q5Legend0: '0 = Not at all likely',
      q5Legend10: '10 = Definitely',

      // Q6: Additional Feedback
      q6: '6. Additional Feedback',
      q6Label: 'Please share any additional feedback with us:',
      q6Placeholder: 'Enter your comments or suggestions here...',

      submitBtn: 'Submit Feedback',
      submitting: 'Submitting Survey...',
      mandatoryError: 'Please answer Question 1: Has your complaint been resolved?',
      expiredTokenError: 'This feedback link has already been submitted or expired.',
      invalidTokenError: 'Invalid feedback token.',
      noTokenError: 'No secure feedback token was provided in the URL. Please verify your feedback link.',
      genericError: 'Failed to submit feedback. Please try again.',
      successTitle: 'Thank You!',
      successDescYes: 'Thank you! Your feedback has been successfully recorded and your case has been closed.',
      successDescNo: 'Thank you! Your response has been submitted. Our Customer Care Leadership team will follow up on your issue.',
    },
    amharic: {
      documentTitle: 'የመፍትሄ እርካታ ዳሰሳ – ዳሽን ባንክ',
      title: 'የመፍትሄ እርካታ ዳሰሳ ጥናት',
      subtitle: 'ይህን አጭር ዳሰሳ በመሙላት የደንበኞቻችንን እርካታ እንድንለካ እና እንድናሻሽል ይርዱን።',
      ticketNumber: 'የቅሬታ ቁጥር',
      validating: 'ግንኙነት እየተረጋገጠ ነው...',
      pleaseWait: 'የእርካታ ዳሰሳ መለያ ቁጥር እየተረጋገጠ ነው...',

      q1: '1. ቅሬታዎ መፍትሄ አግኝቷል? *',
      q1Yes: 'አዎ',
      q1No: 'አይ',

      q2: '2. ለጥያቄ 1 "አዎ" ብለው የመለሱ ከሆነ፣ ቅሬታዎ በተፈታበት ሁኔታ ምን ያህል ረክተዋል?',
      q2Options: [
        { label: 'በጣም ረክቻለሁ', value: 'Very satisfied', score: 5 },
        { label: 'ረክቻለሁ', value: 'Satisfied', score: 4 },
        { label: 'አልረካሁም', value: 'Dissatisfied', score: 2 },
        { label: 'በጣም አልረካሁም', value: 'Very dissatisfied', score: 1 },
      ],

      q3: '3. ቅሬታዎ የተፈታበትን ፍጥነት እንዴት ይመዝኑታል?',
      q3Options: ['በጣም ፈጣን', 'ፈጣን', 'ዝግተኛ', 'በጣም ዝግተኛ'],

      q4: '4. ቅሬታዎን ለማስገባት እና ሂደቱን ለመከታተል ምን ያህል ቀላል ነበር?',
      q4Options: [
        { label: 'በጣም ቀላል', value: 'Very easy', score: 4 },
        { label: 'ቀላል', value: 'Easy', score: 3 },
        { label: 'ከባድ', value: 'Difficult', score: 2 },
        { label: 'በጣም ከባድ', value: 'Very difficult', score: 1 },
      ],

      q5: '5. ከ 0–10 ባለው ልኬት ዳሽን ባንክን ለቤተሰብዎ፣ ለጓደኞችዎ እና ለሚያውቋቸው ሰዎች የመምከር እድልዎ ምን ያህል ነው?',
      q5Legend0: '0 = በፍጹም አልመክርም',
      q5Legend10: '10 = በእርግጠኝነት እመክራለሁ',

      q6: '6. ተጨማሪ አስተያየት',
      q6Label: 'እባክዎ ማንኛውንም ተጨማሪ አስተያየት ያካፍሉን:',
      q6Placeholder: 'አስተያየትዎን እዚህ ያስገቡ...',

      submitBtn: 'አስተያየቱን አስገባ',
      submitting: 'ዳሰሳው እየገባ ነው...',
      mandatoryError: 'እባክዎ ለጥያቄ 1 መልስ ይስጡ: ቅሬታዎ መፍትሄ አግኝቷል?',
      expiredTokenError: 'ይህ የእርካታ ዳሰሳ አስቀድሞ ገብቷል ወይም ጊዜው አልፏል።',
      invalidTokenError: 'ልክ ያልሆነ የእርካታ ዳሰሳ ሊንክ።',
      noTokenError: 'በሊንኩ ውስጥ ምንም የእርካታ ዳሰሳ መለያ አልተገኘም። እባክዎ ሊንኩን ያረጋግጡ።',
      genericError: 'ዳሰሳውን ማስገባት አልተቻለም። እባክዎ ድጋሚ ይሞክሩ።',
      successTitle: 'እናመሰግናለን!',
      successDescYes: 'እናመሰግናለን! አስተያየትዎ በተሳካ ሁኔታ ተመዝግቧል።',
      successDescNo: 'እናመሰግናለን! ምላሽዎ ገብቷል። የደንበኞች አገልግሎት ኃላፊዎቻችን ጉዳዩን ይከታተሉታል ።',
    }
  };

  // Form states
  const [satisfied, setSatisfied] = useState(null); // true (Yes) | false (No)
  const [csatOption, setCsatOption] = useState(null); // 'Very satisfied' | 'Satisfied' | 'Dissatisfied' | 'Very dissatisfied'
  const [speedOption, setSpeedOption] = useState(null); // 'Very fast' | 'Fast' | 'Slow' | 'Very slow'
  const [easeOption, setEaseOption] = useState(null); // 'Very easy' | 'Easy' | 'Difficult' | 'Very difficult'
  const [npsScore, setNpsScore] = useState(null); // 0 to 10
  const [additionalComments, setAdditionalComments] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [submitMessage, setSubmitMessage] = useState('');
  const [error, setError] = useState('');
  const [hoveredYesNo, setHoveredYesNo] = useState(null);

  useEffect(() => {
    document.title = translations[language].documentTitle;
  }, [language]);

  useEffect(() => {
    validateToken();
  }, [token]);

  const validateToken = async () => {
    if (!token) {
      setValidationError(translations[language].noTokenError);
      setValidating(false);
      return;
    }
    try {
      setValidating(true);
      const res = await ApiService.validateCustomerFeedbackToken(token);
      if (res?.valid) {
        setTicketNumber(res.ticketNumber);
        if (res.preferredLanguage) {
          setLanguage(res.preferredLanguage.toLowerCase());
        }
      } else {
        setValidationError(res?.message || res?.error || translations[language].invalidTokenError);
      }
    } catch (err) {
      if (err.code === 'FEEDBACK_TOKEN_EXPIRED' || err.status === 409) {
        setValidationError(translations[language].expiredTokenError);
      } else if (err.code === 'FEEDBACK_TOKEN_NOT_FOUND' || err.status === 404) {
        setValidationError(translations[language].invalidTokenError);
      } else {
        setValidationError(err.message || translations[language].genericError);
      }
    } finally {
      setValidating(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (satisfied === null) {
      setError(translations[language].mandatoryError);
      return;
    }

    setError('');
    setIsSubmitting(true);

    const payload = buildCustomerFeedbackPayload({
      token,
      satisfied,
      csatOption,
      speedOption,
      easeOption,
      npsScore,
      additionalComments,
      q2Options: translations.english.q2Options,
      q4Options: translations.english.q4Options
    });

    try {
      const result = await ApiService.submitCustomerFeedback(payload);
      const fallbackMessage = satisfied ? translations[language].successDescYes : translations[language].successDescNo;
      setSubmitMessage(result.message || fallbackMessage);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || translations[language].genericError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const styles = {
    page: {
      minHeight: '100vh',
      background: '#f8fafc',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 12px',
      fontFamily: "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    },
    container: {
      width: '100%',
      maxWidth: '640px',
    },
    card: {
      background: '#ffffff',
      borderRadius: '16px',
      padding: '24px 20px',
      boxShadow: '0 4px 24px rgba(0, 0, 0, 0.05)',
      border: '1px solid #e2e8f0',
    },
    logo: {
      height: '42px',
      width: 'auto',
      display: 'block',
      margin: '0 auto 20px',
    },
    title: {
      color: BRAND_COLORS.primary,
      fontSize: '22px',
      fontWeight: 700,
      margin: '0 0 8px',
      textAlign: 'center',
      letterSpacing: '-0.3px',
    },
    tagline: {
      color: '#64748b',
      fontSize: '14px',
      textAlign: 'center',
      margin: '0 0 24px',
    },
    ticketBadge: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#f1f5f9',
      borderRadius: '8px',
      padding: '8px 14px',
      marginBottom: '28px',
      width: '100%',
      boxSizing: 'border-box',
    },
    ticketLabel: {
      color: '#64748b',
      fontSize: '11px',
      fontWeight: 600,
      textTransform: 'uppercase',
      letterSpacing: '0.6px',
      marginRight: '6px',
    },
    ticketValue: {
      color: BRAND_COLORS.primary,
      fontSize: '14px',
      fontWeight: 700,
      fontFamily: 'monospace',
    },
    section: {
      width: '100%',
      marginBottom: '28px',
      paddingBottom: '24px',
      borderBottom: '1px solid #f1f5f9',
    },
    sectionTitle: {
      color: '#1e293b',
      fontSize: '15px',
      fontWeight: 600,
      marginBottom: '16px',
      lineHeight: '1.4',
    },
    yesNoRow: {
      display: 'flex',
      gap: '16px',
      width: '100%',
    },
    yesNoBtn: (isSelected, isYes) => ({
      flex: 1,
      padding: '14px',
      borderRadius: '10px',
      border: `2px solid ${yesNoBorderColor(isSelected, isYes)}`,
      background: yesNoBackground(isSelected, isYes, hoveredYesNo),
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '8px',
      transition: 'all 0.15s ease',
      outline: 'none',
      fontSize: '15px',
      fontWeight: 600,
      color: yesNoTextColor(isSelected, isYes),
    }),
    optionGrid: {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
      gap: '12px',
    },
    optionBtn: (isSelected) => ({
      padding: '12px 14px',
      borderRadius: '8px',
      border: `1.5px solid ${isSelected ? BRAND_COLORS.primary : '#e2e8f0'}`,
      background: isSelected ? '#eff6ff' : '#ffffff',
      color: isSelected ? BRAND_COLORS.primary : '#334155',
      fontSize: '14px',
      fontWeight: isSelected ? 600 : 500,
      cursor: 'pointer',
      textAlign: 'center',
      transition: 'all 0.15s ease',
      outline: 'none',
    }),
    scaleRow: {
      display: 'flex',
      justifyContent: 'space-between',
      gap: '4px',
      marginBottom: '8px',
      flexWrap: 'wrap',
    },
    scaleBtn: (isSelected) => ({
      flex: '1 1 28px',
      minWidth: '28px',
      height: '40px',
      borderRadius: '8px',
      border: `1.5px solid ${isSelected ? BRAND_COLORS.primary : '#e2e8f0'}`,
      background: isSelected ? BRAND_COLORS.primary : '#ffffff',
      color: isSelected ? '#ffffff' : '#334155',
      cursor: 'pointer',
      fontSize: '13px',
      fontWeight: 600,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      transition: 'all 0.15s ease',
    }),
    scaleLabels: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: '11px',
      color: '#64748b',
      marginTop: '6px',
    },
    textareaLabel: {
      display: 'block',
      fontSize: '13px',
      color: '#475569',
      marginBottom: '8px',
      fontWeight: 500,
    },
    textarea: {
      width: '100%',
      minHeight: '90px',
      padding: '12px',
      borderRadius: '8px',
      border: '1.5px solid #cbd5e1',
      fontSize: '14px',
      color: '#1e293b',
      resize: 'vertical',
      outline: 'none',
      fontFamily: 'inherit',
      boxSizing: 'border-box',
      background: '#ffffff',
    },
    submitBtn: {
      width: '100%',
      padding: '14px',
      borderRadius: '10px',
      border: 'none',
      background: BRAND_COLORS.primary,
      color: '#ffffff',
      fontSize: '16px',
      fontWeight: 600,
      cursor: isSubmitting ? 'not-allowed' : 'pointer',
      opacity: isSubmitting ? 0.7 : 1,
      boxShadow: '0 4px 14px rgba(30, 58, 138, 0.25)',
      transition: 'all 0.2s ease',
    },
    errorBox: {
      background: '#fef2f2',
      border: '1px solid #fecaca',
      borderRadius: '8px',
      padding: '12px 14px',
      color: '#dc2626',
      fontSize: '13px',
      marginBottom: '20px',
      textAlign: 'center',
    },
    successTitle: {
      color: BRAND_COLORS.primary,
      fontSize: '24px',
      fontWeight: 700,
      margin: '20px 0 12px',
      textAlign: 'center',
    },
    successText: {
      color: '#475569',
      fontSize: '15px',
      textAlign: 'center',
      lineHeight: '1.6',
      margin: '0 auto 28px',
      maxWidth: '480px',
    },
  };

  if (validating) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <div style={{ ...styles.card, textAlign: 'center', padding: '60px 40px' }}>
            <div style={{ border: '4px solid #f3f3f3', borderTop: `4px solid ${BRAND_COLORS.primary}`, borderRadius: '50%', width: '40px', height: '40px', margin: '0 auto 20px', animation: 'spin 1s linear infinite' }} />
            <h3 style={{ color: '#1e293b', fontWeight: 600 }}>{translations[language].validating}</h3>
            <p style={{ color: '#64748b', fontSize: '13px' }}>{translations[language].pleaseWait}</p>
          </div>
        </div>
      </div>
    );
  }

  if (validationError) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <div style={{ ...styles.card, textAlign: 'center', padding: '50px 40px' }}>
            <span style={{ fontSize: '48px', display: 'block', marginBottom: '16px' }}>⚠️</span>
            <h3 style={{ color: '#dc2626', fontSize: '18px', fontWeight: 700, margin: '0 0 12px' }}>
              {language === 'amharic' ? 'የእርካታ ዳሰሳ ጥናት አይገኝም' : 'Survey Link Expired or Invalid'}
            </h3>
            <p style={{ color: '#475569', fontSize: '14px', lineHeight: '1.6', margin: '0 0 24px' }}>
              {validationError}
            </p>
            <p style={{ color: '#94a3b8', fontSize: '12px' }}>
              {language === 'amharic' ? 'ለደህንነት ሲባል እያንዳንዱ የእርካታ ዳሰሳ ሊንክ ለአንድ ጊዜ ብቻ የሚያገለግል ነው።' : 'For security, each survey link is unique and can only be submitted once.'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <div style={styles.card}>
          <img src="/download.png" alt="Dashen Bank" style={styles.logo} />

          {submitted ? (
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <span style={{ fontSize: '56px', display: 'block', marginBottom: '16px' }}>
                {satisfied ? '😊' : '📩'}
              </span>
              <h2 style={styles.successTitle}>
                {translations[language].successTitle}
              </h2>
              <p style={styles.successText}>
                {submitMessage}
              </p>
              <div style={{ height: '1px', background: '#e2e8f0', margin: '24px 0' }} />
              <p style={{ color: '#94a3b8', fontSize: '12px' }}>
                {language === 'amharic' ? 'ለዳሽን ባንክ ጊዜ ሰጥተው ስላካፈሉን እናመሰግናለን።' : 'Thank you for taking the time to share your feedback with Dashen Bank.'}
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <h2 style={styles.title}>{translations[language].title}</h2>
              <p style={styles.tagline}>
                {translations[language].subtitle}
              </p>

              {ticketNumber && (
                <div style={styles.ticketBadge}>
                  <span style={styles.ticketLabel}>{translations[language].ticketNumber}:</span>
                  <span style={styles.ticketValue}>{ticketNumber}</span>
                </div>
              )}

              {/* Question 1: Has your complaint been resolved? (Mandatory) */}
              <div style={styles.section}>
                <h3 style={styles.sectionTitle}>
                  {translations[language].q1}
                </h3>
                <div style={styles.yesNoRow}>
                  <button
                    type="button"
                    style={styles.yesNoBtn(satisfied === true, true)}
                    onClick={() => {
                      setSatisfied(true);
                      setError('');
                    }}
                    onMouseEnter={() => setHoveredYesNo(true)}
                    onMouseLeave={() => setHoveredYesNo(null)}
                  >
                    <span>👍</span> {translations[language].q1Yes}
                  </button>
                  <button
                    type="button"
                    style={styles.yesNoBtn(satisfied === false, false)}
                    onClick={() => {
                      setSatisfied(false);
                      setError('');
                    }}
                    onMouseEnter={() => setHoveredYesNo(false)}
                    onMouseLeave={() => setHoveredYesNo(null)}
                  >
                    <span>👎</span> {translations[language].q1No}
                  </button>
                </div>
              </div>

              {/* Questions 2–6 are ONLY displayed if customer selected "Yes" to Question 1 */}
              {satisfied === true && (
                <>
                  {/* Question 2: Satisfaction level */}
                  <div style={styles.section}>
                    <h3 style={styles.sectionTitle}>
                      {translations[language].q2}
                    </h3>
                    <div style={styles.optionGrid}>
                      {translations[language].q2Options.map((opt) => (
                        <button
                          key={opt.value}
                          type="button"
                          style={styles.optionBtn(csatOption === opt.value)}
                          onClick={() => setCsatOption(opt.value)}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Question 3: Speed Rating */}
                  <div style={styles.section}>
                    <h3 style={styles.sectionTitle}>
                      {translations[language].q3}
                    </h3>
                    <div style={styles.optionGrid}>
                      {translations[language].q3Options.map((speedLabel, idx) => {
                        const englishValue = translations.english.q3Options[idx];
                        return (
                          <button
                            key={englishValue}
                            type="button"
                            style={styles.optionBtn(speedOption === englishValue)}
                            onClick={() => setSpeedOption(englishValue)}
                          >
                            {speedLabel}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Question 4: Ease Rating */}
                  <div style={styles.section}>
                    <h3 style={styles.sectionTitle}>
                      {translations[language].q4}
                    </h3>
                    <div style={styles.optionGrid}>
                      {translations[language].q4Options.map((opt) => (
                        <button
                          key={opt.value}
                          type="button"
                          style={styles.optionBtn(easeOption === opt.value)}
                          onClick={() => setEaseOption(opt.value)}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Question 5: NPS (Scale 0-10) */}
                  <div style={styles.section}>
                    <h3 style={styles.sectionTitle}>
                      {translations[language].q5}
                    </h3>
                    <div style={styles.scaleRow}>
                      {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((score) => (
                        <button
                          key={score}
                          type="button"
                          style={styles.scaleBtn(npsScore === score)}
                          onClick={() => setNpsScore(score)}
                        >
                          {score}
                        </button>
                      ))}
                    </div>
                    <div style={styles.scaleLabels}>
                      <span>{translations[language].q5Legend0}</span>
                      <span>{translations[language].q5Legend10}</span>
                    </div>
                  </div>

                  {/* Question 6: Additional Feedback */}
                  <div style={{ ...styles.section, borderBottom: 'none', marginBottom: '16px', paddingBottom: 0 }}>
                    <h3 style={styles.sectionTitle}>
                      {translations[language].q6}
                    </h3>
                    <label style={styles.textareaLabel}>
                      {translations[language].q6Label}
                    </label>
                    <textarea
                      style={styles.textarea}
                      value={additionalComments}
                      onChange={(e) => setAdditionalComments(e.target.value)}
                      placeholder={translations[language].q6Placeholder}
                    />
                  </div>
                </>
              )}

              {error && <div style={styles.errorBox}>{error}</div>}

              <button
                type="submit"
                style={styles.submitBtn}
                disabled={isSubmitting}
              >
                {isSubmitting ? translations[language].submitting : translations[language].submitBtn}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default CustomerFeedbackPage;
