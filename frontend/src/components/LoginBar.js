import React, { useState } from 'react';
import { login, logout, getUser } from '../auth';

export default function LoginBar({ onAuthChange }) {
  const [username, setUsername] = useState('engineer');
  const [password, setPassword] = useState('engineer123');
  const [error, setError] = useState(null);
  const user = getUser();

  async function handleLogin(e) {
    e.preventDefault();
    setError(null);
    try {
      await login(username, password);
      onAuthChange();
    } catch (err) {
      setError(err.message);
    }
  }

  function handleLogout() {
    logout();
    onAuthChange();
  }

  if (user) {
    return (
      <div className="login-bar">
        <span>
          Signed in as <strong>{user.username}</strong> ({user.roles.join(', ')})
        </span>
        <button onClick={handleLogout}>Log out</button>
      </div>
    );
  }

  return (
    <form className="login-bar" onSubmit={handleLogin}>
      <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="username" />
      <input
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="password"
        type="password"
      />
      <button type="submit">Log in</button>
      {error && <span className="error">{error}</span>}
      <span className="hint">demo users: admin/admin123, engineer/engineer123, viewer/viewer123</span>
    </form>
  );
}
