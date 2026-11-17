-- Workflow V1 → V2 migration (PostgreSQL)
-- Port of V1-TO-V2-MIGRATION.sql for local PostgreSQL (quoted identifiers, boolean IS_DELETED).
-- Queries are separated by: -- *****

-- -----------------------------------------------------------------------------
-- 1) Pre-flight
-- -----------------------------------------------------------------------------


-- *****

SELECT COUNT(*) AS v1_active_workflows FROM omwflworkflow WHERE "IS_DELETED" = false;


-- *****

SELECT COUNT(*) AS v1_workflows_without_actions
FROM omwflworkflow w
WHERE w."IS_DELETED" = false
  AND NOT EXISTS (SELECT 1 FROM omwflactions a WHERE a."WORKFLOW_ID" = w."ID");


-- *****

SELECT COUNT(*) AS v1_in_progress_histories
FROM omwflexecutionhistory WHERE "STATUS" = 1;


-- -----------------------------------------------------------------------------
-- 2) Mapping / staging tables
-- -----------------------------------------------------------------------------


-- *****

CREATE TABLE IF NOT EXISTS map_workflow_v1_v2 (
  v1_workflow_id BIGINT PRIMARY KEY,
  v2_workflow_id BIGINT NOT NULL,
  v2_version_id  BIGINT NOT NULL,
  org_id         BIGINT NOT NULL,
  migrated_at    TIMESTAMP NOT NULL
);


-- *****

CREATE TABLE IF NOT EXISTS map_node_v1 (
  v1_workflow_id BIGINT NOT NULL,
  client_key     VARCHAR(50) NOT NULL,
  v2_node_id     BIGINT NOT NULL,
  node_type      SMALLINT NOT NULL,
  PRIMARY KEY (v1_workflow_id, client_key)
);


-- *****

CREATE TABLE IF NOT EXISTS map_action_v1_v2 (
  v1_action_id   BIGINT PRIMARY KEY,
  v1_workflow_id BIGINT NOT NULL,
  v2_node_id     BIGINT NOT NULL,
  v2_action_id   BIGINT NOT NULL,
  org_id         BIGINT NOT NULL
);


-- *****

CREATE TABLE IF NOT EXISTS map_criteria_set_v1_v2 (v1_set_id BIGINT PRIMARY KEY, v2_set_id BIGINT NOT NULL);


-- *****

CREATE TABLE IF NOT EXISTS map_criteria_v1_v2 (v1_criteria_id BIGINT PRIMARY KEY, v2_criteria_id BIGINT NOT NULL);


-- *****

CREATE TABLE IF NOT EXISTS map_history_v1_v2 (
  v1_history_id   BIGINT PRIMARY KEY,
  v2_execution_id BIGINT NOT NULL,
  org_id          BIGINT NOT NULL
);


-- -----------------------------------------------------------------------------
-- 3) Definition migration
-- -----------------------------------------------------------------------------


-- *****

BEGIN;


-- *****

DROP TABLE IF EXISTS mig_wf;


-- *****

CREATE TEMP TABLE mig_wf AS
SELECT
  w."ID" AS v1_workflow_id,
  w."ID" AS v2_workflow_id,
  w."ORG_ID",
  w."OWNER_ID",
  w."NAME",
  w."DESCRIPTION",
  w."TEMPLATE_ID",
  w."TRIGGER_SOURCE",
  w."TRIGGER_EVENT",
  w."IS_ACTIVE",
  w."CREATED_TIME",
  w."MODIFIED_TIME",
  w."IS_DELETED",
  CASE
    WHEN EXISTS (
      SELECT 1 FROM omwflcriteriaset cs
      INNER JOIN omwflcriteria c ON c."SET_ID" = cs."ID"
      WHERE cs."WORKFLOW_ID" = w."ID"
    ) THEN 1 ELSE 0
  END AS has_condition
