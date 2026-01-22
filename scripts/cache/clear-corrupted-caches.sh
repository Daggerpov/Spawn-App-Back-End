#!/bin/bash

# Script to clear all Redis caches that may be corrupted from the old JDK serialization format
# This script should be run after deploying the JSON serialization fix to Redis cache

echo "=========================================="
echo "Clear Corrupted Redis Caches"
echo "=========================================="
echo ""
echo "This script will clear all caches that may contain corrupted data"
echo "from the old JDK serialization format."
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

# List of cache patterns to clear (based on the errors in the logs)
CACHE_PATTERNS=(
    "spawn:activityTypesByUserId:*"
    "spawn:incomingFetchFriendRequests:*"
    "spawn:sentFetchFriendRequests:*"
    "spawn:friendsByUserId:*"
    "spawn:recommendedFriends:*"
    "spawn:userStats:*"
    "spawn:userInterests:*"
    "spawn:userSocialMedia:*"
    "spawn:calendarActivitiesByUserId:*"
    "spawn:recentActivitiesByUserId:*"
    "spawn:upcomingActivitiesByUserId:*"
)

echo "Scanning for cache entries to clear..."
echo ""

TOTAL_COUNT=0
for pattern in "${CACHE_PATTERNS[@]}"; do
    COUNT=$($REDIS_CMD --scan --pattern "$pattern" 2>/dev/null | wc -l)
    if [ "$COUNT" -gt 0 ]; then
        echo "  Found $COUNT entries for pattern: $pattern"
        TOTAL_COUNT=$((TOTAL_COUNT + COUNT))
    fi
done

echo ""
echo "Total cache entries found: $TOTAL_COUNT"

if [ "$TOTAL_COUNT" -eq 0 ]; then
    echo "No cache entries to delete"
    exit 0
fi

# Ask for confirmation
echo ""
read -p "Do you want to delete all $TOTAL_COUNT cache entries? (y/n): " CONFIRM
if [ "$CONFIRM" != "y" ]; then
    echo "Aborted"
    exit 0
fi

echo ""
echo "Deleting cache entries..."
echo ""

DELETED_COUNT=0
for pattern in "${CACHE_PATTERNS[@]}"; do
    PATTERN_COUNT=$($REDIS_CMD --scan --pattern "$pattern" 2>/dev/null | wc -l)
    if [ "$PATTERN_COUNT" -gt 0 ]; then
        echo "  Deleting $PATTERN_COUNT entries for pattern: $pattern"
        PATTERN_DELETED=$($REDIS_CMD --scan --pattern "$pattern" | xargs -I{} $REDIS_CMD del {} 2>/dev/null | grep -c "1" || echo "0")
        DELETED_COUNT=$((DELETED_COUNT + PATTERN_DELETED))
        echo "    ✓ Deleted $PATTERN_DELETED entries"
    fi
done

echo ""
echo "=========================================="
echo "Cache Clear Complete"
echo "=========================================="
echo ""
echo "✓ Successfully deleted $DELETED_COUNT cache entries"
echo ""
echo "Notes:"
echo "  - The cache will be automatically rebuilt from the database when needed"
echo "  - The updated Redis configuration should prevent encoding issues going forward"
echo "  - Auto-recovery is now enabled for JSON deserialization errors"
echo ""
echo "If you continue to see errors, check the application logs for details."
echo ""

