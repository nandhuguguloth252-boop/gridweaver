# GridWeaver Build Status

## Fixed in this rebuild

- Added Maven Wrapper for Windows/Linux backend startup.
- Fixed the `GridSummaryService` generic type compilation issue from the earlier build.
- Removed frontend `latest` package versions and pinned dependency versions.
- Updated frontend requirement to Node 22.12+.
- Reworked the frontend WebSocket handler so node updates are consumed instead of discarded.
- Added frontend connection/error/loading states.
- Fixed the state-machine event ordering so demand surge is reachable.
- Added deterministic fault recovery logic.
- Removed the 3,000-node sampling cap from simulation paths so a configured 50,000-node registry can actually process 50,000 tasks.
- Improved regional balancing so donor capacity cannot be over-allocated across multiple receivers.
- Added a Kafka routing gateway for the regular telemetry simulator.
- Added a Kafka Pulse demonstration endpoint and dashboard button.
- Protected concurrent WebSocket sends with `ConcurrentWebSocketSessionDecorator` and limited browser streaming to the dashboard sample. Spring documents this decorator as the mechanism to avoid concurrent sends on a WebSocket session.
- Added Nginx reverse proxy configuration for REST and WebSocket traffic.
- Changed Docker Compose to expose one main application URL at `localhost:8080` while keeping backend/Kafka internal.
- Added Docker startup, stop, and prerequisite-check scripts.
- Added power-flow lines for calculated regional transfers on the GIS map.
- Added a 50K demonstration mode and documentation.

## Verification performed in this environment

- `package.json` parses as valid JSON.
- `pom.xml` parses as valid XML.
- Docker Compose/Prometheus/Grafana YAML files parse successfully.
- Known previously reported broken patterns were scanned out of the rebuilt files.

## Environment limitation

The build environment used to assemble this archive does not have Docker installed and does not have external DNS/network access to Maven Central or the npm registry. Therefore the full Maven dependency build, npm dependency installation, Docker image build, and end-to-end runtime test could not be executed here. The archive is structured for a clean Windows Docker Desktop environment and includes deterministic startup instructions, but the first local build still needs to download Maven/Node/Docker dependencies.
