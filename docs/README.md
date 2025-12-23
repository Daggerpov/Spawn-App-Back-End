# Spawn App Back-End Documentation

This directory contains comprehensive documentation for the Spawn App back-end codebase, organized by category.

## 📁 Directory Structure

### 🚀 [optimization/](optimization/)
Performance and memory optimization documentation:
- **[RAM_OPTIMIZATION_STRATEGIES.md](optimization/RAM_OPTIMIZATION_STRATEGIES.md)** - Comprehensive RAM optimization guide with JVM tuning, connection pooling, and caching strategies
- **[RAM_OPTIMIZATION_README.md](optimization/RAM_OPTIMIZATION_README.md)** - Overview of RAM optimization implementation
- **[RAM_OPTIMIZATION_DELIVERABLES.md](optimization/RAM_OPTIMIZATION_DELIVERABLES.md)** - Deliverables and status tracking
- **[RAM_OPTIMIZATION_IMPACT_SUMMARY.md](optimization/RAM_OPTIMIZATION_IMPACT_SUMMARY.md)** - Measured impact of optimizations
- **[QUICK_START_RAM_OPTIMIZATION.md](optimization/QUICK_START_RAM_OPTIMIZATION.md)** - Quick start guide for RAM optimizations
- **[PERFORMANCE_OPTIMIZATION_SUMMARY.md](optimization/PERFORMANCE_OPTIMIZATION_SUMMARY.md)** - Overall performance improvements
- **[MEMORY_LEAK_AND_PERFORMANCE_FIXES.md](optimization/MEMORY_LEAK_AND_PERFORMANCE_FIXES.md)** - Memory leak fixes and performance improvements (marking classes as final)

### ✨ [features/](features/)
Feature implementation documentation:
- **[MOBILE_CACHE_IMPLEMENTATION.md](features/MOBILE_CACHE_IMPLEMENTATION.md)** - Mobile cache validation system
- **[FUZZY_SEARCH_IMPLEMENTATION.md](features/FUZZY_SEARCH_IMPLEMENTATION.md)** - Fuzzy search with Jaro-Winkler algorithm
- **[FRIENDSHIP_REFACTOR_PLAN.md](features/FRIENDSHIP_REFACTOR_PLAN.md)** - Friendship system refactoring plan

### 🔧 [fixes/](fixes/)
Bug fixes and issue resolutions:
- **[AUTH_FLOW_FIXES_SUMMARY.md](fixes/AUTH_FLOW_FIXES_SUMMARY.md)** - Authentication flow fixes
- **[OAUTH_CONCURRENCY_FIX_SUMMARY.md](fixes/OAUTH_CONCURRENCY_FIX_SUMMARY.md)** - OAuth concurrency issue resolution
- **[PHONE_NUMBER_MATCHING_FIX.md](fixes/PHONE_NUMBER_MATCHING_FIX.md)** - Phone number matching improvements
- **[REAPPLIED_FIXES_SUMMARY.md](fixes/REAPPLIED_FIXES_SUMMARY.md)** - Summary of re-applied memory leak and performance fixes
- **[DEPENDENCY_FIXES_SUMMARY.md](fixes/DEPENDENCY_FIXES_SUMMARY.md)** - Dependency resolution and fixes
- **[TEST_FIXES_SUMMARY.md](fixes/TEST_FIXES_SUMMARY.md)** - Test suite fixes and improvements

### 🔄 [refactoring/](refactoring/)
Code refactoring and architectural improvements:

**✅ Current Status: Spring Modulith Phase 1 Complete, Phase 2 In Progress**

- **[CURRENT_STATUS.md](refactoring/CURRENT_STATUS.md)** - 📊 **START HERE** - Current progress dashboard with next steps and phase breakdown
- **[PHASE_1_COMPLETE.md](refactoring/PHASE_1_COMPLETE.md)** - ✅ Phase 1 completion summary - All 266 files moved to modular structure, build successful (Dec 8, 2025)
- **[SPRING_MODULITH_REFACTORING_PLAN.md](refactoring/SPRING_MODULITH_REFACTORING_PLAN.md)** - 🔄 **Active Implementation** - Phases 2-6 detailed instructions (fix circular dependencies, add Spring Modulith, testing)
- **[REFACTORING_ORDER_DECISION.md](refactoring/REFACTORING_ORDER_DECISION.md)** - Decision rationale: Modulith first, then Mediator, then Microservices
- **[WHY_SPRING_MODULITH_FIRST.md](refactoring/WHY_SPRING_MODULITH_FIRST.md)** - **RECOMMENDED READ** - Why Spring Modulith is an effective first step before microservices, with detailed analysis of current codebase issues
- **[DRY_REFACTORING_ANALYSIS.md](refactoring/DRY_REFACTORING_ANALYSIS.md)** - DRY principle analysis
- **[BUGS_FIXED_SUMMARY.md](refactoring/BUGS_FIXED_SUMMARY.md)** - Summary of bugs fixed during refactoring

### 🗄️ [database/](database/)
Database-related documentation:
- **[DATABASE_INDEXES.md](database/DATABASE_INDEXES.md)** - Database indexing strategy and implementation
- **[migrations/](database/migrations/)** - Database migration documentation
  - `README-name-migration.md` - Name field migration guide

> **Note:** Database scripts and SQL files are located in `../scripts/database/`

