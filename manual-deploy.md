# Manual Deployment Guide

## Prerequisites

- AWS CLI configured with appropriate permissions
- Maven 3.6+ installed
- Docker (for native compilation)
- S3 bucket permissions for file operations

## Build Steps

1. **Clean and compile**:
   ```bash
   mvn clean compile
   ```

2. **Run tests**:
   ```bash
   mvn test
   ```

3. **Build native executable**:
   ```bash
   mvn package -Dnative -Dquarkus.native.container-build=true
   ```

## Deployment Steps

### Create Deployment Bucket (One-time setup)

```bash
aws s3 mb s3://your-deployment-bucket
```

### Development Environment

1. **Upload function and deploy**:
   ```bash
   # Upload function package
   aws s3 cp target/function.zip s3://your-deployment-bucket/file-handler/function.zip
   
   # Deploy stack
   aws cloudformation deploy \
     --template-file cloudformation-template.yaml \
     --stack-name file-handler-stack-dev \
     --capabilities CAPABILITY_IAM \
     --parameter-overrides \
       Environment=dev \
       S3BucketName=file-handler-bucket \
       DeploymentBucket=your-deployment-bucket
   ```

### Production Environment

1. **Upload function and deploy**:
   ```bash
   # Upload function package
   aws s3 cp target/function.zip s3://your-deployment-bucket/file-handler/function.zip
   
   # Deploy stack
   aws cloudformation deploy \
     --template-file cloudformation-template.yaml \
     --stack-name file-handler-stack-prod \
     --capabilities CAPABILITY_IAM \
     --parameter-overrides \
       Environment=prod \
       S3BucketName=file-handler-bucket \
       DeploymentBucket=your-deployment-bucket
   ```

## S3 Storage Classes

### Development
- **Storage Class**: STANDARD
- **Rationale**: Frequent read/write during development
- **Cost**: ~$0.023/GB/month

### Production
- **Storage Class**: STANDARD_IA
- **Rationale**: Infrequent access (5 reads/year, 1 write/year)
- **Cost**: ~$0.0125/GB/month storage + $0.01/GB retrieval
- **Lifecycle**: Auto-transition to Glacier after 90 days

## File Size Limits

- **Lambda payload**: 6MB (base64 encoded)
- **Direct S3 upload**: Up to 5TB per file
- **Supported formats**: PDF, PNG, JPG only

## Testing Endpoints

### Upload File
```bash
curl -X POST https://your-api-url/files/upload/user123 \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "test.pdf",
    "contentType": "application/pdf",
    "fileData": "'$(base64 -i test.pdf)'"
  }'
```

### List Files
```bash
curl https://your-api-url/files/user/user123
```

### Download File
```bash
curl https://your-api-url/files/download/user123/file-id \
  -o downloaded-file.pdf
```

## Monitoring

- CloudWatch logs: `/aws/lambda/file-handler-{env}`
- S3 metrics: Storage usage, request metrics
- Lambda metrics: Duration, errors, throttles

## Troubleshooting

1. **File upload fails**: Check file type and size limits
2. **S3 access denied**: Verify IAM permissions
3. **Lambda timeout**: Increase timeout for large files
4. **Native build fails**: Ensure Docker is running