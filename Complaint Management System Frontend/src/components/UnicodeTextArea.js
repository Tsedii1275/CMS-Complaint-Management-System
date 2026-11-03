import React, { forwardRef, useEffect, useRef } from 'react';
import { Input } from 'antd';

const FIELD_FONT = "'Noto Sans Ethiopic', Geezigna, 'Segoe UI', sans-serif";
const ANT_ONLY_PROPS = ['size', 'showCount', 'allowClear', 'bordered', 'status', 'variant', 'maxLength'];

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
  { value, onChange, onFocus, onBlur, multiline = false, antd = false, style, maxLength, ...rest },
  ref
) {
  const focused = useRef(false);
  const composing = useRef(false);
  const leftWhileComposing = useRef(false);
  const innerRef = useRef(null);
  const onChangeRef = useRef(onChange);
  const onBlurRef = useRef(onBlur);
  const onFocusRef = useRef(onFocus);
  const initialValue = useRef(undefined);
  if (initialValue.current === undefined) initialValue.current = value ?? '';
  onChangeRef.current = onChange;
  onBlurRef.current = onBlur;
  onFocusRef.current = onFocus;

  const setRef = (node) => {
    innerRef.current = node;
    if (typeof ref === 'function') ref(node);
    else if (ref) ref.current = node;
  };

  useEffect(() => {
    const node = domNode(innerRef.current);
    if (!node) return undefined;
    const start = () => {
      composing.current = true;
      focused.current = true;
    };
    const end = () => {
      composing.current = false;
    };
    node.addEventListener('compositionstart', start);
    node.addEventListener('compositionend', end);
    return () => {
      node.removeEventListener('compositionstart', start);
      node.removeEventListener('compositionend', end);
    };
  }, []);

  useEffect(() => {
    const node = domNode(innerRef.current);
    if (!node || focused.current || composing.current || document.activeElement === node) return;
    const next = value ?? '';
    if (node.value !== next) node.value = next;
  }, [value]);

  const fieldStyle = style ? { ...style, fontFamily: FIELD_FONT } : { fontFamily: FIELD_FONT };
  const domRest = {};
  Object.keys(rest).forEach((key) => {
    if (!ANT_ONLY_PROPS.includes(key)) domRest[key] = rest[key];
  });

  const emitChange = (event) => {
    const node = event.target;
    if (
      maxLength != null &&
      !composing.current &&
      !event.nativeEvent?.isComposing &&
      node.value.length > maxLength
    ) {
      node.value = node.value.slice(0, Number(maxLength));
    }
    if (onChangeRef.current) onChangeRef.current(event);
  };

  const props = {
    ...domRest,
    className: antd ? ['ant-input', rest.className].filter(Boolean).join(' ') : rest.className,
    style: fieldStyle,
    defaultValue: initialValue.current,
    onCompositionStart: (event) => {
      composing.current = true;
      focused.current = true;
      if (domRest.onCompositionStart) domRest.onCompositionStart(event);
    },
    onCompositionEnd: (event) => {
      composing.current = false;
      emitChange(event);
      if (domRest.onCompositionEnd) domRest.onCompositionEnd(event);
      if (leftWhileComposing.current) {
        leftWhileComposing.current = false;
        focused.current = false;
        if (onBlurRef.current) onBlurRef.current(event);
      }
    },
    onChange: emitChange,
    onFocus: (event) => {
      focused.current = true;
      if (onFocusRef.current) onFocusRef.current(event);
    },
    onBlur: (event) => {
      if (composing.current) {
        leftWhileComposing.current = true;
        return;
      }
      focused.current = false;
      emitChange(event);
      if (onBlurRef.current) onBlurRef.current(event);
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
