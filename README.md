# GridWeaver — Virtual Thread IoT Microgrid State Engine

GridWeaver is a high-concurrency, real-time IoT microgrid monitoring and autonomous energy balancing system designed with **Java 21 Virtual Threads (Project Loom)**, **Spring Boot 3**, **Spring StateMachine**, **Apache Kafka**, **PostgreSQL**, **Redis**, **React 19**, and **Leaflet GIS maps**.

It is engineered to ingest, process, and state-transition telemetry from tens of thousands (up to 50,000+) distributed energy resources (solar panels, home battery storage systems, smart meters, substations) concurrently with sub-100ms latency.

---

## Architecture Overview

```text
                    ┌─────────────────────────┐
                    │     IoT DEVICES         │
                    │  50,000+ Solar/Battery  │
                    └────────────┬────────────┘
                                 │
                         HTTP / WebSocket
                                 │
                                 ▼
              ┌──────────────────────────────────┐
              │     INGESTION SERVICE             │
              │      Java 21 + Spring Boot        │
              │   Executors.newVirtualThread...   │
              └───────────────┬──────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │      KAFKA      │
                     │  Event Broker   │
                     └────────┬────────┘
                              │
                              ▼
              ┌──────────────────────────────┐
              │    STATE PROCESSOR           │
              │   Spring State Machine       │
              │ IDLE ◄► CHARGING ◄► DISCHARG │
              │          ▲ FAULT ▲           │
              └───────────────┬──────────────┘
                              │
                 ┌────────────┴────────────┐
                 │                         │
                 ▼                         ▼
        ┌────────────────┐       ┌────────────────┐
        │ PostgreSQL /   │       │ Redis /        │
        │ H2 Event Audit │       │ State Registry │
        └────────────────┘       └────────────────┘
                 │                         │
                 └────────────┬────────────┘
                              ▼
                    ┌────────────────────┐
                    │ WebSocket Gateway  │
                    │      /ws/grid      │
                    └──────────┬─────────┘
                               │
                               ▼
              ┌────────────────────────────────┐
              │       REACT DASHBOARD           │
              │   Leaflet GIS Digital Twin     │
              │   Real-time Telemetry & KPIs   │
              │   Regional Balancing Lines     │
              │   Event Audit Timeline Log     │
              └────────────────────────────────┘
```

---

## Key Features

1. **Java 21 Virtual Threads**: Handles massive concurrent IoT message spikes without thread-pool exhaustion or high memory overhead.
2. **Spring State Machine Lifecycle**:
   - `IDLE`: Normal grid load and balanced generation.
   - `CHARGING`: Solar generation surplus (`solar > load * 1.25`).
   - `DISCHARGING`: High grid load / peak demand surge (`load > 42 kW` or `load >= 60 kW`).
   - `FAULT`: Overheating (`temp > 60°C` or `health < 20%`).
   - `RECOVERY`: Returns from `FAULT` to `IDLE` when parameters normalize (`health >= 25%`).
3. **Regional Energy Balancing**: Evaluates generation vs. demand across `NORTH`, `SOUTH`, `EAST`, and `WEST` zones, computing donor-to-receiver power transfers without over-allocating donor capacity.
4. **Real-time GIS Digital Twin**: React + Leaflet interactive dashboard displaying live nodes color-coded by state:
   - 🟢 `IDLE` (Green)
   - 🔵 `CHARGING` (Blue)
   - 🟡 `DISCHARGING` (Amber)
   - 🔴 `FAULT` (Red)
   - Blue dashed power-flow lines representing active inter-zone energy transfers.
5. **High-Throughput IoT Simulator**: Multi-threaded Python and Node.js simulator generating 100 to 50,000+ IoT battery telemetry events.
6. **Multi-tier Testing Suite**:
   - 22 Backend JUnit 5 & Mockito unit/integration tests
   - Frontend Vitest suite
   - Automated End-to-End verification script
   - Load testing scripts with k6 & Python concurrency benchmark

---

## Quick Start Guide

### Option 1: Run Locally (Recommended for Instant Testing)

No Docker required! Uses Java 21, Maven, and Node.js:

1. **Start all services with one double-click**:
   ```cmd
   START-LOCAL.cmd
   ```
   Or run manually in two terminals:
   - **Backend**:
     ```cmd
     cd backend
     mvn spring-boot:run
     ```
     Backend runs at `http://localhost:8080`.
   - **Frontend**:
     ```cmd
     cd frontend
     npm run dev
     ```
     Frontend runs at `http://localhost:5173`.

2. Open the React Dashboard:
   👉 **http://localhost:5173** (or `http://localhost:8080` in Docker)

---

### Option 2: Run with Docker Compose

Launches PostgreSQL, Redis, Kafka, Kafka UI, Spring Boot Backend, Nginx + React Frontend, Prometheus, and Grafana:

```cmd
START-DOCKER.cmd
```
or:
```cmd
docker compose -f devops\docker-compose.yml up --build
```

**Services in Docker:**
- GridWeaver Dashboard: **http://localhost:8080**
- Kafka UI: **http://localhost:8090**
- Prometheus: **http://localhost:9090**
- Grafana: **http://localhost:3000** (credentials: `admin` / `admin`)

To stop Docker:
```cmd
docker compose -f devops\docker-compose.yml down
```

---

## Running the Test Phase (Direct Testing)

All testing tools are pre-configured and automated:

### 1. Run Complete Automated Test Suite (One-Click)
```cmd
RUN-TESTS.cmd
```
This runs:
- Backend JUnit 5 unit & integration tests (22 tests)
- Frontend Vitest suite
- Live End-to-End automated test suite against the running backend

---

