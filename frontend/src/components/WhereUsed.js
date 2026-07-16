import React from 'react';
import { useQuery } from '@apollo/client';
import { WHERE_USED } from '../graphql/queries';

export default function WhereUsed({ partId, onSelectPart }) {
  const { data, loading, error } = useQuery(WHERE_USED, { variables: { partId }, skip: !partId });

  if (!partId) return null;
  if (loading) return <p>Loading where-used...</p>;
  if (error) return <p className="error">Failed to load where-used: {error.message}</p>;

  const parts = data?.whereUsed || [];

  return (
    <div className="where-used">
      <h3>Where Used</h3>
      {parts.length === 0 ? (
        <p>Not used in any assembly.</p>
      ) : (
        <ul>
          {parts.map((p) => (
            <li key={p.id} className="bom-node" onClick={() => onSelectPart(p.id)}>
              {p.name} <code>{p.id}</code>{' '}
              <span className={`badge badge-${p.lifecycleState.toLowerCase()}`}>{p.lifecycleState}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
