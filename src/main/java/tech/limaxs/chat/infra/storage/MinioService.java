package tech.limaxs.chat.infra.storage;

import io.minio.*;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class MinioService {

    private static final Logger LOG = Logger.getLogger(MinioService.class.getName());

    private MinioClient minioClient;
    private String bucketName;

    @ConfigProperty(name = "minio.endpoint")
    String endpoint;

    @ConfigProperty(name = "minio.access-key")
    String accessKey;

    @ConfigProperty(name = "minio.secret-key")
    String secretKey;

    @ConfigProperty(name = "minio.bucket.name")
    String configuredBucketName;

    @ConfigProperty(name = "minio.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "minio.upload.url-expiry-seconds", defaultValue = "3600")
    int uploadUrlExpirySeconds;

    @ConfigProperty(name = "minio.download.url-expiry-seconds", defaultValue = "3600")
    int downloadUrlExpirySeconds;

    public MinioService() {
        // MinioClient will be initialized in @PostConstruct
        this.minioClient = null;
        this.bucketName = null;
    }

    @PostConstruct
    public void init() {
        // Initialize MinIO client
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();

        this.bucketName = configuredBucketName;

        // Ensure bucket exists and is private
        try {
            ensureBucketExists();
            setBucketToPrivate();
            LOG.info("MinIO service initialized successfully with bucket: " + bucketName);
        } catch (Exception e) {
            LOG.severe("Failed to initialize MinIO service: " + e.getMessage());
            throw new RuntimeException("MinIO initialization failed", e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!found) {
            LOG.info("Bucket '" + bucketName + "' does not exist. Creating...");
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .region(region)
                    .build());
            LOG.info("Bucket '" + bucketName + "' created successfully.");
        } else {
            LOG.info("Bucket '" + bucketName + "' already exists.");
        }
    }

    private void setBucketToPrivate() throws Exception {
        // Set bucket policy to private (no public access)
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";

        try {
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
            LOG.info("Bucket '" + bucketName + "' set to private.");
        } catch (Exception e) {
            LOG.warning("Could not set bucket policy: " + e.getMessage());
            // Some MinIO versions might not support this, continue anyway
        }
    }

    public String generateObjectKey(java.util.UUID uploaderId, String fileName) {
        return String.format("%d/%s/%s", System.currentTimeMillis(), uploaderId, fileName);
    }

    public String getPresignedUploadUrl(String objectKey, int expirySeconds) {
        try {
            int actualExpiry = (expirySeconds > 0) ? expirySeconds : uploadUrlExpirySeconds;
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(actualExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            LOG.severe("Failed to generate presigned upload URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }

    public String getPresignedDownloadUrl(String objectKey, int expirySeconds) {
        try {
            int actualExpiry = (expirySeconds > 0) ? expirySeconds : downloadUrlExpirySeconds;
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(actualExpiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            LOG.severe("Failed to generate presigned download URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            LOG.info("File deleted from MinIO: " + objectKey);
        } catch (Exception e) {
            LOG.severe("Failed to delete file from MinIO: " + e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public void uploadFile(String objectKey, byte[] data, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new java.io.ByteArrayInputStream(data), size, -1)
                            .contentType(contentType)
                            .build()
            );
            LOG.info("File uploaded to MinIO: " + objectKey);
        } catch (Exception e) {
            LOG.severe("Failed to upload file to MinIO: " + e.getMessage());
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public boolean fileExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }

    public String getBucketName() {
        return bucketName;
    }
}
