package com.lopez.filehandler.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.lopez.filehandler.dto.ApiResponse;
import com.lopez.filehandler.dto.FileCollectionResponse;
import com.lopez.filehandler.dto.FileCollectionResponseWithContent;
import com.lopez.filehandler.dto.FileResponse;
import com.lopez.filehandler.dto.FileResponseWithContent;
import com.lopez.filehandler.dto.FileUploadRequest;
import com.lopez.filehandler.dto.MultiFileUploadRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;

@ApplicationScoped
public class FileService {

    private static final Logger logger = Logger.getLogger(FileService.class);
    private static final List<String> ALLOWED_TYPES = Arrays.asList("application/pdf", "image/png", "image/jpeg");

    S3Client s3Client;

    @jakarta.annotation.PostConstruct
    void initS3Client() {
        try {
            s3Client = S3Client.create();
        } catch (Exception e) {
            // S3 client will be null - handled in methods
        }
    }

    @ConfigProperty(name = "file-handler.s3.bucket-name", defaultValue = "test-bucket")
    String bucketName;

    @ConfigProperty(name = "file-handler.s3.storage-class", defaultValue = "STANDARD")
    String storageClass;

    public ApiResponse<FileResponse> uploadFile(FileUploadRequest request) {
        try {
            if (s3Client == null) {
                return ApiResponse.error("S3 service unavailable");
            }

            if (!isValidFileType(request.getContentType())) {
                return ApiResponse.badRequest("Only PDF, PNG, and JPG files are allowed");
            }

            String fileId = UUID.randomUUID().toString();
            String s3Key = generateS3Key(request.getUserId(), fileId, request.getFileName());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(request.getContentType())
                    .storageClass(StorageClass.fromValue(storageClass))
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(request.getFileData()));

            FileResponse response = new FileResponse(
                    fileId,
                    request.getFileName(),
                    request.getContentType(),
                    s3Key,
                    request.getFileData().length,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            logger.infof("File uploaded successfully: %s", s3Key);
            return ApiResponse.success("File uploaded successfully", response);

        } catch (Exception e) {
            logger.error("Error uploading file", e);
            return ApiResponse.error("Failed to upload file: " + e.getMessage());
        }
    }

