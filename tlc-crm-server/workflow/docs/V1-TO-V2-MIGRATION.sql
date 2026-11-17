-- =============================================================================
-- Workflow V1 → V2 migration (SQL only)
-- =============================================================================
-- Target: MySQL 8+ / MariaDB 10.4+ (uses ROW_NUMBER).
-- Reuses V1 OMWFLWorkflow.ID as OMWFLWorkflowV2.ID.
-- New IDs for version/node/edge/criteria/action rows: MAX(ID) + ROW_NUMBER().
-- After migration, advance platform sequence counters to MAX(ID) per table.
--
-- Run in order: pre-flight → DDL maps → migrate definitions → migrate history
-- → validation. Wrap sections 3–4 in a transaction on first pilot org.
-- =============================================================================

-- Optional: limit pilot to one org (repeat script per org or remove filter)
-- SET @pilot_org_id = 12345;

-- -----------------------------------------------------------------------------
-- 1) Pre-flight
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS v1_active_workflows FROM OMWFLWorkflow WHERE IS_DELETED = 0;

SELECT COUNT(*) AS v1_workflows_without_actions
FROM OMWFLWorkflow w
WHERE w.IS_DELETED = 0
  AND NOT EXISTS (SELECT 1 FROM OMWFLActions a WHERE a.WORKFLOW_ID = w.ID);

SELECT COUNT(*) AS v1_in_progress_histories
FROM OMWFLExecutionHistory WHERE STATUS = 1;

