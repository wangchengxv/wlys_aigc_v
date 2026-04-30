import { deleteApiData, getApiData, postApiData, putApiData } from './client';

export type Script = {
  id?: number;
  projectId: number;
  title?: string;
  content?: string;
  version?: number;
  createTime?: string;
  updateTime?: string;
};

export function listScripts(projectId: number) {
  return getApiData<Script[]>(`/projects/${projectId}/scripts`);
}

export function getLatestScript(projectId: number) {
  return getApiData<Script | null>(`/projects/${projectId}/scripts/latest`);
}

export function getScript(id: number) {
  return getApiData<Script>(`/scripts/${id}`);
}

export function createScript(payload: Script) {
  return postApiData<Script>('/scripts', payload);
}

export function updateScript(id: number, payload: Partial<Script>) {
  return putApiData<boolean>(`/scripts/${id}`, payload);
}

export function deleteScript(id: number) {
  return deleteApiData<boolean>(`/scripts/${id}`);
}

export function saveOrUpdateLatestScript(projectId: number, payload: { title?: string; content?: string }) {
  return postApiData<Script>(`/projects/${projectId}/scripts/save-or-update`, payload);
}
