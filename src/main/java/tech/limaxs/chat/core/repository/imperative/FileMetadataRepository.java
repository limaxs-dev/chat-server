package tech.limaxs.chat.core.repository.imperative;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import tech.limaxs.chat.core.model.FileMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FileMetadataRepository implements PanacheRepositoryBase<FileMetadata, UUID> {

    public Optional<FileMetadata> findByIdAndUploaderId(UUID id, UUID uploaderId) {
        return find("id = ?1 and uploaderId = ?2", id, uploaderId).firstResultOptional();
    }

    public Optional<FileMetadata> findByObjectKey(String objectKey) {
        return find("objectKey = ?1", objectKey).firstResultOptional();
    }

    public List<FileMetadata> findUnconfirmedOlderThan(LocalDateTime threshold) {
        return list("confirmed = false AND createdAt < ?1", threshold);
    }

    public List<FileMetadata> findExpiredFiles(LocalDateTime threshold) {
        return list("expirationDate IS NOT NULL AND expirationDate < ?1", threshold);
    }
}