-- -----------------------------------------------------------------------------
-- 2) Mapping / staging tables (idempotent)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS map_workflow_v1_v2 (
  v1_workflow_id BIGINT PRIMARY KEY,
  v2_workflow_id BIGINT NOT NULL,
  v2_version_id  BIGINT NOT NULL,
  org_id         BIGINT NOT NULL,
  migrated_at    DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS map_node_v1 (
  v1_workflow_id BIGINT NOT NULL,
  client_key     VARCHAR(50) NOT NULL,
  v2_node_id     BIGINT NOT NULL,
  node_type      SMALLINT NOT NULL,
  PRIMARY KEY (v1_workflow_id, client_key)
);

CREATE TABLE IF NOT EXISTS map_action_v1_v2 (
  v1_action_id   BIGINT PRIMARY KEY,
  v1_workflow_id BIGINT NOT NULL,
  v2_node_id     BIGINT NOT NULL,
  v2_action_id   BIGINT NOT NULL,
  org_id         BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS map_criteria_set_v1_v2 (v1_set_id BIGINT PRIMARY KEY, v2_set_id BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS map_criteria_v1_v2 (v1_criteria_id BIGINT PRIMARY KEY, v2_criteria_id BIGINT NOT NULL);

CREATE TABLE IF NOT EXISTS map_history_v1_v2 (
  v1_history_id   BIGINT PRIMARY KEY,
  v2_execution_id BIGINT NOT NULL,
  org_id          BIGINT NOT NULL
);

-- -----------------------------------------------------------------------------
-- 3) Definition migration
-- -----------------------------------------------------------------------------
START TRANSACTION;

-- 3.1 Workflows pending migration
DROP TEMPORARY TABLE IF EXISTS mig_wf;
CREATE TEMPORARY TABLE mig_wf AS
SELECT
  w.ID AS v1_workflow_id,
  w.ID AS v2_workflow_id,
  w.ORG_ID,
  w.OWNER_ID,
  w.NAME,
  w.DESCRIPTION,
  w.TEMPLATE_ID,
  w.TRIGGER_SOURCE,
  w.TRIGGER_EVENT,
  w.IS_ACTIVE,
  w.CREATED_TIME,
  w.MODIFIED_TIME,
  w.IS_DELETED,
  CASE
    WHEN EXISTS (
      SELECT 1 FROM OMWFLCriteriaSet cs
      INNER JOIN OMWFLCriteria c ON c.SET_ID = cs.ID
      WHERE cs.WORKFLOW_ID = w.ID
    ) THEN 1 ELSE 0
  END AS has_condition
FROM OMWFLWorkflow w
WHERE w.IS_DELETED = 0
  AND EXISTS (SELECT 1 FROM OMWFLActions a WHERE a.WORKFLOW_ID = w.ID)
  AND NOT EXISTS (SELECT 1 FROM OMWFLWorkflowV2 v2 WHERE v2.ID = w.ID)
  AND NOT EXISTS (SELECT 1 FROM map_workflow_v1_v2 m WHERE m.v1_workflow_id = w.ID);
  -- AND (@pilot_org_id IS NULL OR w.ORG_ID = @pilot_org_id);

-- 3.2 Workflow V2 headers (same ID as V1)
INSERT INTO OMWFLWorkflowV2 (
  ID, ORG_ID, OWNER_ID, NAME, DESCRIPTION, TRIGGER_SCOPE,
  TRIGGER_SOURCE, TRIGGER_EVENT, TEMPLATE_ID, IS_ACTIVE,
  CREATED_TIME, MODIFIED_TIME, IS_DELETED
)
SELECT
  v2_workflow_id, ORG_ID, OWNER_ID, NAME, DESCRIPTION,
  2,
  TRIGGER_SOURCE, TRIGGER_EVENT, TEMPLATE_ID, IS_ACTIVE,
  CREATED_TIME, MODIFIED_TIME, IS_DELETED
FROM mig_wf;

-- 3.3 Published version 1 per workflow
DROP TEMPORARY TABLE IF EXISTS mig_version;
SET @ver_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLVersion);

CREATE TEMPORARY TABLE mig_version AS
SELECT
  @ver_base + ROW_NUMBER() OVER (ORDER BY v1_workflow_id) AS version_id,
  v1_workflow_id,
  v2_workflow_id,
  ORG_ID,
  COALESCE(MODIFIED_TIME, CREATED_TIME) AS published_time
FROM mig_wf;

INSERT INTO OMWFLVersion (ID, ORG_ID, WORKFLOW_ID, VERSION_NUMBER, STATUS, INITIAL_NODE_ID, PUBLISHED_TIME)
SELECT version_id, ORG_ID, v2_workflow_id, 1, 1, 1, published_time
FROM mig_version;

INSERT INTO map_workflow_v1_v2 (v1_workflow_id, v2_workflow_id, v2_version_id, org_id, migrated_at)
SELECT v1_workflow_id, v2_workflow_id, version_id, ORG_ID, NOW()
FROM mig_version;

-- 3.4 Node plan (trigger, optional condition, actions)
DROP TEMPORARY TABLE IF EXISTS mig_node_plan;
CREATE TEMPORARY TABLE mig_node_plan AS
SELECT v.v1_workflow_id, v.version_id, v.ORG_ID, '1' AS client_key, 1 AS node_type, 1 AS seq, NULL AS action_ord
FROM mig_version v
UNION ALL
SELECT v.v1_workflow_id, v.version_id, v.ORG_ID, '2', 2, 2, NULL
FROM mig_version v
INNER JOIN mig_wf w ON w.v1_workflow_id = v.v1_workflow_id AND w.has_condition = 1
UNION ALL
SELECT
  v.v1_workflow_id,
  v.version_id,
  v.ORG_ID,
  CAST(
    (CASE WHEN w.has_condition = 1 THEN 2 ELSE 1 END)
      + ROW_NUMBER() OVER (PARTITION BY a.WORKFLOW_ID ORDER BY a.`ORDER`)
    AS CHAR
  ) AS client_key,
  5 AS node_type,
  (CASE WHEN w.has_condition = 1 THEN 2 ELSE 1 END)
    + ROW_NUMBER() OVER (PARTITION BY a.WORKFLOW_ID ORDER BY a.`ORDER`) AS seq,
  ROW_NUMBER() OVER (PARTITION BY a.WORKFLOW_ID ORDER BY a.`ORDER`) AS action_ord
FROM mig_version v
INNER JOIN mig_wf w ON w.v1_workflow_id = v.v1_workflow_id
INNER JOIN OMWFLActions a ON a.WORKFLOW_ID = v.v1_workflow_id;

DROP TEMPORARY TABLE IF EXISTS mig_node;
SET @node_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLNode);

CREATE TEMPORARY TABLE mig_node AS
SELECT
  @node_base + ROW_NUMBER() OVER (ORDER BY v1_workflow_id, seq) AS node_id,
  v1_workflow_id,
  version_id,
  ORG_ID,
  client_key,
  node_type,
  seq,
  action_ord
