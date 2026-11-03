import React from 'react';
import ReactDOM from 'react-dom/client';
import '@fontsource/noto-sans-ethiopic/ethiopic-400.css';
import '@fontsource/noto-sans-ethiopic/ethiopic-700.css';
import App from './App';
import { installUnicodeTextArea } from './components/UnicodeTextArea';

installUnicodeTextArea();

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