FROM omwflworkflow w
WHERE w."IS_DELETED" = false
  AND EXISTS (SELECT 1 FROM omwflactions a WHERE a."WORKFLOW_ID" = w."ID")
  AND NOT EXISTS (SELECT 1 FROM omwflworkflowv2 v2 WHERE v2."ID" = w."ID")
  AND NOT EXISTS (SELECT 1 FROM map_workflow_v1_v2 m WHERE m.v1_workflow_id = w."ID");


-- *****

INSERT INTO omwflworkflowv2 (
  "ID", "ORG_ID", "OWNER_ID", "NAME", "DESCRIPTION", "TRIGGER_SCOPE",
  "TRIGGER_SOURCE", "TRIGGER_EVENT", "TEMPLATE_ID", "IS_ACTIVE",
  "CREATED_TIME", "MODIFIED_TIME", "IS_DELETED"
)
SELECT
  v2_workflow_id, "ORG_ID", "OWNER_ID", "NAME", "DESCRIPTION",
  2,
  "TRIGGER_SOURCE", "TRIGGER_EVENT", "TEMPLATE_ID", "IS_ACTIVE",
  "CREATED_TIME", "MODIFIED_TIME", "IS_DELETED"
FROM mig_wf;


-- *****

DROP TABLE IF EXISTS mig_version;


-- *****

CREATE TEMP TABLE mig_version AS
WITH ver_base AS (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflversion)
SELECT
  ver_base.b + ROW_NUMBER() OVER (ORDER BY v1_workflow_id) AS version_id,
  v1_workflow_id,
  v2_workflow_id,
  "ORG_ID",
  COALESCE("MODIFIED_TIME", "CREATED_TIME") AS published_time
FROM mig_wf
CROSS JOIN ver_base;


-- *****

INSERT INTO omwflversion ("ID", "ORG_ID", "WORKFLOW_ID", "VERSION_NUMBER", "STATUS", "INITIAL_NODE_ID", "PUBLISHED_TIME")
SELECT version_id, "ORG_ID", v2_workflow_id, 1, 1, 1, published_time
FROM mig_version;


-- *****

INSERT INTO map_workflow_v1_v2 (v1_workflow_id, v2_workflow_id, v2_version_id, org_id, migrated_at)
SELECT v1_workflow_id, v2_workflow_id, version_id, "ORG_ID", NOW()
FROM mig_version;


-- *****

DROP TABLE IF EXISTS mig_node_plan;


-- *****

CREATE TEMP TABLE mig_node_plan AS
SELECT v.v1_workflow_id, v.version_id, v."ORG_ID", '1' AS client_key, 1 AS node_type, 1 AS seq, NULL::bigint AS action_ord
FROM mig_version v
UNION ALL
SELECT v.v1_workflow_id, v.version_id, v."ORG_ID", '2', 2, 2, NULL::bigint
FROM mig_version v
INNER JOIN mig_wf w ON w.v1_workflow_id = v.v1_workflow_id AND w.has_condition = 1
UNION ALL
SELECT
  v.v1_workflow_id,
  v.version_id,
  v."ORG_ID",
  (
    (CASE WHEN w.has_condition = 1 THEN 2 ELSE 1 END)
      + ROW_NUMBER() OVER (PARTITION BY a."WORKFLOW_ID" ORDER BY a."ORDER")
  )::text AS client_key,
  5 AS node_type,
  (CASE WHEN w.has_condition = 1 THEN 2 ELSE 1 END)
    + ROW_NUMBER() OVER (PARTITION BY a."WORKFLOW_ID" ORDER BY a."ORDER") AS seq,
  ROW_NUMBER() OVER (PARTITION BY a."WORKFLOW_ID" ORDER BY a."ORDER") AS action_ord
FROM mig_version v
INNER JOIN mig_wf w ON w.v1_workflow_id = v.v1_workflow_id
INNER JOIN omwflactions a ON a."WORKFLOW_ID" = v.v1_workflow_id;


-- *****

DROP TABLE IF EXISTS mig_node;