FROM mig_node_plan;

INSERT INTO OMWFLNode (ID, ORG_ID, VERSION_ID, CLIENT_KEY, NODE_TYPE)
SELECT node_id, ORG_ID, version_id, client_key, node_type
FROM mig_node;

INSERT INTO map_node_v1 (v1_workflow_id, client_key, v2_node_id, node_type)
SELECT v1_workflow_id, client_key, node_id, node_type
FROM mig_node;

-- 3.5 NEXT edges along seq chain
DROP TEMPORARY TABLE IF EXISTS mig_edge_pairs;
CREATE TEMPORARY TABLE mig_edge_pairs AS
SELECT n1.node_id AS from_node_id, n2.node_id AS to_node_id
FROM mig_node n1
INNER JOIN mig_node n2
  ON n1.v1_workflow_id = n2.v1_workflow_id AND n2.seq = n1.seq + 1;

SET @edge_base := (SELECT COALESCE(MAX(ID), 0) FROM MWFLNodeEdge);

INSERT INTO MWFLNodeEdge (ID, FROM_NODE_ID, TO_NODE_ID, EDGE_TYPE)
SELECT @edge_base + ROW_NUMBER() OVER (ORDER BY from_node_id), from_node_id, to_node_id, 1
FROM mig_edge_pairs;

-- 3.6 Condition node event (CONTEXT_TYPE = 1 TRIGGER)
INSERT INTO OMWFLNodeEvent (ID, ORG_ID, NODE_ID, CONTEXT_TYPE, SOURCE_NODE_ID)
SELECT
  (SELECT COALESCE(MAX(ID), 0) FROM OMWFLNodeEvent)
    + ROW_NUMBER() OVER (ORDER BY cond.node_id),
  cond.ORG_ID,
  cond.node_id,
  1,
  trig.node_id
FROM mig_node cond
INNER JOIN mig_node trig
  ON trig.v1_workflow_id = cond.v1_workflow_id AND trig.client_key = '1'
WHERE cond.node_type = 2;

-- 3.7 Criteria sets → V2 (on condition nodes)
DROP TEMPORARY TABLE IF EXISTS mig_set_src;
CREATE TEMPORARY TABLE mig_set_src AS
SELECT
  cs.ID AS v1_set_id,
  cs.ORG_ID,
  mn.node_id,
  cs.CONNECTOR_ID,
  cs.`ORDER` AS set_order
FROM OMWFLCriteriaSet cs
INNER JOIN mig_node mn ON mn.v1_workflow_id = cs.WORKFLOW_ID AND mn.node_type = 2;

SET @set_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLCriteriaSetV2);

DROP TEMPORARY TABLE IF EXISTS mig_set_new;
CREATE TEMPORARY TABLE mig_set_new AS
SELECT
  v1_set_id,
  @set_base + ROW_NUMBER() OVER (ORDER BY v1_set_id) AS v2_set_id,
  ORG_ID,
  node_id,
  CONNECTOR_ID,
  set_order
FROM mig_set_src;

INSERT INTO map_criteria_set_v1_v2 (v1_set_id, v2_set_id)
SELECT v1_set_id, v2_set_id FROM mig_set_new;

INSERT INTO OMWFLCriteriaSetV2 (ID, ORG_ID, NODE_ID, CONNECTOR_ID, `ORDER`)
SELECT v2_set_id, ORG_ID, node_id, CONNECTOR_ID, set_order
FROM mig_set_new;

-- 3.8 Criteria → V2
DROP TEMPORARY TABLE IF EXISTS mig_crit_src;
CREATE TEMPORARY TABLE mig_crit_src AS
SELECT
  c.ID AS v1_criteria_id,
  c.ORG_ID,
  mn.node_id,
  sn.v2_set_id,
  c.COMPARATOR_ID,
  c.CONNECTOR_ID,
  c.`ORDER` AS crit_order
FROM OMWFLCriteria c
INNER JOIN mig_set_new sn ON sn.v1_set_id = c.SET_ID
INNER JOIN mig_node mn ON mn.v1_workflow_id = c.WORKFLOW_ID AND mn.node_type = 2;

