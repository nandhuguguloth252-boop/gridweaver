#!/usr/bin/env python3
"""
GridWeaver End-to-End Automated Verification Test Suite
Tests every layer: REST, State Transitions, Balancing, Audit Logging, and Concurrency.
"""

import json
import sys
import time
import urllib.request
import urllib.error

class TestRunner:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.passed = 0
        self.failed = 0

    def assert_true(self, condition, test_name, detail=""):
        if condition:
            print(f"  [PASS] {test_name}")
            self.passed += 1
        else:
            print(f"  [FAIL] {test_name} - {detail}")
            self.failed += 1

    def request(self, method, path, data=None):
        url = f"{self.base_url}{path}"
        body = json.dumps(data).encode("utf-8") if data else None
        req = urllib.request.Request(
            url,
            data=body,
            headers={"Content-Type": "application/json"} if body else {}
        )
        req.get_method = lambda: method
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                res_body = response.read().decode("utf-8")
                return response.status, json.loads(res_body) if res_body else {}
        except urllib.error.HTTPError as e:
            res_body = e.read().decode("utf-8")
            return e.code, json.loads(res_body) if res_body else {}
        except Exception as e:
            return 0, str(e)

    def run_all(self):
        print("==================================================================")
        print("         GRIDWEAVER AUTOMATED END-TO-END VERIFICATION SUITE       ")
        print("==================================================================")
        print(f"Target URL: {self.base_url}\n")

        # 1. Health & Grid Summary
        print("Phase 1: Grid State & Summary")
        status, data = self.request("GET", "/api/grid/summary")
        self.assert_true(status == 200, "GET /api/grid/summary status is 200")
        self.assert_true("nodes" in data and data["nodes"] >= 100, f"Grid has registered nodes ({data.get('nodes')} nodes)")
        self.assert_true("resilience" in data, f"Grid resilience calculated ({data.get('resilience')}%)")
        self.assert_true("states" in data, "State distribution provided")

        # 2. Nodes Query
        print("\nPhase 2: Digital Twin Query")
        status, nodes = self.request("GET", "/api/grid/nodes?limit=10")
        self.assert_true(status == 200, "GET /api/grid/nodes status is 200")
        self.assert_true(len(nodes) == 10, "Returns requested sample limit of 10 nodes")
        first_node = nodes[0]
        self.assert_true("id" in first_node and first_node["id"].startswith("GW-"), "Node contains valid identifier")

        # 3. Device Registration
        print("\nPhase 3: IoT Device Registration")
        test_device = "BAT-E2E-TEST"
        reg_payload = {
            "deviceId": test_device,
            "zone": "NORTH",
            "latitude": 26.9124,
            "longitude": 75.7873,
            "capacityKwh": 100.0,
            "batteryLevel": 80.0
        }
        status, reg_res = self.request("POST", "/api/devices", reg_payload)
        self.assert_true(status == 200, "POST /api/devices registers new device successfully")
        self.assert_true(reg_res.get("id") == test_device, "Registered device ID matches")

        status, dev_res = self.request("GET", f"/api/devices/{test_device}")
        self.assert_true(status == 200, f"GET /api/devices/{test_device} retrieves device")

        # 4. State Machine Transitions Verification
        print("\nPhase 4: Battery State Machine Rule Engine")

        # Test A: Normal load -> IDLE
        t_idle = {
            "nodeId": test_device,
            "solarKw": 20.0,
            "loadKw": 20.0,
            "chargeKwh": 70.0,
            "health": 95.0,
            "temperature": 25.0
        }
        status, res_idle = self.request("POST", "/api/telemetry", t_idle)
        self.assert_true(status == 200 and res_idle.get("state") == "IDLE",
                         "Scenario 1: Normal load & generation -> IDLE state")

        # Test B: Solar Surplus -> CHARGING
        t_charging = {
            "nodeId": test_device,
            "solarKw": 60.0,
            "loadKw": 20.0,
            "chargeKwh": 75.0,
            "health": 95.0,
            "temperature": 28.0
        }
        status, res_charging = self.request("POST", "/api/telemetry", t_charging)
        self.assert_true(status == 200 and res_charging.get("state") == "CHARGING",
                         "Scenario 2: Solar Generation > Demand -> CHARGING state")

        # Test C: Demand Surge (load = 75) -> DISCHARGING
        t_discharging = {
            "nodeId": test_device,
            "solarKw": 10.0,
            "loadKw": 75.0,
            "chargeKwh": 70.0,
            "health": 95.0,
            "temperature": 32.0
        }
        status, res_discharging = self.request("POST", "/api/telemetry", t_discharging)
        self.assert_true(status == 200 and res_discharging.get("state") == "DISCHARGING",
                         "Scenario 3: High Grid Load / Surge -> DISCHARGING state")

        # Test D: Overheat (temp = 68°C) -> FAULT
        t_fault = {
            "nodeId": test_device,
            "solarKw": 10.0,
            "loadKw": 20.0,
            "chargeKwh": 70.0,
            "temperature": 68.0
        }
        status, res_fault = self.request("POST", "/api/telemetry", t_fault)
        self.assert_true(status == 200 and res_fault.get("state") == "FAULT",
                         "Scenario 4: High Temperature (68°C) -> FAULT state")

        # Test E: Recovery (temp = 25°C, health = 95) -> IDLE
        t_recover = {
            "nodeId": test_device,
            "solarKw": 20.0,
            "loadKw": 20.0,
            "chargeKwh": 70.0,
            "health": 95.0,
            "temperature": 25.0
        }
        status, res_recover = self.request("POST", "/api/telemetry", t_recover)
        self.assert_true(status == 200 and res_recover.get("state") == "IDLE",
                         "Scenario 5: FAULT + Recovery telemetry -> IDLE state")

        # 5. Regional Balancing
        print("\nPhase 5: Distributed Regional Balancing")
        status, balance_res = self.request("POST", "/api/grid/balance")
        self.assert_true(status == 200, "POST /api/grid/balance calculates zone transfers")
        print(f"  [INFO] Calculated regional transfers: {len(balance_res)}")

        # 6. Audit Trail
        print("\nPhase 6: Audit Event Timeline")
        status, events = self.request("GET", "/api/events?limit=20")
        self.assert_true(status == 200, "GET /api/events returns event timeline")
        self.assert_true(len(events) > 0, f"Recorded {len(events)} state-change events in audit trail")

        # 7. Virtual Threads Concurrency Benchmark
        print("\nPhase 7: Java 21 Virtual Threads Benchmark")
        status, bench = self.request("POST", "/api/simulation/benchmark?nodes=1000")
        self.assert_true(status == 200 and bench.get("virtualThreads") is True,
                         f"Virtual thread benchmark completed {bench.get('tasks')} tasks at {bench.get('tasksPerSecond'):,.0f} tasks/sec")

        # 8. Storm Simulation
        print("\nPhase 8: Grid Storm Simulation")
        status, storm = self.request("POST", "/api/simulation/storm?nodes=500")
        self.assert_true(status == 200 and storm.get("mode") == "STORM",
                         f"Storm simulation completed {storm.get('processedNodes')} nodes at {storm.get('nodesPerSecond'):,.0f} nodes/sec")

        print("\n==================================================================")
        print(f" TEST RUN COMPLETE: {self.passed} PASSED | {self.failed} FAILED")
        print("==================================================================")
        return self.failed == 0

if __name__ == "__main__":
    url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    runner = TestRunner(url)
    success = runner.run_all()
    sys.exit(0 if success else 1)
