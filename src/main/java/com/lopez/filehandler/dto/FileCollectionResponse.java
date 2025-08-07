package com.lopez.filehandler.dto;

import java.util.List;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FileCollectionResponse {
    private String userId;
    private List<FileResponse> files;

    public FileCollectionResponse() {}

    public FileCollectionResponse(String userId, List<FileResponse> files) {
        this.userId = userId;
        this.files = files;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<FileResponse> getFiles() { return files; }
    public void setFiles(List<FileResponse> files) { this.files = files; }
}