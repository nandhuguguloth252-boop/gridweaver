import { useState } from 'react';
import { useGrid } from './hooks/useGrid';
import KpiCard from './components/common/KpiCard';
import ControlPanel from './components/dashboard/ControlPanel';
import ZoneTable from './components/dashboard/ZoneTable';
import StateLegend from './components/dashboard/StateLegend';
import GridMap from './components/map/GridMap';
import EventLog from './components/eventlog/EventLog';
import './styles.css';

export default function App() {
  const { summary, nodes, events, counts, online, error, refresh } = useGrid();
  const [tab, setTab] = useState<'overview' | 'events'>('overview');

  return (
    <div className="app">
      <header>
        <div>
          <div className="brand">GRIDWEAVER</div>
          <div className="subtitle">Virtual Thread IoT Microgrid State Engine</div>
        </div>
        <div className="status">
          <span className={online ? 'onlineDot' : 'offlineDot'} />
          <span>{online ? 'WebSocket LIVE' : 'Connecting to Grid...'}</span>
        </div>
      </header>

      {error && (
        <div className="errorBanner">
          <span>⚠️ {error}</span>
          <button onClick={() => void refresh()} style={{ background: '#254460', border: 'none', color: '#fff', padding: '6px 14px', borderRadius: 6, cursor: 'pointer' }}>
            Reconnect
          </button>
        </div>
      )}

      {!summary ? (
        <section className="loading panel">
          <div className="panelTitle">GridWeaver Microgrid Engine</div>
          <p>
            Connecting to Java 21 Virtual Thread Backend at <code>http://localhost:8080</code>...
            <br />
            Make sure the backend is running.
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
            <button onClick={() => void refresh()}>🔄 Retry Connection</button>
          </div>
        </section>
      ) : (
        <main>
          <section className="kpis">
            <KpiCard
              label="Grid Nodes"
              value={(summary.nodes ?? 0).toLocaleString()}
              sub={`${counts.FAULT ?? 0} faulted`}
            />
            <KpiCard
              label="Generation"
              value={`${(summary.solarKw ?? 0).toFixed(0)} kW`}
              sub={`load ${(summary.loadKw ?? 0).toFixed(0)} kW`}
            />
            <KpiCard
              label="Resilience"
              value={`${(summary.resilience ?? 0).toFixed(1)}%`}
              sub={`health ${(summary.avgHealth ?? 0).toFixed(1)}%`}
            />
            <KpiCard
              label="Virtual Tasks"
              value={(summary.virtualTasksCompleted ?? 0).toLocaleString()}
              sub={`${(summary.virtualTasksSubmitted ?? 0).toLocaleString()} submitted`}
            />
          </section>

          <ControlPanel onDone={refresh} />

          <div className="tabs">
            <button className={tab === 'overview' ? 'active' : ''} onClick={() => setTab('overview')}>
              Grid Overview & GIS Map
            </button>
            <button className={tab === 'events' ? 'active' : ''} onClick={() => setTab('events')}>
              Audit Timeline
            </button>
          </div>

          {tab === 'overview' ? (
            <>
              <section className="mapPanel">
                <div className="panelTitle">
                  <span>GIS Digital Twin</span>
                  <StateLegend />
                </div>
                <GridMap nodes={nodes ?? []} transfers={summary.transfers ?? {}} />
              </section>
              <section className="twoCol">
                <ZoneTable zones={summary.zones ?? []} />
                <div className="panel">
                  <div className="panelTitle">State Distribution</div>
                  <div className="states">
                    {Object.entries(counts).map(([k, v]) => (
                      <div key={k}>
                        <span>{k}</span>
                        <b>{v.toLocaleString()}</b>
                        <div className="bar">
                          <i
                            style={{
                              width: `${Math.min(100, (v / Math.max(1, summary.nodes ?? 1)) * 100)}%`
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            </>
          ) : (
            <section>
              <EventLog events={events ?? []} />
            </section>
          )}
        </main>
      )}

      <footer>GridWeaver demo · Java 21 · Virtual Threads · Spring StateMachine · Apache Kafka · React + Leaflet</footer>
    </div>
  );
}
