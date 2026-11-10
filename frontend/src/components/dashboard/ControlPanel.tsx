import { useState } from 'react';
import { api } from '../../api/client';

export default function ControlPanel({ onDone }: { onDone: () => void }) {
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const run = async (fn: () => Promise<unknown>, label: string) => {
    setBusy(true);
    setMsg(`${label}…`);
    try {
      const result = await fn();
      setMsg(JSON.stringify(result));
      onDone();
    } catch (error) {
      setMsg(error instanceof Error ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="controlPanel">
      <div className="panelTitle">Scenario Lab</div>
      <div className="row">
        <button disabled={busy} onClick={() => run(() => api.storm(50000), 'Storm 50k')}>⛈ Storm 50k</button>
        <button disabled={busy} onClick={() => run(() => api.benchmark(50000), 'Virtual Thread Benchmark')}>⚡ Benchmark 50k</button>
        <button disabled={busy} onClick={() => run(() => api.kafkaPulse(1000), 'Kafka Pulse 1k')}>📡 Kafka Pulse 1k</button>
        <button disabled={busy} onClick={() => run(() => api.balance(), 'Regional Auto-Balance')}>↔ Auto-Balance</button>
      </div>
      <div className="result">{msg || 'Ready for a scenario.'}</div>
    </div>
  );
}
