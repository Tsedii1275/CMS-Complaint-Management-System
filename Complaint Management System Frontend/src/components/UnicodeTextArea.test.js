import React, { useState } from 'react';
import { act } from 'react-dom/test-utils';
import { createRoot } from 'react-dom/client';
import { StableField } from './UnicodeTextArea';

function mount(ui) {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  act(() => {
    root.render(ui);
  });
  return {
    container,
    rerender(next) {
      act(() => {
        root.render(next);
      });
    },
    unmount() {
      act(() => root.unmount());
      container.remove();
    }
  };
}

function typeInto(field, value) {
  const setter = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(field), 'value').set;
  setter.call(field, value);
  act(() => {
    field.dispatchEvent(new Event('input', { bubbles: true }));
  });
}

function NameBox() {
  const [value, setValue] = useState('');
  return (
    <>
      <StableField aria-label="customer name" value={value} onChange={(event) => setValue(event.target.value)} />
      <span data-testid="stored">{value}</span>
    </>
  );
}

test('english typing and a pasted Amharic name stay in the field', () => {
  const view = mount(<NameBox />);
  const input = view.container.querySelector('input');

  typeInto(input, 'Abebe Bekele');
  expect(view.container.querySelector('[data-testid="stored"]').textContent).toBe('Abebe Bekele');
  expect(input.value).toBe('Abebe Bekele');

  typeInto(input, 'አበበ ተስፋዬ');
  expect(view.container.querySelector('[data-testid="stored"]').textContent).toBe('አበበ ተስፋዬ');
  expect(input.value).toBe('አበበ ተስፋዬ');
  view.unmount();
});

test('clearing a field is kept', () => {
  const view = mount(<NameBox />);
  const input = view.container.querySelector('input');
  typeInto(input, 'Abebe');
  typeInto(input, '');
  expect(input.value).toBe('');
  expect(view.container.querySelector('[data-testid="stored"]').textContent).toBe('');
  view.unmount();
});

test('an Amharic syllable is put back if the field is cleared mid-composition', () => {
  const view = mount(<StableField aria-label="customer name" value="" onChange={() => {}} />);
  const input = view.container.querySelector('input');

  act(() => {
    input.focus();
    input.dispatchEvent(new Event('compositionstart', { bubbles: true }));
  });
  input.value = 'ሰላም';
  act(() => {
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  view.rerender(<StableField aria-label="customer name" value="ሰ" onChange={() => {}} />);

  expect(input.value).toBe('ሰላም');
  input.value = '';
  act(() => {
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  expect(input.value).toBe('ሰላም');

  act(() => {
    input.dispatchEvent(new Event('compositionend', { bubbles: true }));
  });
  expect(input.value).toBe('ሰላም');
  view.unmount();
});

test('a description keeps pasted Amharic text', () => {
  function Area() {
    const [value, setValue] = useState('');
    return (
      <>
        <StableField
          multiline
          aria-label="description"
          value={value}
          onChange={(event) => setValue(event.target.value)}
        />
        <span data-testid="stored">{value}</span>
      </>
    );
  }

  const view = mount(<Area />);
  const area = view.container.querySelector('textarea');
  const text = 'የኤቲኤም ገንዘብ አላወጣልኝም። እባክዎ ይመልከቱ።';
  typeInto(area, text);
  expect(area.value).toBe(text);
  expect(view.container.querySelector('[data-testid="stored"]').textContent).toBe(text);
  view.unmount();
});
