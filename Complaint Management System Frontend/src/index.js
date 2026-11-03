import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { installUnicodeTextArea } from './components/UnicodeTextArea';

installUnicodeTextArea();

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
