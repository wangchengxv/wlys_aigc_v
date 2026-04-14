import type { SerialisableGraph } from '@comfyorg/litegraph/dist/types/serialisation'

export const CANVAS_GRAPH_SCHEMA_VERSION = 1

export type GraphViewport = {
  offset: [number, number]
  scale: number
}

export type GraphState = {
  schemaVersion: number
  updatedAt: number
  graph: SerialisableGraph
  viewport: GraphViewport
}
