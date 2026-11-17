# Workflow V1 → V2 migration (SQL only)

Use **[V1-TO-V2-MIGRATION.sql](V1-TO-V2-MIGRATION.sql)** — no Java migrator required.

## Requirements

- MySQL 8+ or MariaDB 10.4+ (`ROW_NUMBER`, user variables)
- Backup database before running sections 3–4
- V1 workflows must have at least one action row

## What the script does

1. **Pre-flight** — counts and orphan checks  
2. **Map tables** — `map_workflow_v1_v2`, `map_node_v1`, `map_action_v1_v2`, `map_history_v1_v2`, etc.  
3. **Definitions** — `OMWFLWorkflowV2` (same ID as V1), published `OMWFLVersion`, graph nodes/edges, criteria V2, `OMWFLActionsV2`  
4. **History** — `OMWFLEXECUTIONV2` (same ID as V1 history), `OMWFLExecutionNode`  
5. **Validation** — row-count parity queries  
6. **Rollback** — commented templates  

## ID strategy

| Entity | ID rule |
|--------|---------|
| Workflow V2 | Same as V1 `OMWFLWorkflow.ID` |
| Version / nodes / edges / criteria / actions | `MAX(existing ID) + ROW_NUMBER()` |
| Execution V2 | Same as V1 `OMWFLExecutionHistory.ID` |

After migration, **advance application sequence generators** for each affected table to at least `MAX(ID)` (your DBA / platform tooling).

## Pilot

Uncomment and set `@pilot_org_id` in section 3.1 to migrate one org first.

## Assumptions

- `TRIGGER_SCOPE = 2` (EVERYONE)  
- Condition node only when V1 has at least one criteria row  
- Execution `USER_ID` = V1 workflow `OWNER_ID`  
- V1 status `3` (FAILURE) → V2 execution status `5` (FAILED)  
