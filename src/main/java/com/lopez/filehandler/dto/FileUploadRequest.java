package com.lopez.filehandler.dto;

public class FileUploadRequest {
    private String userId;
    private String fileName;
    private String contentType;
    private byte[] fileData;

    public FileUploadRequest() {}

    public FileUploadRequest(String userId, String fileName, String contentType, byte[] fileData) {
        this.userId = userId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileData = fileData;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
}