-- *****

CREATE TEMP TABLE mig_node AS
WITH node_base AS (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflnode)
SELECT
  node_base.b + ROW_NUMBER() OVER (ORDER BY v1_workflow_id, seq) AS node_id,
  v1_workflow_id,
  version_id,
  "ORG_ID",
  client_key,
  node_type,
  seq,
  action_ord
FROM mig_node_plan
CROSS JOIN node_base;


-- *****

INSERT INTO omwflnode ("ID", "ORG_ID", "VERSION_ID", "CLIENT_KEY", "NODE_TYPE")
SELECT node_id, "ORG_ID", version_id, client_key, node_type
FROM mig_node;


-- *****

INSERT INTO map_node_v1 (v1_workflow_id, client_key, v2_node_id, node_type)
SELECT v1_workflow_id, client_key, node_id, node_type
FROM mig_node;


-- *****

DROP TABLE IF EXISTS mig_edge_pairs;


-- *****

CREATE TEMP TABLE mig_edge_pairs AS
SELECT n1.node_id AS from_node_id, n2.node_id AS to_node_id
FROM mig_node n1
INNER JOIN mig_node n2
  ON n1.v1_workflow_id = n2.v1_workflow_id AND n2.seq = n1.seq + 1;


-- *****

INSERT INTO mwflnodeedge ("ID", "FROM_NODE_ID", "TO_NODE_ID", "EDGE_TYPE")
SELECT
  edge_base.b + ROW_NUMBER() OVER (ORDER BY from_node_id),
  from_node_id,
  to_node_id,
  1
FROM mig_edge_pairs
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM mwflnodeedge) edge_base;


-- *****

INSERT INTO omwflnodeevent ("ID", "ORG_ID", "NODE_ID", "CONTEXT_TYPE", "SOURCE_NODE_ID")
SELECT
  ne_base.b + ROW_NUMBER() OVER (ORDER BY cond.node_id),
  cond."ORG_ID",
  cond.node_id,
  1,
  trig.node_id
FROM mig_node cond
INNER JOIN mig_node trig
  ON trig.v1_workflow_id = cond.v1_workflow_id AND trig.client_key = '1'
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflnodeevent) ne_base
WHERE cond.node_type = 2;


-- *****

DROP TABLE IF EXISTS mig_set_src;


-- *****

CREATE TEMP TABLE mig_set_src AS
SELECT
  cs."ID" AS v1_set_id,
  cs."ORG_ID",
  mn.node_id,
  cs."CONNECTOR_ID",
  cs."ORDER" AS set_order
FROM omwflcriteriaset cs
INNER JOIN mig_node mn ON mn.v1_workflow_id = cs."WORKFLOW_ID" AND mn.node_type = 2;


-- *****

DROP TABLE IF EXISTS mig_set_new;


-- *****

CREATE TEMP TABLE mig_set_new AS
WITH set_base AS (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflcriteriasetv2)
SELECT
  v1_set_id,
  set_base.b + ROW_NUMBER() OVER (ORDER BY v1_set_id) AS v2_set_id,
  "ORG_ID",
  node_id,
  "CONNECTOR_ID",
  set_order
FROM mig_set_src
CROSS JOIN set_base;


-- *****

INSERT INTO map_criteria_set_v1_v2 (v1_set_id, v2_set_id)
SELECT v1_set_id, v2_set_id FROM mig_set_new;


-- *****

INSERT INTO omwflcriteriasetv2 ("ID", "ORG_ID", "NODE_ID", "CONNECTOR_ID", "ORDER")
SELECT v2_set_id, "ORG_ID", node_id, "CONNECTOR_ID", set_order
FROM mig_set_new;


-- *****

DROP TABLE IF EXISTS mig_crit_src;


-- *****

CREATE TEMP TABLE mig_crit_src AS
SELECT
  c."ID" AS v1_criteria_id,
  c."ORG_ID",
  mn.node_id,
  sn.v2_set_id,
  c."COMPARATOR_ID",
  c."CONNECTOR_ID",
  c."ORDER" AS crit_order
