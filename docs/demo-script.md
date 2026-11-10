# GridWeaver 5-minute demo script

## 0:00 — Start

Run `START-GRIDWEAVER.cmd` and open http://localhost:8080.

## 0:30 — Explain the architecture

The browser talks to one Nginx endpoint. Nginx serves the React GIS dashboard and routes API/WebSocket traffic to Spring Boot. Kafka sits inside the Docker network and buffers telemetry. Spring StateMachine converts telemetry pressure into battery states.

## 1:00 — Show the map

Point out the live state legend: IDLE, CHARGING, DISCHARGING and FAULT. Explain that the backend may process thousands of devices while the browser renders a sampled map view.

## 1:45 — Regional balancing

Press **Auto-Balance**. Explain that NORTH, SOUTH, EAST and WEST are compared by generation versus demand and transfer actions are calculated from surplus zones to deficit zones.

## 2:15 — Kafka

Press **Kafka Pulse 1k**. Open Kafka UI at http://localhost:8090 to show the `grid.telemetry` topic and explain that the consumer feeds telemetry into the same ingestion pipeline.

## 2:45 — Storm

For the full benchmark, set `GRID_NODE_COUNT: 50000` in `devops/docker-compose.yml` and rebuild. Press **Storm 50k**. Explain that each submitted telemetry change is executed through Java virtual-thread tasks and the Spring StateMachine updates the battery lifecycle.

## 3:45 — Benchmark

Press **Benchmark 50k** and show the returned elapsed time and tasks/sec. This isolates the virtual-thread concurrency path from Kafka latency.

## 4:20 — Audit timeline

Open the Audit Timeline. Show state transitions such as IDLE → DISCHARGING or IDLE → CHARGING and the reasons recorded by the backend.

## 4:45 — Observability

Open Grafana at http://localhost:3000 and Prometheus at http://localhost:9090 to demonstrate runtime observability.

## Closing statement

GridWeaver separates high-concurrency ingestion, deterministic state transitions, message buffering, GIS visualization and observability so the system can react to a storm spike without blocking on one platform thread per IoT device.
