package com.lopez.filehandler.dto;

public class MultiFileUploadRequest {
    private String userId;
    private String fileName;
    private String contentType;
    private String encodedFile;

    public MultiFileUploadRequest() {
    }

    public MultiFileUploadRequest(String userId, String fileName, String contentType, String encodedFile) {
        this.userId = userId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.encodedFile = encodedFile;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEncodedFile() {
        return encodedFile;
    }

    public void setEncodedFile(String encodedFile) {
        this.encodedFile = encodedFile;
    }
}