FROM omwflcriteria c
INNER JOIN mig_set_new sn ON sn.v1_set_id = c."SET_ID"
INNER JOIN mig_node mn ON mn.v1_workflow_id = c."WORKFLOW_ID" AND mn.node_type = 2;


-- *****

DROP TABLE IF EXISTS mig_crit_new;


-- *****

CREATE TEMP TABLE mig_crit_new AS
WITH crit_base AS (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflcriteriav2)
SELECT
  v1_criteria_id,
  crit_base.b + ROW_NUMBER() OVER (ORDER BY v1_criteria_id) AS v2_criteria_id,
  "ORG_ID",
  node_id,
  v2_set_id,
  "COMPARATOR_ID",
  "CONNECTOR_ID",
  crit_order
FROM mig_crit_src
CROSS JOIN crit_base;


-- *****

INSERT INTO map_criteria_v1_v2 (v1_criteria_id, v2_criteria_id)
SELECT v1_criteria_id, v2_criteria_id FROM mig_crit_new;


-- *****

INSERT INTO omwflcriteriav2 ("ID", "ORG_ID", "NODE_ID", "SET_ID", "COMPARATOR_ID", "CONNECTOR_ID", "ORDER")
SELECT v2_criteria_id, "ORG_ID", node_id, v2_set_id, "COMPARATOR_ID", "CONNECTOR_ID", crit_order
FROM mig_crit_new;


-- *****

INSERT INTO mwflcriteriavaluev2 ("ID", "CRITERIA_ID", "VALUE_ID", "VALUE")
SELECT
  cv_base.b + ROW_NUMBER() OVER (ORDER BY cv."ID"),
  cn.v2_criteria_id,
  cv."VALUE_ID",
  cv."VALUE"
FROM mwflcriteriavalue cv
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = cv."CRITERIA_ID"
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM mwflcriteriavaluev2) cv_base;


-- *****

INSERT INTO mwflfieldcriteriav2 ("ID", "CRITERIA_ID", "FIELD_ID")
SELECT
  fc_base.b + ROW_NUMBER() OVER (ORDER BY fc."ID"),
  cn.v2_criteria_id,
  fc."FIELD_ID"
FROM mwflfieldcriteria fc
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = fc."CRITERIA_ID"
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM mwflfieldcriteriav2) fc_base;


-- *****

INSERT INTO mwflsourcefieldcriteriav2 ("ID", "CRITERIA_ID", "FIELD_ID")
SELECT
  sfc_base.b + ROW_NUMBER() OVER (ORDER BY sfc."ID"),
  cn.v2_criteria_id,
  sfc."FIELD_ID"
FROM mwflsourcefieldcriteria sfc
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = sfc."CRITERIA_ID"
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM mwflsourcefieldcriteriav2) sfc_base;


-- *****

DROP TABLE IF EXISTS mig_action_ord;


-- *****

CREATE TEMP TABLE mig_action_ord AS
SELECT
  "ID" AS v1_action_id,
  "WORKFLOW_ID" AS v1_workflow_id,
  "ORG_ID",
  "ACTION_SOURCE",
  "ACTION_EVENT",
  "FIELD_MAPPING",
  ROW_NUMBER() OVER (PARTITION BY "WORKFLOW_ID" ORDER BY "ORDER") AS action_ord
FROM omwflactions
WHERE "WORKFLOW_ID" IN (SELECT v1_workflow_id FROM mig_wf);


-- *****

DROP TABLE IF EXISTS mig_action_ins;


-- *****

CREATE TEMP TABLE mig_action_ins AS
WITH act_base AS (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflactionsv2)
SELECT
  ao.v1_action_id,
  ao.v1_workflow_id,
  ao."ORG_ID",
  mn.node_id,
  ao."ACTION_SOURCE",
  ao."ACTION_EVENT",
  ao."FIELD_MAPPING",
  act_base.b + ROW_NUMBER() OVER (ORDER BY ao.v1_action_id) AS v2_action_id
