package tech.limaxs.chat.infra.storage;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.limaxs.chat.core.model.FileMetadata;
import tech.limaxs.chat.core.repository.imperative.FileMetadataRepository;
import tech.limaxs.chat.infra.config.FileCleanupConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class FileCleanupService {

    private static final Logger LOG = Logger.getLogger(FileCleanupService.class.getName());

    private final FileMetadataRepository fileRepository;
    private final MinioService minioService;
    private final FileCleanupConfig config;

    public FileCleanupService(
            FileMetadataRepository fileRepository,
            MinioService minioService,
            FileCleanupConfig config) {
        this.fileRepository = fileRepository;
        this.minioService = minioService;
        this.config = config;
    }

    @Scheduled(cron = "{file.cleanup.unconfirmed-cron:0 0 * * * ?}")
    @Transactional
    public void cleanupUnconfirmedUploads() {
        if (!config.isEnabled()) {
            LOG.fine("File cleanup is disabled. Skipping unconfirmed uploads cleanup.");
            return;
        }

        LocalDateTime threshold = LocalDateTime.now()
                .minusHours(config.getConfirmationTimeoutHours());

        LOG.info("Starting cleanup of unconfirmed uploads older than " + threshold);

        try {
            List<FileMetadata> unconfirmedFiles =
                    fileRepository.findUnconfirmedOlderThan(threshold);

            LOG.info("Found " + unconfirmedFiles.size() + " unconfirmed files to clean up");

            for (FileMetadata file : unconfirmedFiles) {
                try {
                    // Delete from MinIO (if uploaded)
                    if (file.getObjectKey() != null && minioService.fileExists(file.getObjectKey())) {
                        minioService.deleteFile(file.getObjectKey());
                    }
                    // Delete from database
                    fileRepository.deleteById(file.getId());
                    LOG.info("Cleaned up unconfirmed file: " + file.getFileName() + " (id: " + file.getId() + ")");
                } catch (Exception e) {
                    LOG.severe("Failed to cleanup file: " + file.getFileName() + " - " + e.getMessage());
                }
            }

            LOG.info("Completed cleanup of unconfirmed uploads");
        } catch (Exception e) {
            LOG.severe("Error during unconfirmed uploads cleanup: " + e.getMessage());
        }
    }

    @Scheduled(cron = "{file.cleanup.expired-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupExpiredFiles() {
        if (!config.isEnabled()) {
            LOG.fine("File cleanup is disabled. Skipping expired files cleanup.");
            return;
        }

        LocalDateTime threshold = LocalDateTime.now()
                .minusDays(config.getRetentionDays());

        LOG.info("Starting cleanup of expired files older than " + threshold);

        try {
            List<FileMetadata> expiredFiles =
                    fileRepository.findExpiredFiles(threshold);

            LOG.info("Found " + expiredFiles.size() + " expired files to clean up");

            for (FileMetadata file : expiredFiles) {
                try {
                    // Delete from MinIO
                    if (file.getObjectKey() != null && minioService.fileExists(file.getObjectKey())) {
                        minioService.deleteFile(file.getObjectKey());
                    }
                    // Delete from database
                    fileRepository.deleteById(file.getId());
                    LOG.info("Cleaned up expired file: " + file.getFileName() + " (id: " + file.getId() + ")");
                } catch (Exception e) {
                    LOG.severe("Failed to cleanup file: " + file.getFileName() + " - " + e.getMessage());
                }
            }

            LOG.info("Completed cleanup of expired files");
        } catch (Exception e) {
            LOG.severe("Error during expired files cleanup: " + e.getMessage());
        }
    }
}
