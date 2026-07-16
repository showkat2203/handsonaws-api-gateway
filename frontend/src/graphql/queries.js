import { gql } from '@apollo/client';

export const SEARCH_PARTS = gql`
  query SearchParts($filter: PartFilterInput) {
    searchParts(filter: $filter) {
      totalCount
      page
      size
      items {
        id
        name
        type
        lifecycleState
        revision
        supplier {
          id
          name
        }
      }
    }
  }
`;

export const GET_PART = gql`
  query GetPart($id: ID!) {
    part(id: $id) {
      id
      name
      description
      type
      lifecycleState
      revision
      supplierId
      supplier {
        id
        name
        contactEmail
      }
      attributes {
        key
        value
      }
      createdAt
      updatedAt
    }
  }
`;

export const GET_BOM = gql`
  query GetBom($partId: ID!) {
    bom(partId: $partId) {
      quantity
      part {
        id
        name
        type
        lifecycleState
      }
      children {
        quantity
        part {
          id
          name
          type
          lifecycleState
        }
        children {
          quantity
          part {
            id
            name
            type
            lifecycleState
          }
          children {
            quantity
            part {
              id
              name
              type
              lifecycleState
            }
          }
        }
      }
    }
  }
`;

export const WHERE_USED = gql`
  query WhereUsed($partId: ID!) {
    whereUsed(partId: $partId) {
      id
      name
      type
      lifecycleState
    }
  }
`;

export const LIST_SUPPLIERS = gql`
  query ListSuppliers($page: Int, $size: Int) {
    suppliers(page: $page, size: $size) {
      totalCount
      items {
        id
        name
        active
      }
    }
  }
`;

export const UPDATE_LIFECYCLE = gql`
  mutation UpdateLifecycle($id: ID!, $state: LifecycleState!) {
    updateLifecycle(id: $id, state: $state) {
      id
      lifecycleState
    }
  }
`;
