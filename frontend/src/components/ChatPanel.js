import React, { useState } from 'react';
import { getToken } from '../auth';

const CHAT_URL = process.env.REACT_APP_CHAT_URL || 'http://localhost:8081/chat';

export default function ChatPanel() {
  const [input, setInput] = useState('');
  const [turns, setTurns] = useState([]); // {role, content, toolCalls?}
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function send(e) {
    e.preventDefault();
    const message = input.trim();
    if (!message) return;

    const history = turns.map(({ role, content }) => ({ role, content }));
    setTurns((t) => [...t, { role: 'user', content: message }]);
    setInput('');
    setLoading(true);
    setError(null);

    try {
      const token = getToken();
      const response = await fetch(CHAT_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ message, history }),
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || `Chat request failed (${response.status})`);
      }
      const data = await response.json();
      setTurns((t) => [...t, { role: 'assistant', content: data.answer, toolCalls: data.toolCalls }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="chat-panel">
      <h2>Ask the PLM assistant</h2>
      <p className="hint">
        Try: "What assemblies use PSU-2200?", "Show the BOM for rack R-14", "Which parts from
        supplier Acme are EOL?"
      </p>
      <div className="chat-history">
        {turns.map((turn, idx) => (
          <div key={idx} className={`chat-turn chat-turn-${turn.role}`}>
            <div className="chat-bubble">{turn.content}</div>
            {turn.toolCalls && turn.toolCalls.length > 0 && (
              <details className="tool-trace">
                <summary>{turn.toolCalls.length} tool call(s)</summary>
                {turn.toolCalls.map((tc, i) => (
                  <div key={i} className="tool-call">
                    <div>
                      <strong>{tc.tool}</strong>({JSON.stringify(tc.params)}) — {tc.durationMs}ms
                    </div>
                    {tc.error ? (
                      <pre className="error">{tc.error}</pre>
                    ) : (
                      <pre>{JSON.stringify(tc.result, null, 2)}</pre>
                    )}
                  </div>
                ))}
              </details>
            )}
          </div>
        ))}
        {loading && <div className="chat-turn chat-turn-assistant">Thinking…</div>}
      </div>
      {error && <p className="error">{error}</p>}
      <form onSubmit={send} className="chat-input">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask a question about parts, BOMs, suppliers, or change orders..."
        />
        <button type="submit" disabled={loading}>
          Send
        </button>
      </form>
    </div>
  );
}
