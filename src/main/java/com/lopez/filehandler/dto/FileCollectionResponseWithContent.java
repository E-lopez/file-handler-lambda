package com.lopez.filehandler.dto;

import java.util.List;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FileCollectionResponseWithContent {
    private String userId;
    private List<FileResponseWithContent> files;

    public FileCollectionResponseWithContent() {}

    public FileCollectionResponseWithContent(String userId, List<FileResponseWithContent> files) {
        this.userId = userId;
        this.files = files;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<FileResponseWithContent> getFiles() { return files; }
    public void setFiles(List<FileResponseWithContent> files) { this.files = files; }
}