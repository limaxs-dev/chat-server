package tech.limaxs.chat.api.rest.resource;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.limaxs.chat.api.rest.dto.FileUploadResponse;
import tech.limaxs.chat.api.rest.dto.DownloadUrlResponse;
import tech.limaxs.chat.core.model.ChatUser;
import tech.limaxs.chat.core.model.FileMetadata;
import tech.limaxs.chat.core.repository.imperative.ChatUserRepository;
import tech.limaxs.chat.core.repository.imperative.FileMetadataRepository;
import tech.limaxs.chat.infra.auth.JwtPrincipal;
import tech.limaxs.chat.infra.storage.MinioService;

import java.util.Optional;
import java.util.UUID;

@Path("/api/files")
@RunOnVirtualThread
@ApplicationScoped
public class FileResource {

    private final FileMetadataRepository fileMetadataRepository;
    private final ChatUserRepository userRepository;
    private final MinioService minioService;
    private final JwtPrincipal jwtPrincipal;

    public FileResource(
            FileMetadataRepository fileMetadataRepository,
            ChatUserRepository userRepository,
            MinioService minioService,
            JwtPrincipal jwtPrincipal) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.jwtPrincipal = jwtPrincipal;
    }

    // Maximum file size: 100 MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    @POST
    @Path("/upload-url")
    @Transactional
    public Response getUploadUrl(@QueryParam("fileName") String fileName,
                                  @QueryParam("fileSize") long fileSize,
                                  @QueryParam("contentType") String contentType) {
        // Validate file size
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"File size must be between 1 and " + (MAX_FILE_SIZE / 1024 / 1024) + " MB\"}")
                    .build();
        }

        UUID uploaderId = jwtPrincipal.getUserId();

        // Verify user exists and get the entity
        ChatUser uploader = Optional.ofNullable(userRepository.findById(uploaderId))
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Generate object key first (required field)
        String objectKey = minioService.generateObjectKey(uploaderId, fileName);

        // Create file metadata record
        FileMetadata metadata = new FileMetadata();
        metadata.setUploader(uploader); // Set the entity, not just the ID
        metadata.setFileName(fileName);
        metadata.setMime(contentType);
        metadata.setSize(fileSize);
        metadata.setObjectKey(objectKey);
        fileMetadataRepository.persist(metadata);

        String uploadUrl = minioService.getPresignedUploadUrl(objectKey, 3600);

        return Response.ok(FileUploadResponse.create(metadata.getId(), uploadUrl, objectKey)).build();
    }

    @POST
    @Path("/{fileId}/confirm")
    @Transactional
    public Response confirmUpload(@PathParam("fileId") UUID fileId) {
        UUID uploaderId = jwtPrincipal.getUserId();

        FileMetadata metadata = fileMetadataRepository.findByIdAndUploaderId(fileId, uploaderId)
                .orElseThrow(() -> new NotFoundException("File not found"));

        metadata.setConfirmed(true);
        fileMetadataRepository.persist(metadata);

        return Response.noContent().build();
    }

    @GET
    @Path("/{fileId}/download-url")
    public Response getDownloadUrl(@PathParam("fileId") UUID fileId) {
        UUID userId = jwtPrincipal.getUserId();

        FileMetadata metadata = Optional.ofNullable(fileMetadataRepository.findById(fileId))
                .orElseThrow(() -> new NotFoundException("File not found"));

        String downloadUrl = minioService.getPresignedDownloadUrl(metadata.getObjectKey(), 3600);

        return Response.ok(new DownloadUrlResponse(downloadUrl)).build();
    }
}
