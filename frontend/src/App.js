import React, { useState } from 'react';
import LoginBar from './components/LoginBar';
import PartsBrowser from './components/PartsBrowser';
import PartDetail from './components/PartDetail';
import ChatPanel from './components/ChatPanel';
import { getToken } from './auth';

export default function App() {
  const [selectedPartId, setSelectedPartId] = useState(null);
  const [authVersion, setAuthVersion] = useState(0);
  const [tab, setTab] = useState('browse');

  const authed = !!getToken();

  return (
    <div className="app">
      <header className="app-header">
        <h1>PLM — Data Center Hardware</h1>
        <LoginBar onAuthChange={() => setAuthVersion((v) => v + 1)} />
      </header>

      {!authed ? (
        <p className="hint">Log in to browse parts and use the assistant.</p>
      ) : (
        <>
          <nav className="tabs">
            <button className={tab === 'browse' ? 'active' : ''} onClick={() => setTab('browse')}>
              Parts Browser
            </button>
            <button className={tab === 'chat' ? 'active' : ''} onClick={() => setTab('chat')}>
              Assistant
            </button>
          </nav>

          {tab === 'browse' ? (
            <main className="browser-layout" key={authVersion}>
              <PartsBrowser selectedPartId={selectedPartId} onSelectPart={setSelectedPartId} />
              <PartDetail partId={selectedPartId} onSelectPart={setSelectedPartId} />
            </main>
          ) : (
            <main>
              <ChatPanel />
            </main>
          )}
        </>
      )}
    </div>
  );
}
