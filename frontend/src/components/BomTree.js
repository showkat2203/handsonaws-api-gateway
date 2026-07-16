import React from 'react';
import { useQuery } from '@apollo/client';
import { GET_BOM } from '../graphql/queries';

function TreeNode({ node, onSelectPart }) {
  return (
    <li>
      <span className="bom-node" onClick={() => onSelectPart(node.part.id)}>
        {node.part.name} <code>{node.part.id}</code>
        {' '}
        <span className={`badge badge-${node.part.lifecycleState.toLowerCase()}`}>{node.part.lifecycleState}</span>
        {' '}x{node.quantity}
      </span>
      {node.children && node.children.length > 0 && (
        <ul>
          {node.children.map((child) => (
            <TreeNode key={child.part.id} node={child} onSelectPart={onSelectPart} />
          ))}
        </ul>
      )}
    </li>
  );
}

export default function BomTree({ partId, onSelectPart }) {
  const { data, loading, error } = useQuery(GET_BOM, { variables: { partId }, skip: !partId });

  if (!partId) return null;
  if (loading) return <p>Loading BOM...</p>;
  if (error) return <p className="error">Failed to load BOM: {error.message}</p>;
  if (!data || !data.bom) return <p>No BOM found for this part.</p>;

  return (
    <div className="bom-tree">
      <h3>Bill of Materials</h3>
      <ul>
        <TreeNode node={data.bom} onSelectPart={onSelectPart} />
      </ul>
    </div>
  );
}
