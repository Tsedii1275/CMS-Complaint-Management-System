import {
  getCustomerNameError,
  getDescriptionError,
  getPlaceNameError,
  lettersOnlyPlaceName,
  liveFormText,
  textWhileComposing
} from './complaintFieldValidation';

test('an Amharic name, place and complaint can be written', () => {
  expect(getCustomerNameError('አበበ ተስፋዬ', 'amharic')).toBe('');
  expect(getCustomerNameError('አበበ፣ ተስፋዬ', 'amharic')).toBe('');
  expect(getPlaceNameError('ቦሌ ቅርንጫፍ', 'amharic', 'Complaint Branch')).toBe('');
  expect(getDescriptionError('የኤቲኤም ገንዘብ አላወጣልኝም። እባክዎ ይመልከቱ።', 'amharic')).toBe('');
});

test('an Amharic syllable is kept until the keyboard finishes it', () => {
  const composing = { target: { value: 'አ' }, nativeEvent: { isComposing: true } };
  const committed = { target: { value: 'ቦሌ 1' }, nativeEvent: { isComposing: false } };
  expect(textWhileComposing(composing, lettersOnlyPlaceName)).toBe('አ');
  expect(textWhileComposing(committed, lettersOnlyPlaceName)).toBe('ቦሌ ');
});

test('a typed Amharic value is submitted even when form state is still empty', () => {
  const form = document.createElement('form');
  const name = document.createElement('input');
  name.name = 'customerName';
  name.value = 'አበበ ተስፋዬ';
  const description = document.createElement('textarea');
  description.name = 'complaintDescription';
  description.value = 'የኤቲኤም ገንዘብ አላወጣልኝም።';
  const district = document.createElement('input');
  district.name = 'district';
  district.value = 'ቦሌ 1';
  form.append(name, description, district);
  const typed = liveFormText(form, { customerName: '', complaintDescription: '', district: 'Bole' });
  expect(typed.customerName).toBe('አበበ ተስፋዬ');
  expect(typed.complaintDescription).toBe('የኤቲኤም ገንዘብ አላወጣልኝም።');
  expect(typed.district).toBe('ቦሌ ');
});

test('a name that is not letters is still rejected', () => {
  expect(getCustomerNameError('Abebe1', 'english')).not.toBe('');
  expect(getCustomerNameError('Abebe Bekele', 'english')).toBe('');
});

test('Power Ge\'ez private-use letters stay in the field', () => {
  const typed = '\uF041\uF042 \uF061';
  expect(lettersOnlyPlaceName(typed)).toBe(typed);
  expect(getPlaceNameError(typed, 'amharic', 'Complaint District')).toBe('');
  expect(getCustomerNameError('\uF041\uF042', 'amharic')).toBe('');
});

test('english names and every Amharic letter block can be saved', () => {
  const samples = [
    'Mary-Jane O\'Brien',
    'Abebe Bekele',
    'አበበ ተስፋዬ',
    'አበበ፠ ተስፋዬ',
    'ሰላም \u2D80',
    'ቅርንጫፍ \uAB01',
    'Hello አበበ',
    '\uF041\uF042 ቦሌ'
  ];
  samples.forEach((sample) => {
    expect(getCustomerNameError(sample, 'english')).toBe('');
  });
  expect(getPlaceNameError('ቦሌ፣ ቅርንጫፍ', 'english', 'Complaint Branch')).toBe('');
  expect(getPlaceNameError('Bole Branch', 'english', 'Complaint Branch')).toBe('');
  expect(lettersOnlyPlaceName('ቦሌ፣ 1')).toBe('ቦሌ፣ ');
  expect(getDescriptionError('ATM ገንዘብ አላወጣም። እባክዎ ይመልከቱ።', 'english')).toBe('');
  expect(getDescriptionError('Line one\nየኤቲኤም ችግር አጋጥሞኛል።', 'amharic')).toBe('');
  expect(getCustomerNameError('Abebe1', 'english')).not.toBe('');
  expect(getPlaceNameError('East 1', 'english', 'Complaint District')).not.toBe('');
  expect(getDescriptionError('<script>alert(1)</script> is not allowed here', 'english')).not.toBe('');
});
