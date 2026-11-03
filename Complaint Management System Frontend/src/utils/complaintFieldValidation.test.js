import { getCustomerNameError, getDescriptionError, getPlaceNameError } from './complaintFieldValidation';

test('an Amharic name, place and complaint can be written', () => {
  expect(getCustomerNameError('አበበ ተስፋዬ', 'amharic')).toBe('');
  expect(getPlaceNameError('ቦሌ ቅርንጫፍ', 'amharic', 'Complaint Branch')).toBe('');
  expect(getDescriptionError('የኤቲኤም ገንዘብ አላወጣልኝም። እባክዎ ይመልከቱ።', 'amharic')).toBe('');
});

test('a name that is not letters is still rejected', () => {
  expect(getCustomerNameError('Abebe1', 'english')).not.toBe('');
});