### 🎨 [diagrams/](diagrams/)
Architecture diagrams and visual documentation:
- **[entity-relationship-diagram.png](diagrams/entity-relationship-diagram.png)** - ER diagram visualization
- **[entity-relationship-diagram.md](diagrams/entity-relationship-diagram.md)** - ER diagram documentation
- **[entity-relationship-diagram.dbml](diagrams/entity-relationship-diagram.dbml)** - DBML source file
- **[entity-relationship-diagram-updated.dbml](diagrams/entity-relationship-diagram-updated.dbml)** - Updated DBML source
- **[diagrams-architecture-dependency-injection-dtos.png](diagrams/diagrams-architecture-dependency-injection-dtos.png)** - Architecture diagram
- **[user-dto-relationships.png](diagrams/user-dto-relationships.png)** - User DTO relationships
- **[diagrams.excalidraw](diagrams/diagrams.excalidraw)** - Editable Excalidraw diagram source
- **[ENTITIES_SUMMARY.md](diagrams/ENTITIES_SUMMARY.md)** - Summary of entity relationships

### 🏗️ [microservices/](microservices/)
Microservices architecture planning and implementation:
- **[README.md](microservices/README.md)** - **START HERE** - Quick navigation guide with decision flow diagram and current recommendation summary
- **[MICROSERVICES_DECISION_GUIDE.md](microservices/MICROSERVICES_DECISION_GUIDE.md)** - Complete benefits/drawbacks analysis, cost implications, and decision framework with modular monolith alternative
- **[MICROSERVICES_ARCHITECTURE.md](microservices/MICROSERVICES_ARCHITECTURE.md)** - Comprehensive microservices decomposition strategy with service boundaries and data architecture
- **[MICROSERVICES_IMPLEMENTATION_PLAN.md](microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md)** - Step-by-step implementation plan for Railway platform with detailed tasks and timelines
- **[VISUAL_ARCHITECTURE.md](microservices/VISUAL_ARCHITECTURE.md)** - Visual diagrams and ASCII art for quick reference

### 📋 Root Level
- **[TESTS_STATUS.md](TESTS_STATUS.md)** - Test suite status and coverage tracking
- **README.md** (this file) - Documentation index and navigation guide

---

## 🎯 Quick Navigation

### For Performance Optimization
Start with [optimization/QUICK_START_RAM_OPTIMIZATION.md](optimization/QUICK_START_RAM_OPTIMIZATION.md)

### For New Features
Browse [features/](features/) directory

### For Bug Fixes
Check [fixes/](fixes/) directory

### For Database Issues
Review [database/](database/) directory

### For Microservices Decision
**Current Progress: Spring Modulith Phase 1 Complete ✅**

1. ✅ **DONE**: Phase 1 Package Restructuring - See [refactoring/PHASE_1_COMPLETE.md](refactoring/PHASE_1_COMPLETE.md)
2. 🔄 **CURRENT**: Phase 2 Fix Circular Dependencies - Follow [refactoring/SPRING_MODULITH_REFACTORING_PLAN.md](refactoring/SPRING_MODULITH_REFACTORING_PLAN.md) Phase 2 section
3. **NEXT**: Complete Phases 3-6 of Spring Modulith refactoring (4-5 more weeks)
4. **FUTURE**: Proceed to [microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md](microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md) after Modulith validation

**Background Reading:**
- [refactoring/WHY_SPRING_MODULITH_FIRST.md](refactoring/WHY_SPRING_MODULITH_FIRST.md) - Why this approach
- [refactoring/REFACTORING_ORDER_DECISION.md](refactoring/REFACTORING_ORDER_DECISION.md) - Decision rationale

### For Code Refactoring
**✅ Phase 1 Complete!** Continue with Phase 2 in [refactoring/SPRING_MODULITH_REFACTORING_PLAN.md](refactoring/SPRING_MODULITH_REFACTORING_PLAN.md)

---

## 📝 Documentation Standards

When adding new documentation:
1. **optimization/** - RAM, CPU, network, database performance improvements
2. **features/** - New feature implementations and architecture
3. **fixes/** - Bug fixes, hotfixes, and issue resolutions
4. **refactoring/** - Code refactoring, architectural improvements, and design patterns
5. **database/** - Schema changes, migrations, and database-specific issues
6. **microservices/** - Microservices architecture, decomposition, and implementation plans
7. **Root level** - Only high-level overview and status documents

---

## 🔄 Recent Updates

- **December 23, 2025**: ✅ **Spring Modulith Phase 1 COMPLETE** - All 266 files moved to modular structure, compilation successful, Phase 2 in progress
- **December 8, 2025**: Started Spring Modulith refactoring - Phase 1 package restructuring
- **December 8, 2025**: Added comprehensive Spring Modulith documentation (refactoring plan, rationale, decision guide)
- **December 10, 2025**: Major folder structure reorganization - moved all bash scripts to organized subdirectories in `scripts/`, moved diagrams to `docs/diagrams/`, consolidated fix summaries
- **November 9, 2025**: Added comprehensive microservices decision guide with cost analysis and modular monolith alternative
- **November 9, 2025**: Added microservices architecture and implementation documentation
- **October 31, 2025**: Reorganized docs folder into logical categories
- **October 31, 2025**: Added memory leak fixes and performance improvements
- **October 2025**: Implemented RAM optimization strategies
- **October 2025**: Added mobile cache validation system

---

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [diagrams/](diagrams/) - Architecture and entity relationship diagrams
- [scripts/](../scripts/) - Utility scripts for deployment and maintenance

---

## 🤝 Contributing

When contributing documentation:
1. Place files in the appropriate category folder
2. Use descriptive filenames in UPPER_SNAKE_CASE for consistency
3. Include a summary at the top of each document
4. Update this README.md to link to new documents
5. Use Markdown formatting for readability

---

**Last Updated:** December 23, 2025
