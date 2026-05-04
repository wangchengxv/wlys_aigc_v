import { getApiData, postApiData, putApiData, deleteApiData } from './client';

export interface Video {
  id?: number;
  projectId: number;
  storyboardId?: number;
  videoUrl?: string;
  thumbnailUrl?: string;
  duration?: number;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface VideoForm {
  projectId: number;
  storyboardId?: number;
  videoUrl?: string;
  thumbnailUrl?: string;
  duration?: number;
  status?: string;
}

export function listProjectVideos(projectId: number) {
  return getApiData<Video[]>(`/projects/${projectId}/videos`);
}

export function getVideo(id: number) {
  return getApiData<Video>(`/videos/${id}`);
}

export function createVideo(data: VideoForm) {
  return postApiData<Video>('/videos', data);
}

export function updateVideo(id: number, data: Partial<Video>) {
  return putApiData<boolean>(`/videos/${id}`, data);
}

export function deleteVideo(id: number) {
  return deleteApiData<boolean>(`/videos/${id}`);
}