const http = require('http');

const BASE_URL = 'http://localhost:8080';
const TOTAL_REQUESTS = 1000;
const REQUESTS_PER_SECOND = 1000;

let successCount = 0;
let rateLimitedCount = 0;
let errorCount = 0;
let results = {
  200: 0,
  429: 0,
  other: 0
};

// Helper function to make HTTP requests
function makeRequest(requestNum) {
  return new Promise((resolve) => {
    const url = new URL(BASE_URL + '/api/hello');
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname,
      method: 'GET',
      headers: {
        'User-Agent': 'Rate-Limiter-Bomber',
        'X-Request-ID': requestNum.toString()
      }
    };

    const req = http.request(options, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        if (res.statusCode === 200) {
          successCount++;
          results[200]++;
        } else if (res.statusCode === 429) {
          rateLimitedCount++;
          results[429]++;
        } else {
          errorCount++;
          results.other++;
        }

        resolve({
          requestNum,
          status: res.statusCode,
          remaining: res.headers['x-ratelimit-remaining'] || 'N/A'
        });
      });
    });

    req.on('error', () => {
      errorCount++;
      results.other++;
      resolve({
        requestNum,
        status: 'ERROR',
        remaining: 'N/A'
      });
    });

    req.end();
  });
}

async function bombardServer() {
  console.log('🔫 RATE LIMITER BOMBARD TEST 💣');
  console.log(`Target: ${BASE_URL}/api/hello`);
  console.log(`Total Requests: ${TOTAL_REQUESTS}`);
  console.log(`Rate: ${REQUESTS_PER_SECOND} req/sec\n`);
  console.log('⏱️  Starting bombardment...\n');

  const startTime = Date.now();
  const requests = [];

  // Create all requests at once (simulating 1000 req/sec burst)
  for (let i = 1; i <= TOTAL_REQUESTS; i++) {
    requests.push(makeRequest(i));
  }

  // Wait for all requests to complete
  const responses = await Promise.all(requests);

  const endTime = Date.now();
  const duration = (endTime - startTime) / 1000;

  // Sort and show first few results
  console.log('📊 First 20 Requests:');
  console.log('─'.repeat(60));
  responses.slice(0, 20).forEach(r => {
    const status = r.status === 200 ? '✅' : r.status === 429 ? '❌' : '⚠️ ';
    console.log(`${status} Req #${String(r.requestNum).padStart(4)}: ${r.status} - Tokens Remaining: ${r.remaining}`);
  });

  console.log('\n📊 Last 20 Requests:');
  console.log('─'.repeat(60));
  responses.slice(-20).forEach(r => {
    const status = r.status === 200 ? '✅' : r.status === 429 ? '❌' : '⚠️ ';
    console.log(`${status} Req #${String(r.requestNum).padStart(4)}: ${r.status} - Tokens Remaining: ${r.remaining}`);
  });

  // Calculate statistics
  console.log('\n' + '═'.repeat(60));
  console.log('📈 BOMBARDMENT RESULTS');
  console.log('═'.repeat(60));
  console.log(`Total Requests Sent: ${TOTAL_REQUESTS}`);
  console.log(`Duration: ${duration.toFixed(2)}s`);
  console.log(`Actual Rate: ${(TOTAL_REQUESTS / duration).toFixed(0)} req/sec\n`);

  console.log('Response Breakdown:');
  console.log(`  ✅ 200 OK (Success):         ${results[200].toString().padStart(4)} (${((results[200] / TOTAL_REQUESTS) * 100).toFixed(2)}%)`);
  console.log(`  ❌ 429 Too Many Requests:    ${results[429].toString().padStart(4)} (${((results[429] / TOTAL_REQUESTS) * 100).toFixed(2)}%)`);
  console.log(`  ⚠️  Other/Errors:            ${results.other.toString().padStart(4)} (${((results.other / TOTAL_REQUESTS) * 100).toFixed(2)}%)`);

  console.log('\n' + '═'.repeat(60));
  console.log('🎯 RATE LIMITER ANALYSIS');
  console.log('═'.repeat(60));

  const blockingPercentage = ((results[429] / TOTAL_REQUESTS) * 100).toFixed(2);
  const allowingPercentage = ((results[200] / TOTAL_REQUESTS) * 100).toFixed(2);

  console.log(`Rate Limiter Blocked: ${blockingPercentage}% of requests (${results[429]} requests)`);
  console.log(`Rate Limiter Allowed: ${allowingPercentage}% of requests (${results[200]} requests)`);

  if (results[429] > 0) {
    console.log('\n✅ RATE LIMITER IS WORKING CORRECTLY!');
    console.log(`   Successfully blocked ${results[429]} requests out of ${TOTAL_REQUESTS}`);
    console.log('   Your rate limiter is protecting the server from overload! 🛡️');
  } else {
    console.log('\n⚠️  RATE LIMITER ALLOWED ALL REQUESTS');
    console.log('   Tokens may be recovering faster than consumption');
  }

  console.log('\n' + '═'.repeat(60));
}

// Run bombardment
bombardServer().catch(error => {
  console.error('Error during bombardment:', error.message);
  process.exit(1);
});
