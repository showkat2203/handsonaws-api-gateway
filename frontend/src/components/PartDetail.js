import React from 'react';
import { useQuery } from '@apollo/client';
import { GET_PART } from '../graphql/queries';
import BomTree from './BomTree';
import WhereUsed from './WhereUsed';

export default function PartDetail({ partId, onSelectPart }) {
  const { data, loading, error } = useQuery(GET_PART, { variables: { id: partId }, skip: !partId });

  if (!partId) {
    return <p className="hint">Select a part to see its details.</p>;
  }
  if (loading) return <p>Loading part...</p>;
  if (error) return <p className="error">Failed to load part: {error.message}</p>;

  const part = data?.part;
  if (!part) return <p>Part not found.</p>;

  return (
    <div className="part-detail">
      <h2>
        {part.name} <code>{part.id}</code>
      </h2>
      <p>{part.description}</p>
      <table className="kv-table">
        <tbody>
          <tr>
            <th>Type</th>
            <td>{part.type}</td>
          </tr>
          <tr>
            <th>Lifecycle</th>
            <td>
              <span className={`badge badge-${part.lifecycleState.toLowerCase()}`}>{part.lifecycleState}</span>
            </td>
          </tr>
          <tr>
            <th>Revision</th>
            <td>{part.revision}</td>
          </tr>
          <tr>
            <th>Supplier</th>
            <td>{part.supplier ? `${part.supplier.name} (${part.supplier.id})` : '—'}</td>
          </tr>
          {part.attributes.map((kv) => (
            <tr key={kv.key}>
              <th>{kv.key}</th>
              <td>{kv.value}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <BomTree partId={part.id} onSelectPart={onSelectPart} />
      <WhereUsed partId={part.id} onSelectPart={onSelectPart} />
    </div>
  );
}
