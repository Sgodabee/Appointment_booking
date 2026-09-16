import client from './client';

export const getBranches = () => client.get('/branches');
export const getBranchById = (id) => client.get(`/branches/${id}`);
