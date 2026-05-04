import { getApiData, postApiData, putApiData, deleteApiData } from './client';

export interface Episode {
  id?: number;
  projectId: number;
  episodeNumber: number;
  title?: string;
  status?: string;
  scriptSummary?: string;
  createTime?: string;
  updateTime?: string;
}

export interface EpisodeForm {
  projectId: number;
  episodeNumber: number;
  title?: string;
  status?: string;
  scriptSummary?: string;
}

export function listProjectEpisodes(projectId: number) {
  return getApiData<Episode[]>(`/projects/${projectId}/episodes`);
}

export function getEpisode(id: number) {
  return getApiData<Episode>(`/episodes/${id}`);
}

export function createEpisode(data: EpisodeForm) {
  return postApiData<Episode>('/episodes', data);
}

export function updateEpisode(id: number, data: Partial<Episode>) {
  return putApiData<boolean>(`/episodes/${id}`, data);
}

export function deleteEpisode(id: number) {
  return deleteApiData<boolean>(`/episodes/${id}`);
}
