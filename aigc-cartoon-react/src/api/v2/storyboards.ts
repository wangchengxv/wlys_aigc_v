import { deleteApiData, getApiData, postApiData, putApiData } from './client';

export type Storyboard = {
  id?: number;
  scriptId: number;
  sceneNumber?: number;
  sceneDescription?: string;
  duration?: number;
  bgPrompt?: string;
  characterPrompt?: string;
  audioUrl?: string;
  createTime?: string;
  updateTime?: string;
};

export function listStoryboards(scriptId: number) {
  return getApiData<Storyboard[]>(`/scripts/${scriptId}/storyboards`);
}

export function getStoryboard(id: number) {
  return getApiData<Storyboard>(`/storyboards/${id}`);
}

export function createStoryboard(payload: Storyboard) {
  return postApiData<Storyboard>('/storyboards', payload);
}

export function updateStoryboard(id: number, payload: Partial<Storyboard>) {
  return putApiData<boolean>(`/storyboards/${id}`, payload);
}

export function deleteStoryboard(id: number) {
  return deleteApiData<boolean>(`/storyboards/${id}`);
}

export function saveStoryboardsBatch(payload: Storyboard[]) {
  return postApiData<boolean>('/storyboards/batch', payload);
}
