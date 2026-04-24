import type { GenerateMode } from '@/types'

export type WorkspaceRouteVariant = 'workspace' | 'image' | 'video' | 'image-to-video' | 'reverse-prompt'

export type RouteHandle = {
  title: string
  eyebrow: string
  section?: string
  description?: string
  breadcrumbs?: string[]
  workspaceMode?: GenerateMode
  workspaceVariant?: WorkspaceRouteVariant
}
