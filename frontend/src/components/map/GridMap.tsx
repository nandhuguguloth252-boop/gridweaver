import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, Tooltip } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import type { Node, Summary } from '../../types';

const colors: Record<Node['state'], string> = {
  IDLE: '#42d392',
  CHARGING: '#4aa3ff',
  DISCHARGING: '#ffcc66',
  FAULT: '#ff5d73'
};

const zoneCenters: Record<string, [number, number]> = {
  NORTH: [27.02, 75.82],
  SOUTH: [26.79, 75.82],
  EAST: [26.91, 75.98],
  WEST: [26.91, 75.65]
};

function transferLines(transfers: Record<string, number>) {
  return Object.entries(transfers).map(([key, kw]) => {
    const [from, to] = key.split('->');
    const start = zoneCenters[from];
    const end = zoneCenters[to];
    if (!start || !end) return null;
    return { key, start, end, kw };
  }).filter(Boolean) as Array<{key:string; start:[number,number]; end:[number,number]; kw:number}>;
}

export default function GridMap({ nodes, transfers = {} }: { nodes: Node[]; transfers?: Summary['transfers'] }) {
  const flows = transferLines(transfers);

  return (
    <div className="map">
      <MapContainer center={[26.91, 75.82]} zoom={10} scrollWheelZoom>
        <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

        {flows.map(flow => (
          <Polyline key={flow.key} positions={[flow.start, flow.end]} pathOptions={{ color: '#75c7ff', weight: Math.max(2, Math.min(8, flow.kw / 300)), opacity: 0.8, dashArray: '8 6' }}>
            <Tooltip sticky>{flow.key} · {flow.kw.toFixed(1)} kW</Tooltip>
          </Polyline>
        ))}

        {Object.entries(zoneCenters).map(([zone, center]) => (
          <CircleMarker key={zone} center={center} radius={8} pathOptions={{ color: '#d7f3ff', fillColor: '#173d59', fillOpacity: 0.9, weight: 1 }}>
            <Tooltip>{zone}</Tooltip>
          </CircleMarker>
        ))}

        {nodes.map(n => (
          <CircleMarker key={n.id} center={[n.lat, n.lon]} radius={4} pathOptions={{ color: colors[n.state], fillColor: colors[n.state], fillOpacity: 0.85, weight: 1 }}>
            <Popup>
              <b>{n.id}</b><br />
              Zone: {n.zone}<br />
              State: {n.state}<br />
              SoC: {(n.chargeKwh / Math.max(0.001, n.capacityKwh) * 100).toFixed(1)}%<br />
              Solar: {n.solarKw.toFixed(1)} kW<br />
              Load: {n.loadKw.toFixed(1)} kW<br />
              Health: {n.health.toFixed(1)}%
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  );
}
