import {
  getCustomerNameError,
  getDescriptionError,
  getPlaceNameError,
  lettersOnlyPlaceName,
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

test('a name that is not letters is still rejected', () => {
  expect(getCustomerNameError('Abebe1', 'english')).not.toBe('');
});
