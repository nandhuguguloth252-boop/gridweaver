/**
 * GridWeaver IoT Device Simulator (Node.js)
 * Simulates concurrent IoT devices streaming telemetry via HTTP/WebSocket.
 */

const http = require('http');

const ZONES = ['NORTH', 'SOUTH', 'EAST', 'WEST'];
const args = process.argv.slice(2);

function getArg(key, defaultVal) {
  const idx = args.indexOf(key);
  return idx !== -1 && args[idx + 1] ? args[idx + 1] : defaultVal;
}

const BASE_URL = getArg('--url', 'http://localhost:8080');
const NUM_DEVICES = parseInt(getArg('--devices', '1000'), 10);
const BATCH_SIZE = parseInt(getArg('--batch', '250'), 10);
const INTERVAL_MS = parseInt(getArg('--interval', '2000'), 10);
const MAX_ITERATIONS = parseInt(getArg('--iterations', '5'), 10);

console.log('==================================================');
console.log(' GridWeaver Node.js IoT Simulator');
console.log(` Target       : ${BASE_URL}/api/telemetry/batch`);
console.log(` Devices      : ${NUM_DEVICES.toLocaleString()}`);
console.log(` Batch Size   : ${BATCH_SIZE}`);
console.log(` Interval     : ${INTERVAL_MS}ms`);
console.log(` Iterations   : ${MAX_ITERATIONS === 0 ? 'Infinite' : MAX_ITERATIONS}`);
console.log('==================================================');

function generateTelemetry(deviceId, zone, lat, lon, step) {
  const wave = Math.sin(step * 0.1 + (deviceId.charCodeAt(4) || 0));
  const solar = Math.max(0, parseFloat((30.0 + wave * 25.0 + (Math.random() * 10 - 5)).toFixed(2)));
  const load = Math.max(5, parseFloat((35.0 - wave * 15.0 + (Math.random() * 10 - 4)).toFixed(2)));
  const battery = Math.max(10, Math.min(100, parseFloat((50.0 + wave * 30.0).toFixed(1))));
  const temp = parseFloat((25.0 + (load / 100.0) * 20.0).toFixed(1));

  return {
    deviceId,
    type: 'BATTERY',
    zone,
    latitude: parseFloat(lat.toFixed(5)),
    longitude: parseFloat(lon.toFixed(5)),
    power: solar,
    gridLoad: load,
    batteryLevel: battery,
    temperature: temp,
    timestamp: new Date().toISOString()
  };
}

function sendBatch(urlStr, batch) {
  return new Promise((resolve) => {
    const data = JSON.stringify(batch);
    const url = new URL(urlStr);
    const req = http.request({
      hostname: url.hostname,
      port: url.port,
      path: url.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      }
    }, (res) => {
      resolve(res.statusCode === 200);
    });

    req.on('error', () => resolve(false));
    req.write(data);
    req.end();
  });
}

async function run() {
  const devices = [];
  for (let i = 0; i < NUM_DEVICES; i++) {
    const devId = `BAT-${String(i + 1).padStart(5, '0')}`;
    const zone = ZONES[i % ZONES.length];
    const lat = 26.87 + (i % 100) * 0.0012;
    const lon = 75.78 + Math.floor(i / 100) * 0.0010;
    devices.push({ devId, zone, lat, lon });
  }

  let iteration = 0;
  let totalSent = 0;
  const startAll = Date.now();

  const tick = async () => {
    iteration++;
    const t0 = Date.now();

    const telemetry = devices.map(d => generateTelemetry(d.devId, d.zone, d.lat, d.lon, iteration));
    const batches = [];
    for (let k = 0; k < telemetry.length; k += BATCH_SIZE) {
      batches.push(telemetry.slice(k, k + BATCH_SIZE));
    }

    const results = await Promise.all(batches.map(b => sendBatch(`${BASE_URL}/api/telemetry/batch`, b)));
    const duration = Date.now() - t0;
    const successCount = batches.reduce((acc, b, idx) => acc + (results[idx] ? b.length : 0), 0);
    totalSent += successCount;
    const rate = Math.round(successCount / Math.max(0.001, duration / 1000));

    console.log(`[${new Date().toLocaleTimeString()}] Iteration ${iteration} | Sent: ${successCount.toLocaleString()} events | Duration: ${duration}ms | Rate: ${rate.toLocaleString()} evt/sec`);

    if (MAX_ITERATIONS > 0 && iteration >= MAX_ITERATIONS) {
      const totalTime = ((Date.now() - startAll) / 1000).toFixed(2);
      console.log('\n==================================================');
      console.log(' Node.js Simulator Complete');
      console.log(` Total Events : ${totalSent.toLocaleString()}`);
      console.log(` Duration     : ${totalTime}s`);
      console.log('==================================================');
      process.exit(0);
    } else {
      const wait = Math.max(0, INTERVAL_MS - duration);
      setTimeout(tick, wait);
    }
  };

  tick();
}

run();
