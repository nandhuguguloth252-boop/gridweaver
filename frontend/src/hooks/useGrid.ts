import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api/client';
import type { GridEvent, Node, Summary } from '../types';

const emptyCounts = { IDLE: 0, CHARGING: 0, DISCHARGING: 0, FAULT: 0 } as const;

export function useGrid() {
  const [summary, setSummary] = useState<Summary>();
  const [nodes, setNodes] = useState<Node[]>([]);
  const [events, setEvents] = useState<GridEvent[]>([]);
  const [online, setOnline] = useState(false);
  const [error, setError] = useState<string>();
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectRef = useRef<number | null>(null);
  const mountedRef = useRef(true);

  const refresh = useCallback(async () => {
    try {
      const [s, n, e] = await Promise.all([
        api.summary(),
        api.nodes(800),
        api.events(80)
      ]);
      if (!mountedRef.current) return;
      setSummary(s);
      setNodes(n);
      setEvents(e);
      setError(undefined);
    } catch (err) {
      if (!mountedRef.current) return;
      setError(err instanceof Error ? err.message : 'Unable to reach GridWeaver backend');
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    let stopped = false;

    const scheduleReconnect = () => {
      if (stopped || reconnectRef.current) return;
      reconnectRef.current = window.setTimeout(() => {
        reconnectRef.current = null;
        connect();
      }, 1500);
    };

    const connect = () => {
      if (stopped) return;
      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
      const socket = new WebSocket(`${protocol}://${window.location.host}/ws/grid`);
      wsRef.current = socket;

      socket.onopen = () => {
        if (mountedRef.current) setOnline(true);
      };

      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          if (message.type === 'node.update' && message.node) {
            setNodes(prev => {
              const index = prev.findIndex(node => node.id === message.node.id);
              if (index === -1) return prev;
              const next = [...prev];
              next[index] = message.node as Node;
              return next;
            });
          }
          if (message.type === 'grid.summary' && message.payload) {
            setSummary(message.payload as Summary);
          }
        } catch (err) {
          console.warn('Invalid WebSocket payload', err);
        }
      };

      socket.onerror = () => socket.close();
      socket.onclose = () => {
        if (mountedRef.current) setOnline(false);
        scheduleReconnect();
      };
    };

    void refresh();
    const refreshId = window.setInterval(() => void refresh(), 3000);
    connect();

    return () => {
      stopped = true;
      mountedRef.current = false;
      window.clearInterval(refreshId);
      if (reconnectRef.current) window.clearTimeout(reconnectRef.current);
      reconnectRef.current = null;
      wsRef.current?.close();
      wsRef.current = null;
    };
  }, [refresh]);

  const counts = useMemo(() => ({ ...emptyCounts, ...(summary?.states ?? {}) }), [summary]);
  return { summary, nodes, events, counts, online, error, refresh };
}
