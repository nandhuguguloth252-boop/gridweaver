import type { GridEvent, Node, Summary } from '../types';

async function json<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    let detail = response.statusText;
    try { detail = await response.text(); } catch { /* keep status */ }
    throw new Error(`${response.status} ${detail}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  summary: () => json<Summary>('/api/grid/summary'),
  nodes: (limit = 800) => json<Node[]>(`/api/grid/nodes?limit=${limit}`),
  events: (limit = 100) => json<GridEvent[]>(`/api/events?limit=${limit}`),
  balance: () => json<Record<string, unknown>[]>('/api/grid/balance', { method: 'POST' }),
  storm: (nodes: number) => json<Record<string, unknown>>(`/api/simulation/storm?nodes=${nodes}`, { method: 'POST' }),
  benchmark: (nodes: number) => json<Record<string, unknown>>(`/api/simulation/benchmark?nodes=${nodes}`, { method: 'POST' }),
  kafkaPulse: (nodes: number) => json<Record<string, unknown>>(`/api/simulation/kafka-pulse?nodes=${nodes}`, { method: 'POST' })
};