FROM mig_action_ord ao
INNER JOIN mig_node mn
  ON mn.v1_workflow_id = ao.v1_workflow_id AND mn.node_type = 5 AND mn.action_ord = ao.action_ord
CROSS JOIN act_base;


-- *****

INSERT INTO omwflactionsv2 ("ID", "ORG_ID", "NODE_ID", "ACTION_SOURCE", "ACTION_EVENT", "FIELD_MAPPING")
SELECT v2_action_id, "ORG_ID", node_id, "ACTION_SOURCE", "ACTION_EVENT", "FIELD_MAPPING"
FROM mig_action_ins;


-- *****

INSERT INTO map_action_v1_v2 (v1_action_id, v1_workflow_id, v2_node_id, v2_action_id, org_id)
SELECT v1_action_id, v1_workflow_id, node_id, v2_action_id, "ORG_ID"
FROM mig_action_ins;


-- *****

COMMIT;


-- -----------------------------------------------------------------------------
-- 4) Execution history migration
-- -----------------------------------------------------------------------------


-- *****

BEGIN;


-- *****

INSERT INTO omwflexecutionv2 (
  "ID", "ORG_ID", "USER_ID", "WORKFLOW_ID", "VERSION_ID", "STATUS",
  "NODE_CONTEXT_JSON", "STARTED_TIME", "COMPLETED_TIME"
)
SELECT
  h."ID",
  h."ORG_ID",
  w."OWNER_ID",
  m.v2_workflow_id,
  m.v2_version_id,
  CASE h."STATUS" WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 5 ELSE 1 END,
  h."WORKFLOW_STATE",
  h."WORKFLOW_EXECUTED_TIME",
  CASE
    WHEN h."STATUS" IN (2, 3) THEN GREATEST(
      h."WORKFLOW_EXECUTED_TIME",
      COALESCE(h."CRITERIA_EXECUTED_TIME", h."WORKFLOW_EXECUTED_TIME"),
      COALESCE((
        SELECT MAX(ah."ACTION_EXECUTED_TIME")
        FROM omwflactionexecutionhistory ah
        WHERE ah."EXECUTION_HISTORY_ID" = h."ID"
      ), h."WORKFLOW_EXECUTED_TIME")
    )
    ELSE NULL
  END
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
INNER JOIN omwflworkflow w ON w."ID" = m.v1_workflow_id
WHERE NOT EXISTS (SELECT 1 FROM map_history_v1_v2 x WHERE x.v1_history_id = h."ID")
  AND NOT EXISTS (SELECT 1 FROM omwflexecutionv2 e WHERE e."ID" = h."ID");


-- *****

INSERT INTO map_history_v1_v2 (v1_history_id, v2_execution_id, org_id)
SELECT h."ID", h."ID", h."ORG_ID"
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
WHERE EXISTS (SELECT 1 FROM omwflexecutionv2 e WHERE e."ID" = h."ID")
  AND NOT EXISTS (SELECT 1 FROM map_history_v1_v2 x WHERE x.v1_history_id = h."ID");


-- *****

INSERT INTO omwflexecutionnode ("ID", "ORG_ID", "EXECUTION_ID", "NODE_ID", "STATUS", "STARTED_TIME", "COMPLETED_TIME", "FAILURE_REASON")
SELECT
  en_base.b + ROW_NUMBER() OVER (ORDER BY h."ID"),
  h."ORG_ID",
  h."ID",
  mn.v2_node_id,
  2,
  h."WORKFLOW_EXECUTED_TIME",
  h."WORKFLOW_EXECUTED_TIME",
  NULL
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
INNER JOIN map_node_v1 mn ON mn.v1_workflow_id = m.v1_workflow_id AND mn.client_key = '1'
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflexecutionnode) en_base
WHERE EXISTS (SELECT 1 FROM omwflexecutionv2 e WHERE e."ID" = h."ID")
  AND NOT EXISTS (
    SELECT 1 FROM omwflexecutionnode en
    WHERE en."EXECUTION_ID" = h."ID" AND en."NODE_ID" = mn.v2_node_id
  );


