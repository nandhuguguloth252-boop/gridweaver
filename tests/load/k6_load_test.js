import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 Load Test for GridWeaver Virtual Thread Ingestion
export const options = {
  stages: [
    { duration: '10s', target: 100 },   // Warm-up to 100 concurrent VUs
    { duration: '20s', target: 1000 },  // Scale to 1,000 concurrent VUs
    { duration: '30s', target: 5000 },  // Surge to 5,000 VUs
    { duration: '20s', target: 10000 }, // Peak stress at 10,000 VUs
    { duration: '10s', target: 0 },     // Cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests should complete within 200ms
    http_req_failed: ['rate<0.01'],   // Less than 1% failed requests
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export default function () {
  const deviceId = `BAT-${Math.floor(Math.random() * 50000) + 1}`;
  const payload = JSON.stringify({
    deviceId: deviceId,
    solarKw: Math.random() * 60,
    loadKw: 15 + Math.random() * 50,
    chargeKwh: 40 + Math.random() * 40,
    health: 90 + Math.random() * 10,
    temperature: 25 + Math.random() * 15,
    timestamp: new Date().toISOString(),
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(`${BASE_URL}/api/telemetry`, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has node ID': (r) => r.body.includes(deviceId),
  });

  sleep(0.1);
}
