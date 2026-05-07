import axios from "axios";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  traceId: string;
}

export const http = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("miioo_token") ?? sessionStorage.getItem("miioo_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
