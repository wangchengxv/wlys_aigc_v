import { get, post, put, del } from './request';

export interface Storyboard {
  id?: number;
  scriptId: number;
  sceneNumber: number;
  sceneDescription?: string;
  duration?: number;
  bgPrompt?: string;
  characterPrompt?: string;
  audioUrl?: string;
  createTime?: string;
  updateTime?: string;
}

export interface StoryboardForm {
  scriptId: number;
  sceneNumber: number;
  sceneDescription?: string;
  duration?: number;
  bgPrompt?: string;
  characterPrompt?: string;
  audioUrl?: string;
}

export const getStoryboards = (scriptId: number) =>
  get<Storyboard[]>(`/scripts/${scriptId}/storyboards`);

export const getStoryboardDetail = (id: number) =>
  get<Storyboard>(`/storyboards/${id}`);

export const createStoryboard = (data: StoryboardForm) =>
  post<Storyboard>('/storyboards', data);

export const updateStoryboard = (id: number, data: Partial<Storyboard>) =>
  put<boolean>(`/storyboards/${id}`, data);

export const deleteStoryboard = (id: number) =>
  del<boolean>(`/storyboards/${id}`);

export const batchCreateStoryboard = (storyboards: StoryboardForm[]) =>
  post<boolean>('/storyboards/batch', storyboards);