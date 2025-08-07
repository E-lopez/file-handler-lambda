package com.lopez.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FileResponse {
    private String fileId;
    private String fileName;
    private String contentType;
    private String s3Key;
    private long size;
    private String uploadDate;

    public FileResponse() {}

    public FileResponse(String fileId, String fileName, String contentType, String s3Key, long size, String uploadDate) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.s3Key = s3Key;
        this.size = size;
        this.uploadDate = uploadDate;
    }

    @JsonProperty
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    @JsonProperty
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    @JsonProperty
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    @JsonProperty
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    @JsonProperty
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    @JsonProperty
    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }
}