SET @crit_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLCriteriaV2);

DROP TEMPORARY TABLE IF EXISTS mig_crit_new;
CREATE TEMPORARY TABLE mig_crit_new AS
SELECT
  v1_criteria_id,
  @crit_base + ROW_NUMBER() OVER (ORDER BY v1_criteria_id) AS v2_criteria_id,
  ORG_ID,
  node_id,
  v2_set_id,
  COMPARATOR_ID,
  CONNECTOR_ID,
  crit_order
FROM mig_crit_src;

INSERT INTO map_criteria_v1_v2 (v1_criteria_id, v2_criteria_id)
SELECT v1_criteria_id, v2_criteria_id FROM mig_crit_new;

INSERT INTO OMWFLCriteriaV2 (ID, ORG_ID, NODE_ID, SET_ID, COMPARATOR_ID, CONNECTOR_ID, `ORDER`)
SELECT v2_criteria_id, ORG_ID, node_id, v2_set_id, COMPARATOR_ID, CONNECTOR_ID, crit_order
FROM mig_crit_new;

-- 3.9 Criteria values / field links
SET @cv_base := (SELECT COALESCE(MAX(ID), 0) FROM MWFLCriteriaValueV2);

INSERT INTO MWFLCriteriaValueV2 (ID, CRITERIA_ID, VALUE_ID, VALUE)
SELECT
  @cv_base + ROW_NUMBER() OVER (ORDER BY cv.ID),
  cn.v2_criteria_id,
  cv.VALUE_ID,
  cv.VALUE
FROM MWFLCriteriaValue cv
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = cv.CRITERIA_ID;

SET @fc_base := (SELECT COALESCE(MAX(ID), 0) FROM MWFLFieldCriteriaV2);

INSERT INTO MWFLFieldCriteriaV2 (ID, CRITERIA_ID, FIELD_ID)
SELECT
  @fc_base + ROW_NUMBER() OVER (ORDER BY fc.ID),
  cn.v2_criteria_id,
  fc.FIELD_ID
FROM MWFLFieldCriteria fc
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = fc.CRITERIA_ID;

SET @sfc_base := (SELECT COALESCE(MAX(ID), 0) FROM MWFLSourceFieldCriteriaV2);

INSERT INTO MWFLSourceFieldCriteriaV2 (ID, CRITERIA_ID, FIELD_ID)
SELECT
  @sfc_base + ROW_NUMBER() OVER (ORDER BY sfc.ID),
  cn.v2_criteria_id,
  sfc.FIELD_ID
FROM MWFLSourceFieldCriteria sfc
INNER JOIN mig_crit_new cn ON cn.v1_criteria_id = sfc.CRITERIA_ID;

-- 3.10 Actions V2 (one per action node)
DROP TEMPORARY TABLE IF EXISTS mig_action_ord;
CREATE TEMPORARY TABLE mig_action_ord AS
SELECT
  ID AS v1_action_id,
  WORKFLOW_ID AS v1_workflow_id,
  ORG_ID,
  ACTION_SOURCE,
  ACTION_EVENT,
  FIELD_MAPPING,
  ROW_NUMBER() OVER (PARTITION BY WORKFLOW_ID ORDER BY `ORDER`) AS action_ord
FROM OMWFLActions
WHERE WORKFLOW_ID IN (SELECT v1_workflow_id FROM mig_wf);

DROP TEMPORARY TABLE IF EXISTS mig_action_ins;
CREATE TEMPORARY TABLE mig_action_ins AS
SELECT
  ao.v1_action_id,
  ao.v1_workflow_id,
  ao.ORG_ID,
  mn.node_id,
  ao.ACTION_SOURCE,
  ao.ACTION_EVENT,
  ao.FIELD_MAPPING,
  ROW_NUMBER() OVER (ORDER BY ao.v1_action_id) AS rn
FROM mig_action_ord ao
INNER JOIN mig_node mn
  ON mn.v1_workflow_id = ao.v1_workflow_id AND mn.node_type = 5 AND mn.action_ord = ao.action_ord;

SET @act_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLActionsV2);

