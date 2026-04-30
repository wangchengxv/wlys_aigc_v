import { get, post, put, del } from './request';

export interface Script {
  id?: number;
  projectId: number;
  title?: string;
  content?: string;
  version?: number;
  createTime?: string;
  updateTime?: string;
}

export interface ScriptForm {
  projectId: number;
  title?: string;
  content: string;
}

export const getScripts = (projectId: number) =>
  get<Script[]>(`/projects/${projectId}/scripts`);

export const getLatestScript = (projectId: number) =>
  get<Script>(`/projects/${projectId}/scripts/latest`);

export const getScriptDetail = (id: number) =>
  get<Script>(`/scripts/${id}`);

export const createScript = (data: ScriptForm) =>
  post<Script>('/scripts', data);

export const updateScript = (id: number, data: Partial<Script>) =>
  put<boolean>(`/scripts/${id}`, data);

export const deleteScript = (id: number) =>
  del<boolean>(`/scripts/${id}`);

export const saveOrUpdateScript = (projectId: number, data: { title?: string; content: string }) =>
  post<Script>(`/projects/${projectId}/scripts/save-or-update`, data);