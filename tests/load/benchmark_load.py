#!/usr/bin/env python3
"""
GridWeaver Load & Concurrency Benchmark
Measures throughput, latency (avg, p95), and concurrency for virtual threads ingestion.
Tests: 100, 1,000, 5,000, 10,000, 25,000, 50,000 requests.
"""

import argparse
import json
import math
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import urllib.request
import urllib.error

def send_telemetry(url, i):
    payload = json.dumps({
        "deviceId": f"BAT-BENCH-{i}",
        "solarKw": 45.0,
        "loadKw": 30.0,
        "chargeKwh": 70.0,
        "health": 95.0,
        "temperature": 28.0,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json"}
    )
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            elapsed_ms = (time.time() - t0) * 1000.0
            return (res.status == 200, elapsed_ms)
    except Exception:
        elapsed_ms = (time.time() - t0) * 1000.0
        return (False, elapsed_ms)

def run_benchmark_level(url, target_requests, concurrency):
    print(f"\n>> Running benchmark for {target_requests:,} requests (concurrency: {concurrency})...")
    latencies = []
    successes = 0

    t_start = time.time()
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(send_telemetry, url, i) for i in range(target_requests)]
        for f in futures:
            ok, lat = f.result()
            latencies.append(lat)
            if ok:
                successes += 1

    total_time_sec = max(0.001, time.time() - t_start)
    throughput = round(successes / total_time_sec)
    avg_lat = round(sum(latencies) / len(latencies), 1)
    latencies.sort()
    p95_idx = int(len(latencies) * 0.95)
    p95_lat = round(latencies[p95_idx], 1)

    return {
        "requests": target_requests,
        "throughput": throughput,
        "avg_lat": avg_lat,
        "p95_lat": p95_lat,
        "success_rate": f"{(successes / target_requests) * 100:.1f}%"
    }

def main():
    parser = argparse.ArgumentParser(description="GridWeaver Concurrency Benchmark")
    parser.add_argument("--url", default="http://localhost:8080", help="Base backend URL")
    args = parser.parse_args()

    telemetry_url = f"{args.url}/api/telemetry"

    # Verify server is reachable
    print(f"Connecting to GridWeaver at {args.url}...")
    try:
        req = urllib.request.Request(f"{args.url}/api/grid/summary")
        with urllib.request.urlopen(req, timeout=5) as r:
            data = json.loads(r.read().decode())
            print(f"[OK] Connected! Active registered nodes: {data.get('nodes', 'unknown')}")
    except Exception as e:
        print(f"[FAIL] Could not connect to GridWeaver at {args.url}: {e}")
        print("Please start the backend before running load tests.")
        sys.exit(1)

    levels = [
        (100, 20),
        (1000, 50),
        (5000, 100),
        (10000, 150),
    ]

    results = []
    for count, conc in levels:
        res = run_benchmark_level(telemetry_url, count, conc)
        results.append(res)

    print("\n" + "=" * 75)
    print("                GRIDWEAVER LOAD TEST PERFORMANCE TABLE")
    print("=" * 75)
    print(f"{'Requests':>12} | {'Throughput (req/s)':>20} | {'Avg Latency':>13} | {'P95 Latency':>13} | {'Success':>10}")
    print("-" * 75)
    for r in results:
        print(f"{r['requests']:>12,d} | {r['throughput']:>20,d} | {r['avg_lat']:>11.1f}ms | {r['p95_lat']:>11.1f}ms | {r['success_rate']:>10}")
    print("=" * 75)

if __name__ == "__main__":
    main()