INSERT INTO OMWFLActionsV2 (ID, ORG_ID, NODE_ID, ACTION_SOURCE, ACTION_EVENT, FIELD_MAPPING)
SELECT
  @act_base + rn,
  ORG_ID,
  node_id,
  ACTION_SOURCE,
  ACTION_EVENT,
  FIELD_MAPPING
FROM mig_action_ins;

INSERT INTO map_action_v1_v2 (v1_action_id, v1_workflow_id, v2_node_id, v2_action_id, org_id)
SELECT v1_action_id, v1_workflow_id, node_id, @act_base + rn, ORG_ID
FROM mig_action_ins;

COMMIT;

-- -----------------------------------------------------------------------------
-- 4) Execution history migration
-- -----------------------------------------------------------------------------
START TRANSACTION;

-- 4.1 Execution rows (reuse V1 history ID as V2 execution ID)
INSERT INTO OMWFLEXECUTIONV2 (
  ID, ORG_ID, USER_ID, WORKFLOW_ID, VERSION_ID, STATUS,
  NODE_CONTEXT_JSON, STARTED_TIME, COMPLETED_TIME
)
SELECT
  h.ID,
  h.ORG_ID,
  w.OWNER_ID,
  m.v2_workflow_id,
  m.v2_version_id,
  CASE h.STATUS WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 5 ELSE 1 END,
  h.WORKFLOW_STATE,
  h.WORKFLOW_EXECUTED_TIME,
  CASE
    WHEN h.STATUS IN (2, 3) THEN GREATEST(
      h.WORKFLOW_EXECUTED_TIME,
      COALESCE(h.CRITERIA_EXECUTED_TIME, h.WORKFLOW_EXECUTED_TIME),
      COALESCE((
        SELECT MAX(ah.ACTION_EXECUTED_TIME)
        FROM OMWFLActionExecutionHistory ah
        WHERE ah.EXECUTION_HISTORY_ID = h.ID
      ), h.WORKFLOW_EXECUTED_TIME)
    )
    ELSE NULL
  END
FROM OMWFLExecutionHistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h.WORKFLOW_ID
INNER JOIN OMWFLWorkflow w ON w.ID = m.v1_workflow_id
WHERE NOT EXISTS (SELECT 1 FROM map_history_v1_v2 x WHERE x.v1_history_id = h.ID)
  AND NOT EXISTS (SELECT 1 FROM OMWFLEXECUTIONV2 e WHERE e.ID = h.ID);

INSERT INTO map_history_v1_v2 (v1_history_id, v2_execution_id, org_id)
SELECT h.ID, h.ID, h.ORG_ID
FROM OMWFLExecutionHistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h.WORKFLOW_ID
WHERE EXISTS (SELECT 1 FROM OMWFLEXECUTIONV2 e WHERE e.ID = h.ID)
  AND NOT EXISTS (SELECT 1 FROM map_history_v1_v2 x WHERE x.v1_history_id = h.ID);

-- 4.2 Trigger execution nodes
SET @en_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLExecutionNode);

INSERT INTO OMWFLExecutionNode (ID, ORG_ID, EXECUTION_ID, NODE_ID, STATUS, STARTED_TIME, COMPLETED_TIME, FAILURE_REASON)
SELECT
  @en_base + ROW_NUMBER() OVER (ORDER BY h.ID),
  h.ORG_ID,
  h.ID,
  mn.v2_node_id,
  2,
  h.WORKFLOW_EXECUTED_TIME,
  h.WORKFLOW_EXECUTED_TIME,
  NULL
FROM OMWFLExecutionHistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h.WORKFLOW_ID
INNER JOIN map_node_v1 mn ON mn.v1_workflow_id = m.v1_workflow_id AND mn.client_key = '1'
WHERE EXISTS (SELECT 1 FROM OMWFLEXECUTIONV2 e WHERE e.ID = h.ID)
  AND NOT EXISTS (
    SELECT 1 FROM OMWFLExecutionNode en
    WHERE en.EXECUTION_ID = h.ID AND en.NODE_ID = mn.v2_node_id
  );

-- 4.3 Condition execution nodes
SET @en_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLExecutionNode);