-- *****

INSERT INTO omwflexecutionnode ("ID", "ORG_ID", "EXECUTION_ID", "NODE_ID", "STATUS", "STARTED_TIME", "COMPLETED_TIME", "FAILURE_REASON")
SELECT
  en_base.b + ROW_NUMBER() OVER (ORDER BY h."ID"),
  h."ORG_ID",
  h."ID",
  mn.v2_node_id,
  CASE WHEN h."CRITERIA_STATUS" = 2 THEN 3 ELSE 2 END,
  h."WORKFLOW_EXECUTED_TIME",
  COALESCE(h."CRITERIA_EXECUTED_TIME", h."WORKFLOW_EXECUTED_TIME"),
  NULL
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
INNER JOIN map_node_v1 mn ON mn.v1_workflow_id = m.v1_workflow_id AND mn.client_key = '2'
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflexecutionnode) en_base
WHERE h."CRITERIA_STATUS" IS NOT NULL
  AND EXISTS (SELECT 1 FROM omwflexecutionv2 e WHERE e."ID" = h."ID")
  AND NOT EXISTS (
    SELECT 1 FROM omwflexecutionnode en
    WHERE en."EXECUTION_ID" = h."ID" AND en."NODE_ID" = mn.v2_node_id
  );


-- *****

INSERT INTO omwflexecutionnode ("ID", "ORG_ID", "EXECUTION_ID", "NODE_ID", "STATUS", "STARTED_TIME", "COMPLETED_TIME", "FAILURE_REASON")
SELECT
  en_base.b + ROW_NUMBER() OVER (ORDER BY ah."ID"),
  ah."ORG_ID",
  ah."EXECUTION_HISTORY_ID",
  am.v2_node_id,
  CASE WHEN ah."STATUS" = 1 THEN 2 ELSE 3 END,
  ah."ACTION_EXECUTED_TIME",
  ah."ACTION_EXECUTED_TIME",
  ah."FAILURE_REASON"
FROM omwflactionexecutionhistory ah
INNER JOIN map_action_v1_v2 am ON am.v1_action_id = ah."ACTION_ID"
CROSS JOIN (SELECT COALESCE(MAX("ID"), 0) AS b FROM omwflexecutionnode) en_base
WHERE EXISTS (SELECT 1 FROM omwflexecutionv2 e WHERE e."ID" = ah."EXECUTION_HISTORY_ID")
  AND NOT EXISTS (
    SELECT 1 FROM omwflexecutionnode en
    WHERE en."EXECUTION_ID" = ah."EXECUTION_HISTORY_ID" AND en."NODE_ID" = am.v2_node_id
  );


-- *****

COMMIT;


-- -----------------------------------------------------------------------------
-- 5) Validation
-- -----------------------------------------------------------------------------


-- *****

SELECT
  (SELECT COUNT(*) FROM omwflworkflow WHERE "IS_DELETED" = false) AS v1_active_cnt,
  (SELECT COUNT(*) FROM map_workflow_v1_v2) AS migrated_workflow_cnt;


-- *****

SELECT m.v1_workflow_id,
       (SELECT COUNT(*) FROM omwflactions a WHERE a."WORKFLOW_ID" = m.v1_workflow_id) AS v1_actions,
       (SELECT COUNT(*) FROM omwflnode n
        INNER JOIN omwflversion v ON v."ID" = n."VERSION_ID"
        WHERE v."WORKFLOW_ID" = m.v2_workflow_id AND n."NODE_TYPE" = 5) AS v2_action_nodes
FROM map_workflow_v1_v2 m;


-- *****

