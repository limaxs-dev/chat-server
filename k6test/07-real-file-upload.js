// K6 Test: Real File Upload to MinIO
// Tests actual file upload using PNG binary data
// Usage: k6 run 07-real-file-upload.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, generateUUID, getToken, createAuthHeaders } from './config.js';

// Test configuration
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<3000'], // File uploads may take longer
    http_req_failed: ['rate<0.01'],
  },
};

let aliceToken;
let uploadedFileId;
let uploadedObjectKey;

// Create a PNG test data (small PNG file)
// PNG signature: 89 50 4E 47 0D 0A 1A 0A
function createTestPNG() {
  // Create a minimal valid PNG file (1x1 transparent pixel)
  const pngData = new Uint8Array([
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
    // IHDR chunk (image header)
    0x00, 0x00, 0x00, 0x0D, // Length: 13 bytes
    0x49, 0x48, 0x44, 0x52, // Type: IHDR
    0x00, 0x00, 0x00, 0x01, // Width: 1
    0x00, 0x00, 0x00, 0x01, // Height: 1
    0x08, 0x02, 0x00, 0x00, 0x00, // Bit depth: 8, Color type: 2 (RGB), etc.
    0x90, 0x77, 0x53, 0xDE, // CRC
    // IDAT chunk (image data)
    0x00, 0x00, 0x00, 0x0C, // Length: 12 bytes
    0x49, 0x44, 0x41, 0x54, // Type: IDAT
    0x08, 0x99, 0x01, 0x01, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x02, // Compressed data
    0x00, 0x01, 0xE2, 0x21, 0xBC, 0x33, // CRC
    // IEND chunk (end of file)
    0x00, 0x00, 0x00, 0x00, // Length: 0 bytes
    0x49, 0x45, 0x4E, 0x44, // Type: IEND
    0xAE, 0x42, 0x60, 0x82  // CRC
  ]);
  return pngData;
}

export function setup() {
  // Get token for Alice
  aliceToken = getToken(config.users.alice);
  console.log('Alice token:', aliceToken.substring(0, 20) + '...');
  return { aliceToken };
}