INSERT INTO OMWFLExecutionNode (ID, ORG_ID, EXECUTION_ID, NODE_ID, STATUS, STARTED_TIME, COMPLETED_TIME, FAILURE_REASON)
SELECT
  @en_base + ROW_NUMBER() OVER (ORDER BY h.ID),
  h.ORG_ID,
  h.ID,
  mn.v2_node_id,
  CASE WHEN h.CRITERIA_STATUS = 2 THEN 3 ELSE 2 END,
  h.WORKFLOW_EXECUTED_TIME,
  COALESCE(h.CRITERIA_EXECUTED_TIME, h.WORKFLOW_EXECUTED_TIME),
  NULL
FROM OMWFLExecutionHistory h
INNER JOIN map_workflow_v1_v2 m ON m.v1_workflow_id = h.WORKFLOW_ID
INNER JOIN map_node_v1 mn ON mn.v1_workflow_id = m.v1_workflow_id AND mn.client_key = '2'
WHERE h.CRITERIA_STATUS IS NOT NULL
  AND EXISTS (SELECT 1 FROM OMWFLEXECUTIONV2 e WHERE e.ID = h.ID)
  AND NOT EXISTS (
    SELECT 1 FROM OMWFLExecutionNode en
    WHERE en.EXECUTION_ID = h.ID AND en.NODE_ID = mn.v2_node_id
  );

-- 4.4 Action execution nodes (from V1 action history)
SET @en_base := (SELECT COALESCE(MAX(ID), 0) FROM OMWFLExecutionNode);

INSERT INTO OMWFLExecutionNode (ID, ORG_ID, EXECUTION_ID, NODE_ID, STATUS, STARTED_TIME, COMPLETED_TIME, FAILURE_REASON)
SELECT
  @en_base + ROW_NUMBER() OVER (ORDER BY ah.ID),
  ah.ORG_ID,
  ah.EXECUTION_HISTORY_ID,
  am.v2_node_id,
  CASE WHEN ah.STATUS = 1 THEN 2 ELSE 3 END,
  ah.ACTION_EXECUTED_TIME,
  ah.ACTION_EXECUTED_TIME,
  ah.FAILURE_REASON
FROM OMWFLActionExecutionHistory ah
INNER JOIN map_action_v1_v2 am ON am.v1_action_id = ah.ACTION_ID
WHERE EXISTS (SELECT 1 FROM OMWFLEXECUTIONV2 e WHERE e.ID = ah.EXECUTION_HISTORY_ID)
  AND NOT EXISTS (
    SELECT 1 FROM OMWFLExecutionNode en
    WHERE en.EXECUTION_ID = ah.EXECUTION_HISTORY_ID AND en.NODE_ID = am.v2_node_id
  );

COMMIT;

-- -----------------------------------------------------------------------------
-- 5) Validation
-- -----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM OMWFLWorkflow WHERE IS_DELETED = 0) AS v1_active_cnt,
  (SELECT COUNT(*) FROM map_workflow_v1_v2) AS migrated_workflow_cnt;

SELECT m.v1_workflow_id,
       (SELECT COUNT(*) FROM OMWFLActions a WHERE a.WORKFLOW_ID = m.v1_workflow_id) AS v1_actions,
       (SELECT COUNT(*) FROM OMWFLNode n
        INNER JOIN OMWFLVersion v ON v.ID = n.VERSION_ID
        WHERE v.WORKFLOW_ID = m.v2_workflow_id AND n.NODE_TYPE = 5) AS v2_action_nodes
FROM map_workflow_v1_v2 m;

-- -----------------------------------------------------------------------------
-- 6) Rollback (destructive — V1 data untouched)
-- -----------------------------------------------------------------------------
-- START TRANSACTION;
-- DELETE FROM OMWFLEXECUTIONV2 WHERE WORKFLOW_ID IN (SELECT v2_workflow_id FROM map_workflow_v1_v2);
-- DELETE FROM OMWFLWorkflowV2 WHERE ID IN (SELECT v2_workflow_id FROM map_workflow_v1_v2);
-- TRUNCATE TABLE map_history_v1_v2;
-- TRUNCATE TABLE map_criteria_v1_v2;
-- TRUNCATE TABLE map_criteria_set_v1_v2;
-- TRUNCATE TABLE map_action_v1_v2;
-- TRUNCATE TABLE map_node_v1;
-- TRUNCATE TABLE map_workflow_v1_v2;
-- COMMIT;
