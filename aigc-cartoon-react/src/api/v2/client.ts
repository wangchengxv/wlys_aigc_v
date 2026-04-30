import axios, { AxiosError } from 'axios';
import { message } from 'antd';

export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
};

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiV2 = axios.create({
  baseURL,
  timeout: 30_000,
});

apiV2.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiV2.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown>;
    if (payload?.code === 200) return response;
    message.error(payload?.message || '请求失败');
    return Promise.reject(new Error(payload?.message || '请求失败'));
  },
  (error: AxiosError) => {
    const status = error.response?.status;
    if (status === 401) message.error('未授权，请重新登录');
    else if (status === 403) message.error('拒绝访问');
    else if (status === 404) message.error('请求地址不存在');
    else if (status === 500) message.error('服务器错误');
    else message.error('网络错误');
    return Promise.reject(error);
  },
);

export async function getApiData<T>(path: string, params?: Record<string, unknown>) {
  const res = await apiV2.get<ApiEnvelope<T>>(path, { params });
  return res.data.data;
}

export async function postApiData<T>(path: string, data?: unknown) {
  const res = await apiV2.post<ApiEnvelope<T>>(path, data);
  return res.data.data;
}

export async function putApiData<T>(path: string, data?: unknown) {
  const res = await apiV2.put<ApiEnvelope<T>>(path, data);
  return res.data.data;
}

export async function deleteApiData<T>(path: string) {
  const res = await apiV2.delete<ApiEnvelope<T>>(path);
  return res.data.data;
}
