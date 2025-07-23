# JPQL Syntax Validation

This directory contains tools to validate JPQL (Java Persistence Query Language) syntax in Spring Data JPA repositories to prevent common syntax errors that can cause application startup failures.

## What This Catches

The validation tools catch common JPQL syntax errors that developers often make when coming from SQL or Java backgrounds:

### 1. Java-style Equality Operators
- ❌ **Wrong**: `WHERE user.id == :userId` 
- ✅ **Correct**: `WHERE user.id = :userId`
- **Why**: JPQL uses single `=` for equality, not double `==` like Java

### 2. Java-style Inequality Operators  
- ❌ **Wrong**: `WHERE user.id != :userId`
- ✅ **Correct**: `WHERE user.id <> :userId`
- **Why**: JPQL uses `<>` for inequality, not `!=` like Java

### 3. SQL-specific Keywords
- ❌ **Wrong**: `SELECT u FROM User u LIMIT 10`
- ✅ **Correct**: Use `Pageable` parameter or `setMaxResults()`
- **Why**: JPQL doesn't support `LIMIT`/`OFFSET` syntax directly

### 4. Parameter Consistency
- Validates that `@Param` declarations match the parameters used in queries
- Checks for potential unquoted string literals

## Tools Available

### 1. Local Validation Script
Run this before committing to catch issues early:

```bash
# From the project root directory
./scripts/validate-jpql-syntax.sh
```

**Sample Output:**
```
🔍 Validating JPQL syntax in repository files...
📁 Checking: src/main/java/.../IUserRepository.java
❌ ERROR: Found Java-style equality operator (==) in JPQL query
JPQL uses single = for equality comparison, not double ==
  Line: 23:    @Query("SELECT u FROM User u WHERE u.id == :userId")

📊 Validation Summary:
Repository files checked: 15
❌ Found 1 JPQL syntax issues
Please fix the issues above before committing
```

### 2. GitHub Actions Integration
The validation runs automatically on:
- All pull requests
- Pushes to main branch

**CI Pipeline Steps:**
1. **Static JPQL Validation** - Regex-based syntax checking
2. **Spring Boot Context Validation** - Actual Spring context loading to catch runtime errors
3. **Repository Layer Testing** - Runs any existing repository tests

## Setting Up Pre-commit Hook (Optional)

To automatically run validation before each commit:

```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
echo "Running JPQL syntax validation..."
./scripts/validate-jpql-syntax.sh
EOF

# Make it executable
chmod +x .git/hooks/pre-commit
```

## Common JPQL Syntax Reminders

| Java/SQL | JPQL | Example |
|----------|------|---------|
| `==` | `=` | `WHERE u.id = :id` |
| `!=` | `<>` | `WHERE u.status <> :status` |
| `LIMIT 10` | `Pageable` | Use method parameter `Pageable pageable` |
| `user_id` | `user.id` | Use entity property navigation |
| `SELECT *` | `SELECT u` | Must specify entity alias |

## Fixing Common Issues

### Issue: "Could not create query" with `==`
```java
// ❌ This will fail
@Query("SELECT u FROM User u WHERE u.id == :userId")

// ✅ Fix: Use single =
@Query("SELECT u FROM User u WHERE u.id = :userId")
```

### Issue: "Could not create query" with `!=`
```java
// ❌ This will fail  
@Query("SELECT u FROM User u WHERE u.status != :status")

// ✅ Fix: Use <>
@Query("SELECT u FROM User u WHERE u.status <> :status")
```

### Issue: LIMIT not supported
```java
// ❌ This will fail
@Query("SELECT u FROM User u ORDER BY u.createdDate DESC LIMIT 10")

// ✅ Fix: Use Pageable
@Query("SELECT u FROM User u ORDER BY u.createdDate DESC")
List<User> findRecentUsers(Pageable pageable);

// Usage: findRecentUsers(PageRequest.of(0, 10))
```

## Integration with IDE

### IntelliJ IDEA
1. Go to Settings > Tools > External Tools
2. Add new tool:
   - Name: "Validate JPQL"
   - Program: `./scripts/validate-jpql-syntax.sh`
   - Working directory: `$ProjectFileDir$`

### VS Code  
Add to `.vscode/tasks.json`:
```json
{
    "label": "Validate JPQL Syntax",
    "type": "shell", 
    "command": "./scripts/validate-jpql-syntax.sh",
    "group": "build",
    "presentation": {
        "echo": true,
        "reveal": "always",
        "focus": false,
        "panel": "shared"
    }
}
```

## Troubleshooting

### Script Permission Issues
```bash
chmod +x scripts/validate-jpql-syntax.sh
```

### False Positives
If the script flags valid JPQL, you can:
1. Check if it's actually a syntax error
2. Modify the regex patterns in the script if needed
3. Report the issue for script improvement

### CI/CD Failures
If GitHub Actions fail on JPQL validation:
1. Run the script locally: `./scripts/validate-jpql-syntax.sh`
2. Fix the reported issues
3. Push the fixes

The validation catches these errors early to prevent Spring Boot startup failures in production. 