# GridWeaver - Windows Quick Start

## No manual file editing required

1. Extract this ZIP completely to a normal folder such as `E:\GridWeaver`.
2. Start Docker Desktop and wait until Docker is running.
3. Double-click `MAKE-EDITABLE.cmd` once. This removes Windows Read-only attributes from the extracted files.
4. Double-click `START-GRIDWEAVER.cmd`.
5. Open http://localhost:8080.

### One-CMD alternative

```cmd
cd /d E:\GridWeaver
docker compose -f devops\docker-compose.yml up --build
```

### Stop

```cmd
cd /d E:\GridWeaver
docker compose -f devops\docker-compose.yml down --remove-orphans
```

## Services

- GridWeaver dashboard: http://localhost:8080
- Kafka UI: http://localhost:8090
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

## Kafka note

This project uses the official Apache Kafka Docker image `apache/kafka:3.9.2` rather than the retired public Bitnami Kafka image.

## 50K demonstration

For the full 50,000-node demo, change `GRID_NODE_COUNT` in `devops/docker-compose.yml` from `5000` to `50000`, then rebuild the stack.

The browser uses a sampled dashboard view so it does not attempt to draw all 50,000 nodes at once.
