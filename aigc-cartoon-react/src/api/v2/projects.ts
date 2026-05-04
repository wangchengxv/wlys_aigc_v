import type { Project, ProjectForm } from '@/types/project';
import { apiV2, type ApiEnvelope } from './client';
import { deleteApiData, getApiData, postApiData, putApiData } from './client';

export function listProjects() {
  return getApiData<Project[]>('/projects');
}

export function getProject(id: number) {
  return getApiData<Project>(`/projects/${id}`);
}

export function createProject(payload: ProjectForm) {
  return postApiData<Project>('/projects', payload);
}

export function updateProject(id: number, payload: Partial<Project>) {
  return putApiData<boolean>(`/projects/${id}`, payload);
}

export function deleteProject(id: number) {
  return deleteApiData<boolean>(`/projects/${id}`);
}

export async function renameProject(id: number, name: string) {
  return putApiData<boolean>(`/projects/${id}/rename`, { name });
}

export async function uploadProjectCover(file: File) {
  const form = new FormData();
  form.append('file', file);
  const res = await apiV2.post<ApiEnvelope<{ url: string }>>('/projects/upload-cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data.data;
}
