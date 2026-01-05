// K6 Test: File Upload Integration Test
// Tests file upload/download via MinIO for NexusChat Engine
// Usage: k6 run 03-file-upload.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, generateUUID, getToken, createAuthHeaders } from './config.js';

// Test configuration
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<1000'], // File operations may take longer
    http_req_failed: ['rate<0.01'],
  },
};

let aliceToken;
let uploadedFileId;
let uploadedObjectKey;

export function setup() {
  // Get token for Alice
  aliceToken = getToken(config.users.alice);

  console.log('Alice token:', aliceToken.substring(0, 20) + '...');

  return {
    aliceToken
  };
}

export default function(data) {
  const baseUrl = config.baseUrl;
  const aliceToken = data.aliceToken;

  // ========================================================================
  // Test 1: Get Upload URL for PDF file
  // ========================================================================
  console.log('\n=== Test 1: Get Upload URL for PDF file ===');

  const fileName = 'test-document-' + Date.now() + '.pdf';
  const fileSize = 102400; // 100 KB
  const contentType = 'application/pdf';

  const uploadUrlParams = `fileName=${encodeURIComponent(fileName)}&fileSize=${fileSize}&contentType=${encodeURIComponent(contentType)}`;
  const uploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?${uploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(uploadUrlRes, {
    'POST /api/back/files/upload-url (PDF) - Status 200': (r) => r.status === 200,
    'POST /api/back/files/upload-url (PDF) - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
    'POST /api/back/files/upload-url (PDF) - Has uploadUrl': (r) => JSON.parse(r.body).uploadUrl !== undefined,
    'POST /api/back/files/upload-url (PDF) - Has objectKey': (r) => JSON.parse(r.body).objectKey !== undefined,
    'POST /api/back/files/upload-url (PDF) - Valid UUID fileId': (r) => {
      const fileId = JSON.parse(r.body).fileId;
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      return uuidRegex.test(fileId);
    },
  });

  const uploadUrlData = JSON.parse(uploadUrlRes.body);
  const pdfFileId = uploadUrlData.fileId;
  const pdfUploadUrl = uploadUrlData.uploadUrl;
  const pdfObjectKey = uploadUrlData.objectKey;

  console.log('PDF upload response:', JSON.stringify(uploadUrlData, null, 2));
  console.log('PDF fileId:', pdfFileId);
  console.log('PDF uploadUrl:', pdfUploadUrl.substring(0, 50) + '...');

  sleep(1);

  // ========================================================================
  // Test 2: Upload file to MinIO (simulate upload)
  // ========================================================================
  console.log('\n=== Test 2: Upload file to MinIO (simulate) ===');

  // Create test file content
  const testFileContent = 'This is a test PDF file content for K6 testing. '.repeat(1000);
  const fileParams = {
    headers: {
      'Content-Type': contentType,
      'Content-Length': fileSize.toString(),
    },
    body: testFileContent,
  };

  // Note: In real scenario, we would upload to MinIO using the presigned URL
  // For testing, we'll just verify the URL is valid format
  check(pdfUploadUrl, {
    'MinIO Upload URL - Is HTTPS or HTTP': (url) => url.startsWith('http://') || url.startsWith('https://'),
    'MinIO Upload URL - Contains endpoint': (url) => url.includes('localhost') || url.includes('minio') || url.includes('s3'),
  });

  console.log('MinIO upload URL is valid');

  // ========================================================================
  // Test 3: Confirm Upload (PDF)
  // ========================================================================
  console.log('\n=== Test 3: Confirm Upload (PDF) ===');

  const confirmUploadRes = http.post(
    `${baseUrl}${config.endpoints.backConfirmUpload(pdfFileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(confirmUploadRes, {
    'POST /api/back/files/{fileId}/confirm - Status 204': (r) => r.status === 204,
  });

  console.log('PDF upload confirmed');

  sleep(1);

  // ========================================================================
  // Test 4: Get Download URL (PDF)
  // ========================================================================
  console.log('\n=== Test 4: Get Download URL (PDF) ===');

  const downloadUrlRes = http.get(
    `${baseUrl}${config.endpoints.frontDownloadUrl(pdfFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(downloadUrlRes, {
    'GET /api/front/files/{fileId}/download-url - Status 200': (r) => r.status === 200,
    'GET /api/front/files/{fileId}/download-url - Has downloadUrl': (r) => {
      const body = JSON.parse(r.body);
      return body.downloadUrl !== undefined;
    },
    'GET /api/front/files/{fileId}/download-url - Valid URL format': (r) => {
      const body = JSON.parse(r.body);
      return body.downloadUrl.startsWith('http://') || body.downloadUrl.startsWith('https://');
    },
  });

  const downloadUrlData = JSON.parse(downloadUrlRes.body);
  console.log('Download URL:', downloadUrlData.downloadUrl.substring(0, 50) + '...');

  sleep(1);

  // ========================================================================
  // Test 5: Get Upload URL for Image
  // ========================================================================
  console.log('\n=== Test 5: Get Upload URL for Image ===');

  const imageFileName = 'test-image-' + Date.now() + '.png';
  const imageFileSize = 512000; // 500 KB
  const imageContentType = 'image/png';

  const imageUploadUrlParams = `fileName=${encodeURIComponent(imageFileName)}&fileSize=${imageFileSize}&contentType=${encodeURIComponent(imageContentType)}`;
  const imageUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?${imageUploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(imageUploadUrlRes, {
    'POST /api/back/files/upload-url (PNG) - Status 200': (r) => r.status === 200,
    'POST /api/back/files/upload-url (PNG) - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
  });

  const imageData = JSON.parse(imageUploadUrlRes.body);
  const imageFileId = imageData.fileId;

  console.log('Image fileId:', imageFileId);

  sleep(1);

  // ========================================================================
  // Test 6: Confirm Upload (Image)
  // ========================================================================
  console.log('\n=== Test 6: Confirm Upload (Image) ===');

  const confirmImageUploadRes = http.post(
    `${baseUrl}${config.endpoints.backConfirmUpload(imageFileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(confirmImageUploadRes, {
    'POST /api/back/files/{fileId}/confirm (image) - Status 204': (r) => r.status === 204,
  });

  console.log('Image upload confirmed');

  sleep(1);

  // ========================================================================
  // Test 7: Get Upload URL for Video
  // ========================================================================
  console.log('\n=== Test 7: Get Upload URL for Video ===');

  const videoFileName = 'test-video-' + Date.now() + '.mp4';
  const videoFileSize = 5242880; // 5 MB
  const videoContentType = 'video/mp4';

  const videoUploadUrlParams = `fileName=${encodeURIComponent(videoFileName)}&fileSize=${videoFileSize}&contentType=${encodeURIComponent(videoContentType)}`;
  const videoUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?${videoUploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(videoUploadUrlRes, {
    'POST /api/back/files/upload-url (MP4) - Status 200': (r) => r.status === 200,
    'POST /api/back/files/upload-url (MP4) - Has fileId': (r) => JSON.parse(r.body).fileId !== undefined,
  });

  const videoData = JSON.parse(videoUploadUrlRes.body);
  const videoFileId = videoData.fileId;

  console.log('Video fileId:', videoFileId);

  sleep(1);

  // ========================================================================
  // Test 8: Confirm Upload (Video)
  // ========================================================================
  console.log('\n=== Test 8: Confirm Upload (Video) ===');

  const confirmVideoUploadRes = http.post(
    `${baseUrl}${config.endpoints.backConfirmUpload(videoFileId)}`,
    '',
    { headers: createAuthHeaders(aliceToken) }
  );

  check(confirmVideoUploadRes, {
    'POST /api/back/files/{fileId}/confirm (video) - Status 204': (r) => r.status === 204,
  });

  console.log('Video upload confirmed');

  sleep(1);

  // ========================================================================
  // Test 9: Get Download URL for all files
  // ========================================================================
  console.log('\n=== Test 9: Get Download URL for all files ===');

  const imageDownloadUrlRes = http.get(
    `${baseUrl}${config.endpoints.frontDownloadUrl(imageFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  const videoDownloadUrlRes = http.get(
    `${baseUrl}${config.endpoints.frontDownloadUrl(videoFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(imageDownloadUrlRes, {
    'GET /api/front/files/{fileId}/download-url (image) - Status 200': (r) => r.status === 200,
  });

  check(videoDownloadUrlRes, {
    'GET /api/front/files/{fileId}/download-url (video) - Status 200': (r) => r.status === 200,
  });

  console.log('All download URLs retrieved successfully');

  sleep(1);

  // ========================================================================
  // Test 10: Invalid file size (too large)
  // ========================================================================
  console.log('\n=== Test 10: Invalid file size (too large) ===');

  const hugeFileName = 'huge-file.bin';
  const hugeFileSize = 107374182400; // 100 GB - unrealistic size
  const hugeContentType = 'application/octet-stream';

  const hugeUploadUrlParams = `fileName=${encodeURIComponent(hugeFileName)}&fileSize=${hugeFileSize}&contentType=${encodeURIComponent(hugeContentType)}`;
  const hugeUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?${hugeUploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(hugeUploadUrlRes, {
    'POST /api/back/files/upload-url (huge) - Status 400 or 413': (r) => r.status === 400 || r.status === 413,
  });

  console.log('Large file rejected:', hugeUploadUrlRes.status);

  sleep(1);

  // ========================================================================
  // Test 11: Invalid content type
  // ========================================================================
  console.log('\n=== Test 11: Invalid content type ===');

  const invalidFileName = 'test.exe';
  const invalidFileSize = 1024;
  const invalidContentType = 'application/exe';

  const invalidUploadUrlParams = `fileName=${encodeURIComponent(invalidFileName)}&fileSize=${invalidFileSize}&contentType=${encodeURIComponent(invalidContentType)}`;
  const invalidUploadUrlRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?${invalidUploadUrlParams}`,
    null,
    { headers: createAuthHeaders(aliceToken) }
  );

  // May be rejected or accepted depending on server validation
  console.log('Invalid content type response:', invalidUploadUrlRes.status);

  sleep(1);

  // ========================================================================
  // Test 12: Non-existent file ID for download
  // ========================================================================
  console.log('\n=== Test 12: Non-existent file ID for download ===');

  const fakeFileId = generateUUID();
  const fakeFileDownloadRes = http.get(
    `${baseUrl}${config.endpoints.frontDownloadUrl(fakeFileId)}`,
    { headers: createAuthHeaders(aliceToken) }
  );

  check(fakeFileDownloadRes, {
    'GET /api/front/files/{fakeFileId}/download-url - Status 404': (r) => r.status === 404,
  });

  console.log('Non-existent file ID response:', fakeFileDownloadRes.status);

  // ========================================================================
  // Test 13: Unauthorized access (no token)
  // ========================================================================
  console.log('\n=== Test 13: Unauthorized access (no token) ===');

  const noTokenUploadRes = http.post(
    `${baseUrl}${config.endpoints.backUploadUrl}?fileName=test.pdf&fileSize=1024&contentType=application/pdf`,
    null
  );

  check(noTokenUploadRes, {
    'POST /api/back/files/upload-url (no token) - Status 401': (r) => r.status === 401,
  });

  console.log('No token response:', noTokenUploadRes.status);

  sleep(1);

  // ========================================================================
  // Test 14: Multiple file uploads in sequence
  // ========================================================================
  console.log('\n=== Test 14: Multiple file uploads in sequence ===');

  const uploadedFileIds = [];

  for (let i = 0; i < 3; i++) {
    const fileName = `batch-file-${i}-${Date.now()}.txt`;
    const fileSize = 2048;
    const contentType = 'text/plain';

    const batchUploadUrlParams = `fileName=${encodeURIComponent(fileName)}&fileSize=${fileSize}&contentType=${encodeURIComponent(contentType)}`;
    const batchUploadUrlRes = http.post(
      `${baseUrl}${config.endpoints.backUploadUrl}?${batchUploadUrlParams}`,
      null,
      { headers: createAuthHeaders(aliceToken) }
    );

    if (batchUploadUrlRes.status === 200) {
      const batchData = JSON.parse(batchUploadUrlRes.body);
      uploadedFileIds.push(batchData.fileId);

      // Confirm upload
      http.post(
        `${baseUrl}${config.endpoints.backConfirmUpload(batchData.fileId)}`,
        '',
        { headers: createAuthHeaders(aliceToken) }
      );
    }
  }

  check(uploadedFileIds, {
    'Multiple uploads - All succeeded': () => uploadedFileIds.length === 3,
  });

  console.log('Uploaded file IDs:', uploadedFileIds);

  // ========================================================================
  // Summary
  // ========================================================================
  console.log('\n=== File Upload Test Summary ===');
  console.log('PDF file ID:', pdfFileId);
  console.log('Image file ID:', imageFileId);
  console.log('Video file ID:', videoFileId);
  console.log('Batch upload IDs:', uploadedFileIds);
  console.log('All file upload tests completed!');
}

export function teardown(data) {
  console.log('\n=== Test Teardown ===');
  console.log('File upload tests completed!');
  console.log('\nVerify files in database:');
  console.log("SELECT id, file_name, mime, size, is_confirmed FROM files WHERE is_confirmed = true ORDER BY created_at DESC LIMIT 10;");
  console.log('\nExpected files:');
  console.log('- PDF file: 100 KB');
  console.log('- PNG image: 500 KB');
  console.log('- MP4 video: 5 MB');
  console.log('- 3 batch text files: 2 KB each');
}
