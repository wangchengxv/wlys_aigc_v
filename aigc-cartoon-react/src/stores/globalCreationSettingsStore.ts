import { useSyncExternalStore } from 'react';
import type { VisualStyleMode } from '@/types/project';

const STORAGE_KEY = 'aigc_cartoon_global_creation_settings_v1';

export interface GlobalCreationSettings {
  defaultAspectRatio: string;
  defaultStyleKey: string;
  defaultStyleTemplateId: string;
  defaultVisualStylePrompt: string;
  defaultVisualStyleMode: VisualStyleMode;
  defaultVisualStyleLongTextMode: boolean;
}

export interface ProjectCreationOverride {
  aspectRatio?: string;
  styleKey?: string;
  styleTemplateId?: string;
  visualStylePrompt?: string;
  visualStyleMode?: VisualStyleMode;
  visualStyleLongTextMode?: boolean;
}

interface GlobalCreationSettingsState {
  settings: GlobalCreationSettings;
  projectOverrides: Record<string, ProjectCreationOverride>;
}

const DEFAULT_SETTINGS: GlobalCreationSettings = {
  defaultAspectRatio: '16:9',
  defaultStyleKey: 'anime_japanese',
  defaultStyleTemplateId: 'template-anime-japanese',
  defaultVisualStylePrompt: 'Japanese anime style, clean lineart, soft cinematic light, dynamic camera framing',
  defaultVisualStyleMode: 'preset',
  defaultVisualStyleLongTextMode: false,
};

const DEFAULT_STATE: GlobalCreationSettingsState = {
  settings: DEFAULT_SETTINGS,
  projectOverrides: {},
};

function loadState(): GlobalCreationSettingsState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_STATE;
    const parsed = JSON.parse(raw) as Partial<GlobalCreationSettingsState>;
    return {
      settings: {
        ...DEFAULT_SETTINGS,
        ...(parsed.settings || {}),
      },
      projectOverrides: parsed.projectOverrides || {},
    };
  } catch {
    return DEFAULT_STATE;
  }
}

let state = loadState();
const listeners = new Set<() => void>();

function emitChange() {
  listeners.forEach((listener) => listener());
}

function persistState(nextState: GlobalCreationSettingsState) {
  state = nextState;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
  emitChange();
}

export function useGlobalCreationSettingsStore() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => state,
    () => state,
  );
}

export function patchGlobalCreationSettings(partial: Partial<GlobalCreationSettings>) {
  persistState({
    ...state,
    settings: {
      ...state.settings,
      ...partial,
    },
  });
}

export function setProjectCreationOverride(projectKey: string, override: ProjectCreationOverride) {
  if (!projectKey.trim()) return;
  persistState({
    ...state,
    projectOverrides: {
      ...state.projectOverrides,
      [projectKey]: override,
    },
  });
}

export function getEffectiveCreationSettings(projectKey?: string): ProjectCreationOverride {
  const projectOverride = projectKey ? state.projectOverrides[projectKey] : undefined;
  return {
    aspectRatio: projectOverride?.aspectRatio || state.settings.defaultAspectRatio,
    styleKey: projectOverride?.styleKey || state.settings.defaultStyleKey,
    styleTemplateId: projectOverride?.styleTemplateId || state.settings.defaultStyleTemplateId,
    visualStylePrompt: projectOverride?.visualStylePrompt || state.settings.defaultVisualStylePrompt,
    visualStyleMode: projectOverride?.visualStyleMode || state.settings.defaultVisualStyleMode,
    visualStyleLongTextMode:
      projectOverride?.visualStyleLongTextMode ?? state.settings.defaultVisualStyleLongTextMode,
  };
}
