import React, { forwardRef, useEffect, useRef } from 'react';
import { Input } from 'antd';

const FIELD_FONT = "'Noto Sans Ethiopic', Geezigna, 'Segoe UI', sans-serif";
const ANT_ONLY_PROPS = ['size', 'showCount', 'allowClear', 'bordered', 'status', 'variant'];

function domNode(instance) {
  if (!instance) return null;
  if (instance.tagName) return instance;
  if (instance.input) return instance.input;
  if (instance.resizableTextArea?.textArea) return instance.resizableTextArea.textArea;
  return null;
}

/**
 * The Amharic keyboard builds a syllable over several keys. A controlled field
 * writes the previous value back on each key and the syllable disappears.
 * While the field is focused, the browser keeps what was typed.
 */
export const StableField = forwardRef(function StableField(
  { value, onChange, onFocus, onBlur, multiline = false, antd = false, style, ...rest },
  ref
) {
  const focused = useRef(false);
  const innerRef = useRef(null);

  const setRef = (node) => {
    innerRef.current = node;
    if (typeof ref === 'function') ref(node);
    else if (ref) ref.current = node;
  };

  useEffect(() => {
    const node = domNode(innerRef.current);
    if (!node || focused.current || document.activeElement === node) return;
    const next = value ?? '';
    if (node.value !== next) node.value = next;
  }, [value]);

  const fieldStyle = style ? { ...style, fontFamily: FIELD_FONT } : { fontFamily: FIELD_FONT };
  const domRest = {};
  Object.keys(rest).forEach((key) => {
    if (!ANT_ONLY_PROPS.includes(key)) domRest[key] = rest[key];
  });
  const { className } = rest;
  const props = {
    ...domRest,
    className: antd ? ['ant-input', className].filter(Boolean).join(' ') : className,
    style: fieldStyle,
    defaultValue: value ?? '',
    onCompositionStart: (event) => {
      focused.current = true;
      if (domRest.onCompositionStart) domRest.onCompositionStart(event);
    },
    onChange,
    onFocus: (event) => {
      focused.current = true;
      if (onFocus) onFocus(event);
    },
    onBlur: (event) => {
      focused.current = false;
      if (onChange) onChange(event);
      if (onBlur) onBlur(event);
    }
  };

  if (multiline) return <textarea ref={setRef} {...props} />;
  return <input ref={setRef} {...props} />;
});

const UnicodeTextArea = forwardRef(function UnicodeTextArea(props, ref) {
  return <StableField ref={ref} antd multiline {...props} />;
});

export function installUnicodeTextArea() {
  if (Input.TextArea !== UnicodeTextArea) {
    Input.TextArea = UnicodeTextArea;
  }
}

export default UnicodeTextArea;
