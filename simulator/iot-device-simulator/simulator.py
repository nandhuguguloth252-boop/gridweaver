#!/usr/bin/env python3
"""
GridWeaver IoT Device Simulator
Simulates 10 to 50,000+ IoT solar/battery/meter devices streaming real-time telemetry.
Supports both HTTP REST ingestion and WebSocket streaming.
"""

import argparse
import json
import math
import random
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import urllib.request
import urllib.error

ZONES = ["NORTH", "SOUTH", "EAST", "WEST"]

def generate_telemetry(device_id, zone, lat, lon, step):
    wave = math.sin(step * 0.1 + hash(device_id) % 100)
    solar = max(0.0, round(30.0 + wave * 25.0 + random.uniform(-5.0, 5.0), 2))
    load = max(5.0, round(35.0 - wave * 15.0 + random.uniform(-4.0, 6.0), 2))
    battery = max(10.0, min(100.0, round(50.0 + wave * 30.0 + random.uniform(-5.0, 5.0), 1)))
    temp = round(25.0 + (load / 100.0) * 20.0 + random.uniform(-2.0, 3.0), 1)

    # Occasional overheat or fault simulation (0.5% chance)
    if random.random() < 0.005:
        temp = 68.0

    return {
        "deviceId": device_id,
        "type": "BATTERY",
        "zone": zone,
        "latitude": round(lat, 5),
        "longitude": round(lon, 5),
        "power": solar,
        "gridLoad": load,
        "batteryLevel": battery,
        "temperature": temp,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }

def send_batch(url, batch):
    data = json.dumps(batch).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status == 200
    except Exception as e:
        print(f"[Error] Batch send failed: {e}", file=sys.stderr)
        return False

def run_simulation(base_url, num_devices, batch_size, interval_sec, max_iterations):
    telemetry_url = f"{base_url}/api/telemetry/batch"
    print(f"==================================================")
    print(f" GridWeaver IoT Simulator Starting")
    print(f" Target API   : {telemetry_url}")
    print(f" Devices      : {num_devices:,}")
    print(f" Batch Size   : {batch_size}")
    print(f" Interval     : {interval_sec}s")
    print(f" Iterations   : {'Infinite' if max_iterations == 0 else max_iterations}")
    print(f"==================================================")

    # Initialize device metadata
    devices = []
    for i in range(num_devices):
        dev_id = f"BAT-{i+1:05d}"
        zone = ZONES[i % len(ZONES)]
        lat = 26.87 + (i % 100) * 0.0012
        lon = 75.78 + (i // 100) * 0.0010
        devices.append((dev_id, zone, lat, lon))

    iteration = 0
    total_messages = 0
    start_time = time.time()

    try:
        while True:
            iteration += 1
            iter_start = time.time()
            all_telemetry = [
                generate_telemetry(dev_id, zone, lat, lon, iteration)
                for dev_id, zone, lat, lon in devices
            ]

            # Split into batches
            batches = [
                all_telemetry[k:k + batch_size]
                for k in range(0, len(all_telemetry), batch_size)
            ]

            # Send concurrently
            workers = min(32, max(4, len(batches)))
            with ThreadPoolExecutor(max_workers=workers) as executor:
                results = list(executor.map(lambda b: send_batch(telemetry_url, b), batches))

            iter_duration = time.time() - iter_start
            success_count = sum(len(b) for b, ok in zip(batches, results) if ok)
            total_messages += success_count
            rate = round(success_count / max(0.001, iter_duration), 1)

            print(f"[{datetime.now().strftime('%H:%M:%S')}] Iteration {iteration} | Sent: {success_count:,} events | "
                  f"Duration: {iter_duration*1000:.1f}ms | Throughput: {rate:,.0f} evt/sec")

            if max_iterations > 0 and iteration >= max_iterations:
                break

            sleep_time = max(0.0, interval_sec - iter_duration)
            if sleep_time > 0:
                time.sleep(sleep_time)

    except KeyboardInterrupt:
        print("\nSimulation stopped by user.")

    total_time = time.time() - start_time
    print(f"\n==================================================")
    print(f" Simulation Summary")
    print(f" Total Events Sent : {total_messages:,}")
    print(f" Total Duration    : {total_time:.2f}s")
    print(f" Average Rate      : {round(total_messages / max(0.001, total_time)):,} events/second")
    print(f"==================================================")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="GridWeaver IoT Device Simulator")
    parser.add_argument("--url", default="http://localhost:8080", help="Base backend URL")
    parser.add_argument("--devices", type=int, default=1000, help="Number of simulated devices (e.g. 100, 1000, 10000, 50000)")
    parser.add_argument("--batch", type=int, default=250, help="Batch size for HTTP requests")
    parser.add_argument("--interval", type=float, default=2.0, help="Telemetry send interval in seconds")
    parser.add_argument("--iterations", type=int, default=5, help="Number of iterations (0 for infinite)")
    args = parser.parse_args()

    run_simulation(args.url, args.devices, args.batch, args.interval, args.iterations)