### 2. Run Backend Unit & Integration Tests (JUnit 5 + MockMvc)
```cmd
cd backend
mvn test
```
Tests:
- `BatteryStateMachineTest`: Tests all 8 state transitions (IDLE, CHARGING, DISCHARGING, FAULT, RECOVERY).
- `GridBalanceServiceTest`: Tests regional power calculation, donor capacity limits, and energy conservation.
- `TelemetryIngestionServiceTest`: Tests 1,000 concurrent tasks on Java 21 virtual threads, event persistence, and WebSocket broadcast.
- `GridControllerIntegrationTest`: Full Spring Boot integration test covering `/api/grid/summary`, `/api/grid/nodes`, `/api/grid/balance`, `/api/devices`, `/api/telemetry`, and `/api/simulation/*`.

---

### 3. Run Live End-to-End Verification Test Suite
Start the backend first, then run:
```cmd
python tests\e2e_test.py http://localhost:8080
```
**Verification Output:**
```text
==================================================================
         GRIDWEAVER AUTOMATED END-TO-END VERIFICATION SUITE       
==================================================================
Target URL: http://localhost:8080

Phase 1: Grid State & Summary
  [PASS] GET /api/grid/summary status is 200
  [PASS] Grid has registered nodes (5000 nodes)
  [PASS] Grid resilience calculated (75.2%)
  [PASS] State distribution provided

Phase 2: Digital Twin Query
  [PASS] GET /api/grid/nodes status is 200
  [PASS] Returns requested sample limit of 10 nodes
  [PASS] Node contains valid identifier

Phase 3: IoT Device Registration
  [PASS] POST /api/devices registers new device successfully
  [PASS] Registered device ID matches
  [PASS] GET /api/devices/BAT-E2E-TEST retrieves device

Phase 4: Battery State Machine Rule Engine
  [PASS] Scenario 1: Normal load & generation -> IDLE state
  [PASS] Scenario 2: Solar Generation > Demand -> CHARGING state
  [PASS] Scenario 3: High Grid Load / Surge -> DISCHARGING state
  [PASS] Scenario 4: High Temperature (68°C) -> FAULT state
  [PASS] Scenario 5: FAULT + Recovery telemetry -> IDLE state

Phase 5: Distributed Regional Balancing
  [PASS] POST /api/grid/balance calculates zone transfers

Phase 6: Audit Event Timeline
  [PASS] GET /api/events returns event timeline
  [PASS] Recorded state-change events in audit trail

Phase 7: Java 21 Virtual Threads Benchmark
  [PASS] Virtual thread benchmark completed 1000 tasks at 13,889 tasks/sec

Phase 8: Grid Storm Simulation
  [PASS] Storm simulation completed 500 nodes at 38,462 nodes/sec

==================================================================
 TEST RUN COMPLETE: 20 PASSED | 0 FAILED
==================================================================
```

---

### 4. Run Concurrency Load Benchmark (100 to 10,000+ Requests)
```cmd
RUN-BENCHMARK.cmd
```
or:
```cmd
python tests\load\benchmark_load.py --url http://localhost:8080
```

**Measured Performance Table:**
| Requests | Throughput (req/s) | Avg Latency | P95 Latency | Success Rate |
| --------:| ------------------:| -----------:| -----------:| ------------:|
|      100 |         1,396 req/s|      10.0ms |      17.1ms |       100.0% |
|    1,000 |         1,936 req/s|      20.0ms |      31.9ms |       100.0% |
|    5,000 |         2,132 req/s|      38.0ms |      62.5ms |       100.0% |
|   10,000 |         2,135 req/s|      56.5ms |      95.6ms |       100.0% |

---

### 5. Run the IoT Device Simulator (1,000 to 50,000 Devices)
```cmd
RUN-SIMULATOR.cmd
```
or:
```cmd
python simulator\iot-device-simulator\simulator.py --devices 1000 --batch 200 --iterations 5
```
Or via Node.js:
```cmd
node simulator\iot-device-simulator\simulator.js --devices 1000 --batch 200 --iterations 5
```

---

### 6. Load Testing with k6
If k6 is installed:
```cmd
k6 run tests\load\k6_load_test.js
```

---

## Main API Endpoints Reference

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `GET` | `/api/grid/summary` | Real-time aggregate grid metrics, resilience, zone totals, active transfers |
| `GET` | `/api/grid/nodes?limit=800` | Sampled active digital twin nodes for GIS map rendering |
| `GET` | `/api/devices` | Query registered IoT battery devices |
| `GET` | `/api/devices/{id}` | Query current state and telemetry of a specific device |
| `POST` | `/api/devices` | Register a new device with initial capacity and coordinates |
| `POST` | `/api/telemetry` | Ingest single device telemetry via Virtual Thread executor |
| `POST` | `/api/telemetry/batch` | Ingest batch of IoT telemetry messages concurrently |
| `POST` | `/api/grid/balance` | Trigger regional energy surplus/deficit balancing algorithm |
| `GET` | `/api/events?limit=100` | Retrieve state-change audit timeline from database |
| `POST` | `/api/simulation/benchmark?nodes=50000` | In-memory 50k virtual thread ingestion throughput test |
| `POST` | `/api/simulation/storm?nodes=50000` | Trigger storm event (reduced solar, surge load) |
| `POST` | `/api/simulation/kafka-pulse?nodes=1000` | Publish telemetry burst through Kafka broker |
| `WS` | `/ws/grid` | Bi-directional WebSocket stream for live UI updates and IoT frames |

---

## License & Credits

Developed with Java 21, Spring Boot 3, Spring StateMachine, Apache Kafka, React, and Leaflet. Built as a high-performance demonstration of Project Loom Virtual Threads for large-scale IoT systems.
