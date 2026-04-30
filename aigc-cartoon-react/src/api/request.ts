import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
});

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message.error('未授权，请重新登录');
          localStorage.removeItem('token');
          break;
        case 403:
          message.error('禁止访问');
          break;
        case 404:
          message.error('请求资源不存在');
          break;
        case 500:
          message.error('服务器错误');
          break;
        default:
          message.error(error.message);
      }
    } else {
      message.error('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);

export default request;

export const get = <T>(url: string, params?: object) =>
  request.get<{ data: T }>(url, { params }).then((res) => res.data);

export const post = <T>(url: string, data?: object) =>
  request.post<{ data: T }>(url, data).then((res) => res.data);

export const put = <T>(url: string, data?: object) =>
  request.put<{ data: T }>(url, data).then((res) => res.data);

export const del = <T>(url: string, params?: object) =>
  request.delete<{ data: T }>(url, { params }).then((res) => res.data);
