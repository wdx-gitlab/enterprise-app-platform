# DB Migration Directory Guide

## Purpose

This directory stores SQL migration assets for multiple modules that were developed in parallel during the MVP stage.

Current rule:
- Keep SQL files separated by module ownership.
- Because this directory is one shared Flyway location, version numbers must be globally unique and monotonic across all modules.
- If the database will be re-initialized from scratch, development-stage repair scripts can be folded back into the latest clean baseline.
- Once a Flyway version has been applied to a shared database that must be preserved, do not edit that version again. Add a new versioned repair or upgrade script instead.

## Current module split

| Module | File | Role | Status |
| --- | --- | --- | --- |
| DAP Engine | `V1__dap_engine_schema.sql` | DAP system table baseline | Independent module baseline |
| DAP Engine | `V2__dap_engine_baseline_data.sql` | DAP built-in subject and metadata seed | Independent module baseline data |
| Authz Engine | `V3__authz_engine_schema.sql` | Authz schema baseline | Independent module baseline |
| Authz Engine | `V4__authz_engine_baseline_data.sql` | Authz baseline data | Independent module baseline data |
| USP Portal | `V5__usp_portal_mvp.sql` | Consolidated USP portal MVP schema and seed | Current USP clean baseline |

Current consolidation result:
- The former Authz filenames with `(1)` have been normalized.
- Duplicate `V1` / `V2` prefixes across modules were reordered into one global chain.
- The former `V4__repair_login_config_seed.sql` has been merged into `V5__usp_portal_mvp.sql` because the database will be rebuilt from initialization scripts.

## Consolidation rule for iterative SQL changes

### 1. Development-only iteration phase

If a module is still in local or disposable-environment iteration, repeated table/seed adjustments should be merged back into that module's latest clean baseline.

Example:
- Several temporary USP login seed tweaks during MVP iteration were folded back into the clean initialization baseline because the database will be re-created from scratch.

### 2. Shared-environment phase

If a version has already been applied in a shared environment, follow Flyway rules strictly:
- Keep the old version unchanged.
- Add a new versioned script for repair.
- Typical cases: seed correction, column backfill, default value adjustment, data cleanup, compatibility repair.

Example:
- If a future production or shared test database has already executed `V5__usp_portal_mvp.sql`, then any later correction must be delivered as `V6__...` or a later version instead of editing `V5` in place.

## Recommended interpretation of the current file set

### DAP Engine

`V1__dap_engine_schema.sql` and `V2__dap_engine_baseline_data.sql` should be treated as one DAP baseline chain:
- `V1`: schema
- `V2`: baseline data

### Authz Engine

`V3__authz_engine_schema.sql` and `V4__authz_engine_baseline_data.sql` should be treated as one Authz baseline chain:
- `V3`: schema
- `V4`: baseline data

### USP Portal

`V5__usp_portal_mvp.sql` is the current USP Portal clean baseline:
- `V5`: consolidated clean MVP baseline, already merged with the former login-config repair changes

## Practical maintenance rule

When you need to change SQL in the future, first decide which case you are in:

1. Module is still only in local iteration and the database can be rebuilt from scratch.
   Action: merge the iteration into that module's clean baseline file, while preserving the global Flyway version order of this directory.

2. Module version has already been applied in a shared DB that must be preserved.
   Action: keep the existing script unchanged and add a new incremental version file with the next global version number.

## Naming guidance

For future scripts, keep the description explicit about the owning module and intent.

Examples:
- `V6__usp_portal_add_menu_indexes.sql`
- `V7__usp_portal_backfill_app_context.sql`
- `V8__dap_engine_add_sync_checkpoint.sql`
- `V9__authz_engine_add_policy_template.sql`

## Important note about the current directory state

The current folder still mixes scripts from different modules because they are executed from one shared Flyway location, but the naming has now been normalized into a single ordered chain.

If later the project decides to fully separate migration execution by module, the next step should be:
- split module-specific Flyway locations;
- move DAP/Authz/USP scripts into dedicated subdirectories or dedicated module resources;
- keep this README as the top-level index.