-- History parity (migrated workflows only)
SELECT
  (SELECT COUNT(*) FROM omwflexecutionhistory h
   INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID") AS v1_hist_migrated_wf,
  (SELECT COUNT(*) FROM omwflexecutionv2 e
   INNER JOIN map_workflow_v1_v2 m ON m.v2_workflow_id = e."WORKFLOW_ID") AS v2_exec_cnt,
  (SELECT COUNT(*) FROM map_history_v1_v2) AS map_history_cnt;


-- *****

SELECT h."ID" AS v1_id, h."WORKFLOW_ID" AS v1_wf,
       e."WORKFLOW_ID" AS v2_wf, e."STATUS" AS v2_status,
       (e."ID" IS NOT NULL AND e."WORKFLOW_ID" = m.v2_workflow_id) AS ok
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
LEFT JOIN omwflexecutionv2 e ON e."ID" = h."ID"
ORDER BY h."ID";


-- *****

SELECT h."ID" AS history_id,
       (SELECT COUNT(*) FROM omwflactionexecutionhistory ah WHERE ah."EXECUTION_HISTORY_ID" = h."ID") AS v1_action_rows,
       (SELECT COUNT(*) FROM omwflexecutionnode en
        INNER JOIN omwflnode n ON n."ID" = en."NODE_ID" AND n."NODE_TYPE" = 5
        WHERE en."EXECUTION_ID" = h."ID") AS v2_action_nodes
FROM omwflexecutionhistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h."WORKFLOW_ID"
ORDER BY h."ID";

---- *****

SELECT setval('internal_sql_db_pk_omwflworkflowv2_id', COALESCE((SELECT MAX("ID") FROM omwflworkflowv2), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflversion_id', COALESCE((SELECT MAX("ID") FROM omwflversion), 0) + 50);
SELECT setval('internal_sql_db_pk_omwfltriggerusers_id', COALESCE((SELECT MAX("ID") FROM omwfltriggerusers), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflnode_id', COALESCE((SELECT MAX("ID") FROM omwflnode), 0) + 50);
SELECT setval('internal_sql_db_pk_mwflnodeedge_id', COALESCE((SELECT MAX("ID") FROM mwflnodeedge), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflnodespanconfig_id', COALESCE((SELECT MAX("ID") FROM omwflnodespanconfig), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflnodeevent_id', COALESCE((SELECT MAX("ID") FROM omwflnodeevent), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflactionsv2_id', COALESCE((SELECT MAX("ID") FROM omwflactionsv2), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflcriteriasetv2_id', COALESCE((SELECT MAX("ID") FROM omwflcriteriasetv2), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflcriteriav2_id', COALESCE((SELECT MAX("ID") FROM omwflcriteriav2), 0) + 50);
SELECT setval('internal_sql_db_pk_mwflcriteriavaluev2_id', COALESCE((SELECT MAX("ID") FROM mwflcriteriavaluev2), 0) + 50);
SELECT setval('internal_sql_db_pk_mwflsourcefieldcriteriav2_id', COALESCE((SELECT MAX("ID") FROM mwflsourcefieldcriteriav2), 0) + 50);
SELECT setval('internal_sql_db_pk_mwflfieldcriteriav2_id', COALESCE((SELECT MAX("ID") FROM mwflfieldcriteriav2), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflexecutionv2_id', COALESCE((SELECT MAX("ID") FROM omwflexecutionv2), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflexecutionnode_id', COALESCE((SELECT MAX("ID") FROM omwflexecutionnode), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflexecutionscheduler_id', COALESCE((SELECT MAX("ID") FROM omwflexecutionscheduler), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflexecutionwaitregistration_id', COALESCE((SELECT MAX("ID") FROM omwflexecutionwaitregistration), 0) + 50);
SELECT setval('internal_sql_db_pk_omwflexecutionsourceentity_id', COALESCE((SELECT MAX("ID") FROM omwflexecutionsourceentity), 0) + 50);
