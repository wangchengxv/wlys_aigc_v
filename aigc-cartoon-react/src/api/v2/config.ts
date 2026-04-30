import type { ProjectConfig } from '@/types/project';
import { getApiData, putApiData } from './client';

export function getProjectConfig(projectId: number) {
  return getApiData<ProjectConfig>(`/projects/${projectId}/config`);
}

export function updateProjectConfig(projectId: number, payload: Partial<ProjectConfig>) {
  return putApiData<boolean>(`/projects/${projectId}/config`, payload);
}