    private ApiResponse<FileResponse> uploadFile(MultiFileUploadRequest request) {
        try {
            if (s3Client == null) {
                return ApiResponse.error("S3 service unavailable");
            }

            if (!isValidFileType(request.getContentType())) {
                return ApiResponse.badRequest("Only PDF, PNG, and JPG files are allowed");
            }

            String fileId = UUID.randomUUID().toString();
            String s3Key = generateS3Key(request.getUserId(), fileId, request.getFileName());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(request.getContentType())
                    .storageClass(StorageClass.fromValue(storageClass))
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(request.getEncodedFile()));

            FileResponse response = new FileResponse(
                    fileId,
                    request.getFileName(),
                    request.getContentType(),
                    s3Key,
                    request.getEncodedFile().length(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            logger.infof("File uploaded successfully: %s", s3Key);
            return ApiResponse.success("File uploaded successfully", response);

        } catch (Exception e) {
            logger.error("Error uploading file", e);
            return ApiResponse.error("Failed to upload file: " + e.getMessage());
        }
    }

    public ApiResponse<List<FileResponse>> uploadMultipleFiles(String userId, List<FileUploadRequest> requests) {
        List<FileResponse> responses = new ArrayList<>();

        for (FileUploadRequest request : requests) {
            MultiFileUploadRequest multiRequest = new MultiFileUploadRequest(
                    userId,
                    request.getFileName(),
                    request.getContentType(),
                    Base64.getEncoder().encodeToString(request.getFileData()));

            ApiResponse<FileResponse> result = uploadFile(multiRequest);

            if (result.isSuccess()) {
                responses.add(result.getData());
            } else {
                return ApiResponse.error("Failed to upload file: " + request.getFileName());
            }
        }

        return ApiResponse.success("Files uploaded successfully", responses);
    }

    public ApiResponse<FileCollectionResponseWithContent> getUserFiles(String userId) {
        try {
            String prefix = "users/" + userId + "/";

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<FileResponseWithContent> files = new ArrayList<>();

            for (S3Object s3Object : listResponse.contents()) {
                String[] keyParts = s3Object.key().split("/");
                String fileName = keyParts[keyParts.length - 1];

                // Download file content
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();

                byte[] fileData = s3Client.getObject(getRequest).readAllBytes();
                String base64Content = java.util.Base64.getEncoder().encodeToString(fileData);

                files.add(new FileResponseWithContent(
                        extractFileId(s3Object.key()),
                        fileName,
                        getContentTypeFromKey(s3Object.key()),
                        s3Object.key(),
                        s3Object.size(),
                        s3Object.lastModified().toString(),
                        base64Content));
            }

            FileCollectionResponseWithContent response = new FileCollectionResponseWithContent(userId, files);
            return files.isEmpty()
                    ? ApiResponse.error("No files found for user")
                    : ApiResponse.success(response);

        } catch (Exception e) {
            logger.error("Error fetching user files", e);
            return ApiResponse.error("Failed to fetch files: " + e.getMessage());
        }
    }

    public ApiResponse<byte[]> downloadFile(String userId, String fileId) {
        try {
            String s3Key = findS3KeyByFileId(userId, fileId);
            if (s3Key == null) {
                return ApiResponse.error("File not found");
            }

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            byte[] fileData = s3Client.getObject(getRequest).readAllBytes();
            return ApiResponse.success(fileData);

        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return ApiResponse.error("Failed to download file: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String contentType) {
        return ALLOWED_TYPES.contains(contentType);
    }

    private String generateS3Key(String userId, String fileId, String fileName) {
        return String.format("users/%s/%s_%s", userId, fileId, fileName);
    }

    private String extractFileId(String s3Key) {
        String[] parts = s3Key.split("/");
        String fileName = parts[parts.length - 1];
        return fileName.split("_")[0];
    }

    private String getContentTypeFromKey(String s3Key) {
        if (s3Key.endsWith(".pdf"))
            return "application/pdf";
        if (s3Key.endsWith(".png"))
            return "image/png";
        if (s3Key.endsWith(".jpg") || s3Key.endsWith(".jpeg"))
            return "image/jpeg";
        return "application/octet-stream";
    }

    private String findS3KeyByFileId(String userId, String fileId) {
        try {
            String prefix = "users/" + userId + "/";
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : listResponse.contents()) {
                if (extractFileId(s3Object.key()).equals(fileId)) {
                    return s3Object.key();
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding S3 key", e);
            return null;
        }
    }

    public ApiResponse<List<FileResponse>> getAllFiles() {
        try {
            if (s3Client == null) {
                return ApiResponse.error("S3 service unavailable");
            }

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<FileResponse> files = new ArrayList<>();

            for (S3Object s3Object : listResponse.contents()) {
                String[] keyParts = s3Object.key().split("/");
                String fileName = keyParts[keyParts.length - 1];

                files.add(new FileResponse(
                        extractFileId(s3Object.key()),
                        fileName,
                        getContentTypeFromKey(s3Object.key()),
                        s3Object.key(),
                        s3Object.size(),
                        s3Object.lastModified().toString()));
            }
            return ApiResponse.success(files);

        } catch (Exception e) {
            logger.error("Error fetching all files", e);
            return ApiResponse.error("Failed to fetch files: " + e.getMessage());
        }
    }

    public ApiResponse<Void> deleteAllFiles() {
        try {
            if (s3Client == null) {
                return ApiResponse.error("S3 service unavailable");
            }

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : listResponse.contents()) {
                s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build());
            }

            return ApiResponse.success("All files deleted", null);

        } catch (Exception e) {
            logger.error("Error deleting all files", e);
            return ApiResponse.error("Failed to delete files: " + e.getMessage());
        }
    }

    public ApiResponse<byte[]> downloadFileById(String fileId) {
        try {
            if (s3Client == null) {
                logger.error("S3 client is null");
                return ApiResponse.error("S3 service unavailable");
            }

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : listResponse.contents()) {
                String extractedId = extractFileId(s3Object.key());

                if (extractedId.equals(fileId)) {
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Object.key())
                            .build();

                    byte[] fileData = s3Client.getObject(getRequest).readAllBytes();
                    return ApiResponse.success(fileData);
                }
            }
            return ApiResponse.error("File not found");

        } catch (Exception e) {
            logger.error("Error downloading file by ID", e);
            return ApiResponse.error("Failed to download file: " + e.getMessage());
        }
    }
}