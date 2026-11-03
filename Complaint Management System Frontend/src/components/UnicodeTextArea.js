import React, { forwardRef, useEffect, useState } from 'react';
import { Input } from 'antd';

const AntTextArea = Input.TextArea;

/**
 * Keeps an Amharic syllable while the keyboard is still composing it.
 * A controlled text area that writes the value on every key would replace that
 * syllable with the previous value and the field would look empty.
 */
const UnicodeTextArea = forwardRef(function UnicodeTextArea(
  { value, onChange, onCompositionStart, onCompositionEnd, ...rest },
  ref
) {
  const controlled = value !== undefined && value !== null;
  const [composing, setComposing] = useState(false);
  const [draft, setDraft] = useState(value ?? '');

  useEffect(() => {
    if (!composing) setDraft(value ?? '');
  }, [value, composing]);

  return (
    <AntTextArea
      ref={ref}
      {...rest}
      value={controlled ? (composing ? draft : value) : undefined}
      onCompositionStart={(event) => {
        setDraft(event.target.value);
        setComposing(true);
        if (onCompositionStart) onCompositionStart(event);
      }}
      onChange={(event) => {
        setDraft(event.target.value);
        if (!event.nativeEvent.isComposing && onChange) onChange(event);
      }}
      onCompositionEnd={(event) => {
        setComposing(false);
        setDraft(event.target.value);
        if (onChange) onChange(event);
        if (onCompositionEnd) onCompositionEnd(event);
      }}
    />
  );
});

export function installUnicodeTextArea() {
  if (Input.TextArea !== UnicodeTextArea) {
    Input.TextArea = UnicodeTextArea;
  }
}

export default UnicodeTextArea;
