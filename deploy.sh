#!/bin/bash

# Set environment (default to dev)
ENVIRONMENT=${1:-dev}
S3_BUCKET_NAME=${2:-file-handler-bucket}
DEPLOYMENT_BUCKET=${3:-file-handler-deployment-bucket}

echo "Deploying file-handler to environment: $ENVIRONMENT"

# Build the application
echo "Building the application..."
mvn clean package -Dnative -Dquarkus.native.container-build=true -Dquarkus.profile=$ENVIRONMENT

# Check if build was successful
# if [ $? -ne 0 ]; then
#     echo "Build failed!"
#     exit 1
# fi

# Create deployment bucket if it doesn't exist
echo "Creating deployment bucket if needed..."
if ! aws s3 ls s3://$DEPLOYMENT_BUCKET 2>/dev/null; then
    echo "Creating bucket: $DEPLOYMENT_BUCKET"
    aws s3 mb s3://$DEPLOYMENT_BUCKET
    if [ $? -ne 0 ]; then
        echo "Failed to create deployment bucket. Please check your AWS permissions."
        exit 1
    fi
else
    echo "Bucket $DEPLOYMENT_BUCKET already exists"
fi

# Upload function package to S3
echo "Uploading function package..."
if [ ! -f target/function.zip ]; then
    echo "Error: target/function.zip not found. Build may have failed."
    exit 1
fi

aws s3 cp target/function.zip s3://$DEPLOYMENT_BUCKET/file-handler/function.zip
if [ $? -ne 0 ]; then
    echo "Failed to upload function package. Please check your AWS permissions for bucket: $DEPLOYMENT_BUCKET"
    echo "Required permissions: s3:PutObject, s3:PutObjectAcl"
    exit 1
fi

# Deploy using CloudFormation
echo "Deploying to AWS..."
aws cloudformation deploy \
    --template-file cloudformation-template.yaml \
    --stack-name file-handler-stack-$ENVIRONMENT \
    --capabilities CAPABILITY_IAM \
    --parameter-overrides \
        Environment=$ENVIRONMENT \
        S3BucketName=$S3_BUCKET_NAME \
        DeploymentBucket=$DEPLOYMENT_BUCKET

# Get API Gateway URL from CloudFormation outputs
API_URL=$(aws cloudformation describe-stacks \
    --stack-name file-handler-stack-$ENVIRONMENT \
    --query 'Stacks[0].Outputs[?OutputKey==`FileHandlerApi`].OutputValue' \
    --output text)

echo "Deployment complete!"
echo "Environment: $ENVIRONMENT"
echo "S3 Bucket: $S3_BUCKET_NAME-$ENVIRONMENT"
echo "Deployment Bucket: $DEPLOYMENT_BUCKET"
echo ""
echo "API Endpoints:"
echo "  URL:  ${API_URL}"
echo "  Health Check:     GET  ${API_URL}health"
echo "  Get All Files:    GET  ${API_URL}file"
echo "  Upload File:      POST ${API_URL}file"
echo "  Upload Multiple:  POST ${API_URL}file/multiple"
echo "  Get User Files:   GET  ${API_URL}file/user/{userId}"
echo "  Download File:    GET  ${API_URL}file/download/{userId}/{fileId}"
echo "  Delete All Files: DELETE ${API_URL}file"
