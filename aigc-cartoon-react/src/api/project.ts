import { get, post, put, del } from './request';
import type { Project, ProjectForm } from '@/types/project';

export const getProjectList = () => get<Project[]>('/projects');

export const getProjectDetail = (id: number) => get<Project>(`/projects/${id}`);

export const createProject = (data: ProjectForm) => {
  const styleTemplateId = data.styleTemplateId || data.styleKey || data.style || '';
  const visualStylePrompt = data.visualStylePrompt || data.customStyleText || '';
  const visualStyleLongTextMode = data.visualStyleLongTextMode ?? Boolean(data.customStyleText);
  const visualStyleMode = data.visualStyleMode || (visualStyleLongTextMode ? 'custom' : 'preset');
  return post<Project>('/projects', {
    name: data.name,
    description: data.description || '',
    style: data.style || styleTemplateId,
    styleKey: data.styleKey || data.style || '',
    styleTemplateId,
    visualStylePrompt,
    visualStyleMode,
    visualStyleLongTextMode,
    customStyleText: data.customStyleText || visualStylePrompt,
    aspectRatio: data.aspectRatio || '16:9',
    coverImage: data.coverImage || '',
    userId: 1,
  });
};

export const updateProject = (id: number, data: Partial<Project>) =>
  put<boolean>(`/projects/${id}`, data);

export const deleteProject = (id: number) => del<boolean>(`/projects/${id}`);

export const renameProject = (id: number, name: string) =>
  put<boolean>(`/projects/${id}/rename`, { name });

export const uploadProjectCover = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return post<{ url: string }>('/projects/upload-cover', formData as unknown as object);
};
