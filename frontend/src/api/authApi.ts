import { http, type ApiResponse } from "./http";

export interface PhoneLoginPayload {
  phone: string;
  code: string;
  autoLogin: boolean;
}

export interface SendCodeResponse {
  resendAfterSeconds: number;
  debugCode?: string;
}

export interface LoginUserInfo {
  userId: number;
  phone?: string;
  username: string;
}

export interface PhoneLoginResponse {
  token: string;
  userInfo: LoginUserInfo;
}

export interface WechatQrcodeResponse {
  supported: boolean;
  message: string;
}

export interface WechatPollResponse {
  status: "pending" | "need_bind" | "success" | "unsupported";
  openid?: string;
  token?: string;
  userInfo?: LoginUserInfo;
}

export async function sendCodeApi(phone: string) {
  const resp = await http.get<ApiResponse<SendCodeResponse>>("/auth/send-code", { params: { phone } });
  return resp.data.data;
}

export async function phoneLoginApi(payload: PhoneLoginPayload) {
  const resp = await http.post<ApiResponse<PhoneLoginResponse>>("/auth/login/phone", payload);
  return resp.data.data;
}

export async function getWechatQrcodeApi() {
  const resp = await http.get<ApiResponse<WechatQrcodeResponse>>("/auth/wechat/qrcode");
  return resp.data.data;
}

export async function pollWechatStatusApi(pollId: string) {
  const resp = await http.get<ApiResponse<WechatPollResponse>>("/auth/wechat/poll", { params: { id: pollId } });
  return resp.data.data;
}
