#!/bin/bash

# JPQL Syntax Validation Script
# This script validates JPQL queries in repository files to catch common syntax errors

echo "🔍 Validating JPQL syntax in repository files..."

# Set colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Initialize error counter
errors=0

# Function to check for JPQL syntax errors
check_jpql_syntax() {
    local file="$1"
    echo "📁 Checking: $file"
    
    # Check for Java-style equality operators (== instead of =)
    if grep -n "@Query.*==" "$file" >/dev/null 2>&1; then
        echo -e "${RED}❌ ERROR: Found Java-style equality operator (==) in JPQL query${NC}"
        echo -e "${YELLOW}JPQL uses single = for equality comparison, not double ==${NC}"
        grep -n "@Query.*==" "$file" | while read -r line; do
            echo "  Line: $line"
        done
        ((errors++))
    fi
    
    # Check for Java-style inequality operators (!= instead of <>)
    if grep -n "@Query.*!=" "$file" >/dev/null 2>&1; then
        echo -e "${RED}❌ ERROR: Found Java-style inequality operator (!=) in JPQL query${NC}"
        echo -e "${YELLOW}JPQL uses <> for inequality comparison, not !=${NC}"
        grep -n "@Query.*!=" "$file" | while read -r line; do
            echo "  Line: $line"
        done
        ((errors++))
    fi
    
    # Check for SQL keywords used incorrectly in JPQL
    if grep -n "@Query.*LIMIT\|@Query.*OFFSET" "$file" >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  WARNING: Found SQL LIMIT/OFFSET in JPQL query${NC}"
        echo -e "${YELLOW}Consider using Pageable parameter or setMaxResults() instead${NC}"
        grep -n "@Query.*LIMIT\|@Query.*OFFSET" "$file"
        ((errors++))
    fi
    
    # Check for missing semicolons in multi-statement queries
    if grep -n "@Query.*;\s*[A-Z]" "$file" >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  WARNING: Multiple statements detected in single @Query${NC}"
        echo -e "${YELLOW}Consider splitting into separate methods${NC}"
        grep -n "@Query.*;\s*[A-Z]" "$file"
    fi
    
    # Check for unquoted string literals that might cause issues
    if grep -n "@Query.*=\s*[a-zA-Z]" "$file" | grep -v ":" >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  WARNING: Possible unquoted string literal in JPQL${NC}"
        echo -e "${YELLOW}String literals should be quoted or use parameters${NC}"
        grep -n "@Query.*=\s*[a-zA-Z]" "$file" | grep -v ":"
    fi
}

# Find all repository files and check them
if [ -d "src/main/java" ]; then
    repository_files=$(find src/main/java -name "*Repository*.java" 2>/dev/null)
    
    if [ -z "$repository_files" ]; then
        echo -e "${YELLOW}⚠️  No repository files found${NC}"
        exit 0
    fi
    
    for file in $repository_files; do
        check_jpql_syntax "$file"
    done
else
    echo -e "${RED}❌ ERROR: src/main/java directory not found${NC}"
    echo "Make sure you're running this script from the project root"
    exit 1
fi

# Report results
echo ""
echo "📊 Validation Summary:"
echo "Repository files checked: $(echo "$repository_files" | wc -l)"

if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✅ All JPQL syntax checks passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Found $errors JPQL syntax issues${NC}"
    echo -e "${YELLOW}Please fix the issues above before committing${NC}"
    exit 1
fi 