#!/bin/bash

# Script to clear activity type cache from Redis
# This is useful when there are corrupted cache entries due to encoding issues
# Run this script to clear all activity type cache entries

echo "=========================================="
echo "Clear Activity Type Cache from Redis"
echo "=========================================="
echo ""

# Check if redis-cli is available
if ! command -v redis-cli &> /dev/null; then
    echo "Error: redis-cli is not installed or not in PATH"
    echo "Please install redis-cli to run this script"
    exit 1
fi

# Prompt for Redis connection details
read -p "Enter Redis host (default: localhost): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}

read -p "Enter Redis port (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -p "Enter Redis password (leave empty if none): " -s REDIS_PASSWORD
echo ""

# Build redis-cli command
REDIS_CMD="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
if [ ! -z "$REDIS_PASSWORD" ]; then
    REDIS_CMD="$REDIS_CMD -a $REDIS_PASSWORD"
fi

echo ""
echo "Connecting to Redis at $REDIS_HOST:$REDIS_PORT..."

# Test connection
if ! $REDIS_CMD ping > /dev/null 2>&1; then
    echo "Error: Could not connect to Redis"
    exit 1
fi

echo "✓ Connected to Redis successfully"
echo ""

# Count cache entries before deletion
CACHE_COUNT=$($REDIS_CMD --scan --pattern "spawn:activityTypesByUserId:*" | wc -l)
echo "Found $CACHE_COUNT cache entries for activityTypesByUserId"

if [ "$CACHE_COUNT" -eq 0 ]; then
    echo "No cache entries to delete"
    exit 0
fi

# Ask for confirmation
read -p "Do you want to delete all $CACHE_COUNT cache entries? (y/n): " CONFIRM
if [ "$CONFIRM" != "y" ]; then
    echo "Aborted"
    exit 0
fi

echo ""
echo "Deleting cache entries..."

# Delete all matching keys
DELETED_COUNT=$($REDIS_CMD --scan --pattern "spawn:activityTypesByUserId:*" | xargs -L 1 $REDIS_CMD del | grep -c "1")

echo "✓ Successfully deleted $DELETED_COUNT cache entries"
echo ""
echo "The cache will be automatically rebuilt from the database when needed."
echo "The updated Redis configuration should prevent encoding issues going forward."
echo ""
echo "=========================================="
echo "Cache Clear Complete"
echo "=========================================="

