import { get, post, put, del } from './request';

export interface Character {
  id?: number;
  projectId: number;
  name: string;
  description?: string;
  appearancePrompt?: string;
  imageUrl?: string;
  voiceConfig?: string;
  createTime?: string;
  updateTime?: string;
}

export interface CharacterForm {
  projectId: number;
  name: string;
  description?: string;
  appearancePrompt?: string;
  imageUrl?: string;
  voiceConfig?: string;
}

export const getCharacters = (projectId: number) =>
  get<Character[]>(`/projects/${projectId}/characters`);

export const getCharacterDetail = (id: number) =>
  get<Character>(`/characters/${id}`);

export const createCharacter = (data: CharacterForm) =>
  post<Character>('/characters', data);

export const updateCharacter = (id: number, data: Partial<Character>) =>
  put<boolean>(`/characters/${id}`, data);

export const deleteCharacter = (id: number) =>
  del<boolean>(`/characters/${id}`);

export const uploadCharacterImage = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return post<{ url: string }>('/characters/upload-image', formData as unknown as object);
};