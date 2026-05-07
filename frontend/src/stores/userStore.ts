import { create } from "zustand";

interface UserState {
  userId?: number;
  phone?: string;
  username?: string;
  token?: string;
  setAuth: (payload: { userId: number; username: string; token: string; phone?: string }) => void;
  logout: () => void;
}

export const useUserStore = create<UserState>((set) => ({
  setAuth: (payload) =>
    set(() => ({
      userId: payload.userId,
      username: payload.username,
      token: payload.token,
      phone: payload.phone,
    })),
  logout: () => set(() => ({ userId: undefined, username: undefined, token: undefined, phone: undefined })),
}));