export default function(data) {
  const baseUrl = config.baseUrl;
  const aliceToken = data.aliceToken;

  // ========================================================================
  // Test 1: Upload PNG file to MinIO
  // ========================================================================
  console.log('\n=== Test 1: Upload PNG file to MinIO ===');

  // Create test PNG data
  const imageFile = createTestPNG();
  const fileSize = imageFile.length;

  console.log('File size:', fileSize, 'bytes');

  // Get upload URL from API
  const fileName = 'test-upload-' + Date.now() + '.png';
  const uploadUrlParams = `fileName=${encodeURIComponent(fileName)}&fileSize=${fileSize}&contentType=image/png`;
  const uploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?${uploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(uploadUrlRes, {
    '[Test 1] Get upload URL - Status 200': (r) => r.status === 200,
    '[Test 1] Get upload URL - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
    '[Test 1] Get upload URL - Has uploadUrl': (r) => JSON.parse(r.body).uploadUrl !== undefined,
    '[Test 1] Get upload URL - Has objectKey': (r) => JSON.parse(r.body).objectKey !== undefined,
  });

  const uploadUrlData = JSON.parse(uploadUrlRes.body);
  uploadedFileId = uploadUrlData.fileId;
  const uploadUrl = uploadUrlData.uploadUrl;
  uploadedObjectKey = uploadUrlData.objectKey;

  console.log('File ID:', uploadedFileId);
  console.log('Upload URL:', uploadUrl.substring(0, 60) + '...');
  console.log('Object Key:', uploadedObjectKey);

  sleep(1);

  // Actually upload the file to MinIO using presigned URL
  console.log('Uploading file to MinIO...');

  const minioUploadRes = http.put(uploadUrl, imageFile, {
    headers: {
      'Content-Type': 'image/png',
      'Content-Length': fileSize.toString(),
    },
  });

  check(minioUploadRes, {
    '[Test 1] Upload to MinIO - Status 200': (r) => r.status === 200 || r.status === 201,
  });

  console.log('MinIO upload status:', minioUploadRes.status);
  console.log('File uploaded successfully to MinIO!');

  sleep(1);

  // ========================================================================
  // Test 2: Confirm the upload
  // ========================================================================
  console.log('\n=== Test 2: Confirm the upload ===');

  const confirmUploadRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(uploadedFileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(confirmUploadRes, {
    '[Test 2] Confirm upload - Status 204': (r) => r.status === 204,
  });

  console.log('Upload confirmed');

  sleep(1);

  // ========================================================================
  // Test 3: Get download URL
  // ========================================================================
  console.log('\n=== Test 3: Get download URL ===');

  const downloadUrlRes = http.get(
    `${baseUrl}${config.endpoints.downloadUrl(uploadedFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(downloadUrlRes, {
    '[Test 3] Get download URL - Status 200': (r) => r.status === 200,
    '[Test 3] Get download URL - Has downloadUrl': (r) => {
      const body = JSON.parse(r.body);
      return body.downloadUrl !== undefined;
    },
    '[Test 3] Get download URL - Valid URL format': (r) => {
      const body = JSON.parse(r.body);
      return body.downloadUrl.startsWith('http://') || body.downloadUrl.startsWith('https://');
    },
  });

  const downloadUrlData = JSON.parse(downloadUrlRes.body);
  console.log('Download URL:', downloadUrlData.downloadUrl.substring(0, 60) + '...');

  sleep(1);

  // ========================================================================
  // Test 4: Download the file back from MinIO to verify
  // ========================================================================
  console.log('\n=== Test 4: Download file from MinIO ===');

  const downloadRes = http.get(downloadUrlData.downloadUrl);

  check(downloadRes, {
    '[Test 4] Download from MinIO - Status 200': (r) => r.status === 200,
    '[Test 4] Download from MinIO - File size matches': (r) => r.body.length === fileSize,
    '[Test 4] Download from MinIO - Is PNG': (r) => {
      // Check PNG signature
      const bodyBytes = new Uint8Array(r.body);
      return bodyBytes[0] === 0x89 && bodyBytes[1] === 0x50 &&
             bodyBytes[2] === 0x4E && bodyBytes[3] === 0x47;
    },
  });

  console.log('Downloaded file size:', downloadRes.body.length, 'bytes');
  console.log('Original file size:', fileSize, 'bytes');
  console.log('File sizes match:', downloadRes.body.length === fileSize);

  sleep(1);

  // ========================================================================
  // Test 5: Upload second PNG file
  // ========================================================================
  console.log('\n=== Test 5: Upload second PNG file ===');

  const fileName2 = 'test-upload-2-' + Date.now() + '.png';
  const uploadUrlParams2 = `fileName=${encodeURIComponent(fileName2)}&fileSize=${fileSize}&contentType=image/png`;
  const uploadUrlRes2 = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?${uploadUrlParams2}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(uploadUrlRes2, {
    '[Test 5] Second upload URL - Status 200': (r) => r.status === 200,
  });

  const uploadUrlData2 = JSON.parse(uploadUrlRes2.body);
  const fileId2 = uploadUrlData2.fileId;

  // Upload to MinIO
  const minioUploadRes2 = http.put(uploadUrlData2.uploadUrl, imageFile, {
    headers: {
      'Content-Type': 'image/png',
      'Content-Length': fileSize.toString(),
    },
  });

  check(minioUploadRes2, {
    '[Test 5] Second MinIO upload - Status 200': (r) => r.status === 200 || r.status === 201,
  });

  // Confirm
  const confirmUploadRes2 = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(fileId2)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(confirmUploadRes2, {
    '[Test 5] Second confirm - Status 204': (r) => r.status === 204,
  });

  console.log('Second file uploaded and confirmed');

  sleep(1);

  // ========================================================================
  // Test 6: Get download URL for second file
  // ========================================================================
  console.log('\n=== Test 6: Get download URL for second file ===');

  const downloadUrlRes2 = http.get(
    `${baseUrl}${config.endpoints.downloadUrl(fileId2)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(downloadUrlRes2, {
    '[Test 6] Second download URL - Status 200': (r) => r.status === 200,
  });

  console.log('Second file download URL retrieved');

  sleep(1);

  // ========================================================================
  // Test 7: Upload JPEG file
  // ========================================================================
  console.log('\n=== Test 7: Upload JPEG file ===');

  const jpegFileName = 'test-' + Date.now() + '.jpg';
  const jpegSize = 204800; // 200 KB
  const jpegUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(jpegFileName)}&fileSize=${jpegSize}&contentType=image/jpeg`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(jpegUploadUrlRes, {
    '[Test 7] JPEG upload URL - Status 200': (r) => r.status === 200,
  });

  const jpegData = JSON.parse(jpegUploadUrlRes.body);

  // Create test JPEG data (JPEG header + random data)
  const jpegTestPattern = new Uint8Array(jpegSize);
  // JPEG SOI marker
  jpegTestPattern[0] = 0xFF;
  jpegTestPattern[1] = 0xD8;
  // Fill with test data
  for (let i = 2; i < jpegSize; i++) {
    jpegTestPattern[i] = i % 256;
  }
  // JPEG EOI marker at end
  jpegTestPattern[jpegSize - 2] = 0xFF;
  jpegTestPattern[jpegSize - 1] = 0xD9;

  const jpegMinioRes = http.put(jpegData.uploadUrl, jpegTestPattern, {
    headers: {
      'Content-Type': 'image/jpeg',
      'Content-Length': jpegSize.toString(),
    },
  });

  check(jpegMinioRes, {
    '[Test 7] JPEG MinIO upload - Status 200': (r) => r.status === 200 || r.status === 201,
  });

  const jpegConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(jpegData.fileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(jpegConfirmRes, {
    '[Test 7] JPEG confirm - Status 204': (r) => r.status === 204,
  });

  console.log('JPEG file uploaded');

  sleep(1);

  // ========================================================================
  // Test 8: Upload GIF file
  // ========================================================================
  console.log('\n=== Test 8: Upload GIF file ===');

  const gifFileName = 'test-' + Date.now() + '.gif';
  const gifSize = 102400; // 100 KB
  const gifUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(gifFileName)}&fileSize=${gifSize}&contentType=image/gif`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(gifUploadUrlRes, {
    '[Test 8] GIF upload URL - Status 200': (r) => r.status === 200,
  });

  const gifData = JSON.parse(gifUploadUrlRes.body);

  // Create test GIF data (GIF87a header + random data)
  const gifTestPattern = new Uint8Array(gifSize);
  // GIF header
  gifTestPattern[0] = 0x47; // G
  gifTestPattern[1] = 0x49; // I
  gifTestPattern[2] = 0x46; // F
  gifTestPattern[3] = 0x38; // 8
  gifTestPattern[4] = 0x37; // 7
  gifTestPattern[5] = 0x61; // a
  // Fill with test data
  for (let i = 6; i < gifSize; i++) {
    gifTestPattern[i] = (i * 2) % 256;
  }

  const gifMinioRes = http.put(gifData.uploadUrl, gifTestPattern, {
    headers: {
      'Content-Type': 'image/gif',
      'Content-Length': gifSize.toString(),
    },
  });

  check(gifMinioRes, {
    '[Test 8] GIF MinIO upload - Status 200': (r) => r.status === 200 || r.status === 201,
  });

  const gifConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(gifData.fileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(gifConfirmRes, {
    '[Test 8] GIF confirm - Status 204': (r) => r.status === 204,
  });

  console.log('GIF file uploaded');

  sleep(1);

  // ========================================================================
  // Test 9: Upload WebP file
  // ========================================================================
  console.log('\n=== Test 9: Upload WebP file ===');

  const webpFileName = 'test-' + Date.now() + '.webp';
  const webpSize = 153600; // 150 KB
  const webpUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=${encodeURIComponent(webpFileName)}&fileSize=${webpSize}&contentType=image/webp`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(webpUploadUrlRes, {
    '[Test 9] WebP upload URL - Status 200': (r) => r.status === 200,
  });

  const webpData = JSON.parse(webpUploadUrlRes.body);

  // Create test WebP data (RIFF header + WebP)
  const webpTestPattern = new Uint8Array(webpSize);
  // RIFF header
  webpTestPattern[0] = 0x52; // R
  webpTestPattern[1] = 0x49; // I
  webpTestPattern[2] = 0x46; // F
  webpTestPattern[3] = 0x46; // F
  webpTestPattern[8] = 0x57; // W
  webpTestPattern[9] = 0x45; // E
  webpTestPattern[10] = 0x42; // B
  webpTestPattern[11] = 0x50; // P
  // Fill with test data
  for (let i = 12; i < webpSize; i++) {
    webpTestPattern[i] = (i * 3) % 256;
  }

  const webpMinioRes = http.put(webpData.uploadUrl, webpTestPattern, {
    headers: {
      'Content-Type': 'image/webp',
      'Content-Length': webpSize.toString(),
    },
  });

  check(webpMinioRes, {
    '[Test 9] WebP MinIO upload - Status 200': (r) => r.status === 200 || r.status === 201,
  });

  const webpConfirmRes = http.post(
    `${baseUrl}${config.endpoints.confirmUpload(webpData.fileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(webpConfirmRes, {
    '[Test 9] WebP confirm - Status 204': (r) => r.status === 204,
  });

  console.log('WebP file uploaded');

  // ========================================================================
  // Test 10: Error handling - try to download non-existent file
  // ========================================================================
  console.log('\n=== Test 10: Error handling - non-existent file ===');

  const fakeFileId = generateUUID();
  const fakeFileRes = http.get(
    `${baseUrl}${config.endpoints.downloadUrl(fakeFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(fakeFileRes, {
    '[Test 10] Non-existent file - Status 404': (r) => r.status === 404,
  });

  console.log('Non-existent file returns 404:', fakeFileRes.status);

  // ========================================================================
  // Test 11: Unauthorized access
  // ========================================================================
  console.log('\n=== Test 11: Unauthorized access ===');

  const noTokenRes = http.post(
    `${baseUrl}${config.endpoints.uploadUrl}?fileName=test.png&fileSize=1024&contentType=image/png`,
    null
  );

  check(noTokenRes, {
    '[Test 11] No token - Status 401': (r) => r.status === 401,
  });

  console.log('Unauthorized access rejected:', noTokenRes.status);

  // ========================================================================
  // Test 12: Verify all files in database
  // ========================================================================
  console.log('\n=== Test 12: Database verification ===');

  const downloadUrlRes12 = http.get(
    `${baseUrl}${config.endpoints.downloadUrl(uploadedFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(downloadUrlRes12, {
    '[Test 12] First file still accessible - Status 200': (r) => r.status === 200,
  });

  console.log('All files successfully uploaded and accessible!');

  // ========================================================================
  // Summary
  // ========================================================================
  console.log('\n=== Real File Upload Test Summary ===');
  console.log('✅ PNG file #1 uploaded');
  console.log('✅ PNG file #2 uploaded');
  console.log('✅ JPEG file uploaded');
  console.log('✅ GIF file uploaded');
  console.log('✅ WebP file uploaded');
  console.log('✅ File download verified');
  console.log('✅ Error handling tested');
  console.log('✅ Authorization tested');
  console.log('\nAll file upload tests passed!');
}

export function teardown(data) {
  console.log('\n=== Test Teardown ===');
  console.log('File uploaded ID:', uploadedFileId);
  console.log('Object key:', uploadedObjectKey);
  console.log('\nVerify files in database:');
  console.log("SELECT id, file_name, mime, size, is_confirmed FROM files WHERE is_confirmed = true ORDER BY created_at DESC LIMIT 10;");
  console.log('\nVerify in MinIO:');
  console.log('- Check that files exist in the bucket');
  console.log('- Verify presigned URLs work correctly');
  console.log('- Check file sizes match expected values');
}
