import { get, put } from './request';
import type { ProjectConfig } from '@/types/project';

export const getProjectConfig = (projectId: number) =>
  get<ProjectConfig>(`/projects/${projectId}/config`);

export const updateProjectConfig = (projectId: number, config: Partial<ProjectConfig>) =>
  put<boolean>(`/projects/${projectId}/config`, config);

export const saveScript = (projectId: number, script: string) =>
  put<boolean>(`/projects/${projectId}/config`, { script });

export const saveBatchConfig = (
  projectId: number,
  config: {
    batchModel?: string;
    batchRatio?: string;
    batchQuality?: string;
    batchMethod?: string;
  }
) => put<boolean>(`/projects/${projectId}/config`, config);