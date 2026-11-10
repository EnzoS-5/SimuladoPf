import React from 'react';
import { createRoot } from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';
import App from './App.jsx';
import './styles.css';

const domain = import.meta.env.VITE_AUTH0_DOMAIN;
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID;
const audience = import.meta.env.VITE_AUTH0_AUDIENCE;
const configured = domain && clientId && audience && !domain.includes('SEU_') && !clientId.includes('SEU_');
createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {configured ? <Auth0Provider domain={domain} clientId={clientId} authorizationParams={{ audience, redirect_uri: window.location.origin }}>
      <App />
    </Auth0Provider> : <main className="config-screen"><span className="brand">a<span>·</span> avalia</span><h1>Vamos conectar sua equipe.</h1><p>Preencha as variáveis VITE_AUTH0_DOMAIN, VITE_AUTH0_CLIENT_ID e VITE_AUTH0_AUDIENCE no arquivo .env.local ou na hospedagem e reinicie o front.</p></main>}
  </React.StrictMode>
);
