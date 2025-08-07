package com.lopez.filehandler;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.lopez.filehandler.dto.ApiResponse;
import com.lopez.filehandler.dto.FileCollectionResponse;
import com.lopez.filehandler.dto.FileCollectionResponseWithContent;
import com.lopez.filehandler.dto.FileResponse;
import com.lopez.filehandler.dto.FileResponseWithContent;
import com.lopez.filehandler.dto.FileUploadRequest;
import com.lopez.filehandler.service.FileService;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/file")
@Produces(MediaType.APPLICATION_JSON)
public class FileResource {

    private static final Logger logger = Logger.getLogger(FileResource.class);

    @Inject
    FileService fileService;

    @GET
    public Response getAllFiles() {
        try {
            ApiResponse<List<FileResponse>> result = fileService.getAllFiles();

            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("data", result.getData());
            
            if (result.isSuccess()) {
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(response)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in getAllFiles endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error"))
                    .build();
        }
    }

    @POST
    public Response uploadFile(String rawBody, @Context HttpHeaders headers) {
        try {
            logger.infof("POST /file - Content-Type: %s", headers.getHeaderString("Content-Type"));
            logger.infof("POST /file - Raw body length: %d", rawBody != null ? rawBody.length() : 0);
            logger.infof("POST /file - Raw body preview: %s", rawBody != null ? rawBody.substring(0, Math.min(100, rawBody.length())) : "null");
            
            if (rawBody == null || rawBody.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.badRequest("Request body is required"))
                        .build();
            }

            // Try to parse as FileUploadRequest
            FileUploadRequest request;
            try {
                request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawBody, FileUploadRequest.class);
                logger.infof("Successfully parsed request - userId: %s, fileName: %s", request.getUserId(), request.getFileName());
            } catch (Exception parseError) {
                logger.errorf("Failed to parse JSON: %s", parseError.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.badRequest("Invalid JSON format: " + parseError.getMessage()))
                        .build();
            }

            ApiResponse<FileResponse> result = fileService.uploadFile(request);
            
            if (result.isSuccess()) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(result)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in uploadFile endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/multiple")
    public Response uploadMultipleFiles(List<FileUploadRequest> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.badRequest("At least one file is required"))
                        .build();
            }

            String userId = requests.get(0).getUserId();
            ApiResponse<List<FileResponse>> result = fileService.uploadMultipleFiles(userId, requests);
            
            if (result.isSuccess()) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(result)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in uploadMultipleFiles endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error"))
                    .build();
        }
    }

    @GET
    @Path("/user/{userId}")
    public Response getUserFiles(@PathParam("userId") String userId) {
        try {

            
            ApiResponse<FileCollectionResponseWithContent> result = fileService.getUserFiles(userId);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("data", result.getData());
            
            if (result.isSuccess()) {
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(response)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in getUserFiles endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error"))
                    .build();
        }
    }

    @GET
    @Path("/{fileId}")
    public Response downloadFileById(@PathParam("fileId") String fileId) {
        try {

            
            ApiResponse<byte[]> result = fileService.downloadFileById(fileId);
            
            if (result.isSuccess()) {
                return Response.ok(result.getData())
                        .header("Content-Disposition", "attachment; filename=\"file_" + fileId + "\"")
                        .header("Content-Type", "application/octet-stream")
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .entity(result)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in downloadFileById endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .entity(ApiResponse.error("Internal server error"))
                    .build();
        }
    }

    @DELETE
    public Response deleteAllFiles() {
        try {
            ApiResponse<Void> result = fileService.deleteAllFiles();
            
            if (result.isSuccess()) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(result)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error in deleteAllFiles endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Internal server error"))
                    .build();
        }
    }

    private String getContentTypeFromFileName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}