#!/bin/bash

# Setup script for S3 deployment bucket
DEPLOYMENT_BUCKET=${1:-file-handler-deployment-bucket}

echo "Setting up deployment bucket: $DEPLOYMENT_BUCKET"

# Create bucket with proper permissions
aws s3 mb s3://$DEPLOYMENT_BUCKET

# Set bucket policy for deployment access
cat > bucket-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):root"
            },
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": "arn:aws:s3:::$DEPLOYMENT_BUCKET/*"
        }
    ]
}
EOF

aws s3api put-bucket-policy --bucket $DEPLOYMENT_BUCKET --policy file://bucket-policy.json

# Clean up
rm bucket-policy.json

echo "Deployment bucket setup complete: $DEPLOYMENT_BUCKET"
echo "You can now run: ./deploy.sh dev file-handler-bucket $DEPLOYMENT_BUCKET"