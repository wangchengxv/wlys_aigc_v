import { create } from "zustand";

interface ProjectState {
  currentProjectId?: number;
  setCurrentProject: (projectId?: number) => void;
}

export const useProjectStore = create<ProjectState>((set) => ({
  currentProjectId: undefined,
  setCurrentProject: (projectId) => set(() => ({ currentProjectId: projectId })),
}));
