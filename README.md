# File Handler

A Quarkus-based AWS Lambda function for handling file uploads and downloads with S3 storage. Supports PDF, PNG, and JPG files with environment-specific storage optimization.

## Overview

This lambda function replaces the Spring Boot file-service microservice and provides:

1. File upload (single and multiple) to S3
2. File download from S3
3. List user files
4. Environment-specific S3 storage classes for cost optimization

## Architecture

- **Framework**: Quarkus with AWS Lambda HTTP extension
- **Storage**: AWS S3 with environment-specific storage classes
- **Supported Files**: PDF, PNG, JPG
- **Runtime**: Java 17 on AWS Lambda (provided.al2)

## Storage Strategy

### Development Environment
- **Storage Class**: STANDARD
- **Use Case**: Frequent read/write operations during development
- **Cost**: Higher storage cost, lower access cost

### Production Environment
- **Storage Class**: STANDARD_IA (Infrequent Access)
- **Use Case**: Infrequent reads (5x/year), rare writes (1x/year)
- **Cost**: Lower storage cost, higher access cost
- **Lifecycle**: Auto-transition to Glacier after 90 days

## Endpoints

- `POST /files/upload/{userId}` - Upload single file
- `POST /files/upload/multiple/{userId}` - Upload multiple files
- `GET /files/user/{userId}` - List user files
- `GET /files/download/{userId}/{fileId}` - Download file
- `GET /health` - Health check

## Configuration

Environment variables:
- `S3_BUCKET_NAME` - S3 bucket name
- `AWS_REGION` - AWS region
- `QUARKUS_PROFILE` - Environment profile (dev/prod)

## Building and Deployment

1. **Development deployment**:
   ```bash
   ./deploy.sh dev
   ```

2. **Production deployment**:
   ```bash
   ./deploy.sh prod
   ```

3. **Custom bucket name**:
   ```bash
   ./deploy.sh prod my-custom-bucket
   ```

## File Upload Example

```bash
curl -X POST https://your-api-gateway-url/files/upload/user123 \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "document.pdf",
    "contentType": "application/pdf",
    "fileData": "base64-encoded-file-data"
  }'
```

## Cost Optimization

- **Dev**: Uses STANDARD storage for frequent access
- **Prod**: Uses STANDARD_IA for cost savings on infrequent access
- **Lifecycle**: Automatic transition to Glacier for long-term archival
- **Versioning**: Enabled for data protection

## Testing

Run tests with:
```bash
mvn test
```

## Local Development

For local development:
```bash
mvn quarkus:dev
```