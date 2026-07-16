import React, { useState } from 'react';
import { useQuery } from '@apollo/client';
import { SEARCH_PARTS } from '../graphql/queries';

const PART_TYPES = ['RACK', 'SERVER', 'PSU', 'NIC', 'CABLE', 'OTHER'];
const LIFECYCLE_STATES = ['DESIGN', 'ACTIVE', 'EOL'];

export default function PartsBrowser({ selectedPartId, onSelectPart }) {
  const [nameContains, setNameContains] = useState('');
  const [type, setType] = useState('');
  const [lifecycleState, setLifecycleState] = useState('');

  const filter = {
    nameContains: nameContains || null,
    type: type || null,
    lifecycleState: lifecycleState || null,
    page: 0,
    size: 50,
  };

  const { data, loading, error } = useQuery(SEARCH_PARTS, { variables: { filter } });

  return (
    <div className="parts-browser">
      <h2>Parts</h2>
      <div className="filters">
        <input
          placeholder="Search by name..."
          value={nameContains}
          onChange={(e) => setNameContains(e.target.value)}
        />
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">All types</option>
          {PART_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <select value={lifecycleState} onChange={(e) => setLifecycleState(e.target.value)}>
          <option value="">All lifecycle states</option>
          {LIFECYCLE_STATES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {loading && <p>Loading parts...</p>}
      {error && <p className="error">Failed to load parts: {error.message}</p>}
      {data && (
        <>
          <p className="hint">{data.searchParts.totalCount} part(s)</p>
          <ul className="part-list">
            {data.searchParts.items.map((p) => (
              <li
                key={p.id}
                className={p.id === selectedPartId ? 'selected' : ''}
                onClick={() => onSelectPart(p.id)}
              >
                <strong>{p.name}</strong> <code>{p.id}</code>
                <br />
                <span className="muted">{p.type}</span>{' '}
                <span className={`badge badge-${p.lifecycleState.toLowerCase()}`}>{p.lifecycleState}</span>
                {p.supplier && <span className="muted"> · {p.supplier.name}</span>}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
