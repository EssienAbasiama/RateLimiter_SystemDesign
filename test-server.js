const http = require('http');

const BASE_URL = 'http://localhost:8080';

// Helper function to make HTTP requests
function makeRequest(path, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE_URL + path);
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method: method,
      headers: {
        'Content-Type': 'application/json'
      }
    };

    const req = http.request(options, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        resolve({
          status: res.statusCode,
          statusText: res.statusMessage,
          headers: res.headers,
          body: data
        });
      });
    });

    req.on('error', reject);

    if (body) {
      req.write(JSON.stringify(body));
    }

    req.end();
  });
}

async function testHealthEndpoint() {
  console.log('\n====== TEST 1: Health Check ======');
  try {
    const response = await makeRequest('/api/health');
    console.log(`Status: ${response.status}`);
    console.log(`Response: ${response.body}`);
  } catch (error) {
    console.error('Error:', error.message);
  }
}

async function testRateLimiting() {
  console.log('\n====== TEST 2: Rate Limiting (15 Requests with 500ms delay) ======');
  console.log(`Capacity: 10 tokens, Refill Rate: 2.0/sec`);
  console.log('Expected: Requests 1-10 succeed, Request 11+ get 429 (Too Many Requests)\n');

  for (let i = 1; i <= 15; i++) {
    try {
      const response = await makeRequest('/api/hello');
      const remaining = response.headers['x-ratelimit-remaining'] || 'N/A';
      const capacity = response.headers['x-ratelimit-capacity'] || 'N/A';
      
      if (response.status === 200) {
        console.log(`✅ Request ${i}: SUCCESS - Tokens Remaining: ${remaining}/${capacity}`);
      } else if (response.status === 429) {
        console.log(`❌ Request ${i}: RATE LIMITED (429) - Tokens Remaining: ${remaining}/${capacity}`);
      } else {
        console.log(`⚠️  Request ${i}: Status ${response.status}`);
      }
    } catch (error) {
      console.error(`❌ Request ${i}: ERROR -`, error.message);
    }

    // Wait 500ms between requests
    await new Promise(resolve => setTimeout(resolve, 500));
  }
}

async function testDifferentEndpoints() {
  console.log('\n====== TEST 3: Different Endpoints ======');

  // Test /api/hello
  console.log('\n1. Testing /api/hello (GET):');
  try {
    const response = await makeRequest('/api/hello');
    console.log(`   Status: ${response.status}`);
    console.log(`   Response: ${response.body}`);
  } catch (error) {
    console.error('   Error:', error.message);
  }

  // Test /api/data
  console.log('\n2. Testing /api/data (GET):');
  try {
    const response = await makeRequest('/api/data');
    console.log(`   Status: ${response.status}`);
    console.log(`   Response: ${response.body}`);
  } catch (error) {
    console.error('   Error:', error.message);
  }

  // Test /api/echo (POST)
  console.log('\n3. Testing /api/echo (POST):');
  try {
    const response = await makeRequest('/api/echo', 'POST', { message: 'Hello from Node.js!' });
    console.log(`   Status: ${response.status}`);
    console.log(`   Response: ${response.body}`);
  } catch (error) {
    console.error('   Error:', error.message);
  }
}

async function testWithDelay() {
  console.log('\n====== TEST 4: Rate Limit Recovery (10 requests with 1 second delay) ======');
  console.log('With 2.0 tokens/sec refill rate, tokens should recover after ~0.5 sec per token\n');

  for (let i = 1; i <= 10; i++) {
    try {
      const response = await makeRequest('/api/hello');
      const remaining = response.headers['x-ratelimit-remaining'] || 'N/A';
      const reset = response.headers['x-ratelimit-reset'] || 'N/A';

      if (response.status === 200) {
        console.log(`✅ Request ${i}: SUCCESS - Tokens Remaining: ${remaining}`);
      } else if (response.status === 429) {
        console.log(`❌ Request ${i}: RATE LIMITED (429) - Reset: ${reset}s`);
      }
    } catch (error) {
      console.error(`❌ Request ${i}: ERROR -`, error.message);
    }

    // Wait 1 second between requests (allows token recovery)
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
}

async function runAllTests() {
  console.log('🚀 Starting Rate Limiter Server Tests...\n');
  console.log(`Testing server at: ${BASE_URL}\n`);

  try {
    await testHealthEndpoint();
    await testRateLimiting();
    await testDifferentEndpoints();
    await testWithDelay();

    console.log('\n✅ All tests completed!');
  } catch (error) {
    console.error('\n❌ Test suite failed:', error.message);
    process.exit(1);
  }
}

// Run tests
runAllTests();
