============================================18/08/2026=====================================
================================31/07/2026=================================
1. To Update User name in OMCRMUser table:

          Backup query not required as both First and Last Name will be present in MIAMOUser table.

          a. Count query to check whether any row requires update:

    SELECT
                                                               count(*) AS remaining_mismatches
                                                           FROM
                                                               OMCRMUser as o
                                                               JOIN
                                                               MIAMOUser as m
                                                           ON o."IAM_USER_ID" = m."ID"
                                                           WHERE
                                                               lower(regexp_replace(trim(o."NAME"), '\s+', ' ', 'g')) IS DISTINCT
                                                           FROM
                                                               lower(regexp_replace(trim(concat_ws(' ', m."FIRST_NAME", m."LAST_NAME")), '\s+', ' ', 'g'));

b. Query to update the user name in OMCRMUser table. If the above count query return any this query should be executed:

BEGIN;

UPDATE
    OMCRMUser as o
SET
    "NAME" = regexp_replace(trim(concat_ws(' ', m."FIRST_NAME", m."LAST_NAME")), '\s+', ' ', 'g')
    FROM
    MIAMOUser as m
WHERE
    o."IAM_USER_ID" = m."ID"
  AND lower(regexp_replace(trim(o."NAME"), '\s+', ' ', 'g')) IS DISTINCT
FROM
    lower(regexp_replace(trim(concat_ws(' ', m."FIRST_NAME", m."LAST_NAME")), '\s+', ' ', 'g'));

COMMIT;

========================================== 06/08/2026 ============================================================================
-- Contact List View Displays Blank and Module Details Are Not Shown
-- NOTE: Execute this query only for the production user 50306.

-- Backup Query
CREATE TABLE BACKUP_8682_METSYSTEMFIELDSCONFIGURATION AS
SELECT *
FROM metsystemfieldsconfiguration
WHERE "USER_ID" = 50306;

-- Fix Query
INSERT INTO metsystemfieldsconfiguration
(
    "CONFIG",
    "FIELD_ID",
    "PROVIDER_ID",
    "ORDER",
    "USER_ID",
    "ID"
)
SELECT
    COALESCE(
            f."CONFIG_INFO"::json,
            '{"isEnabledForWeb": true}'::json
    ) AS "CONFIG",
    f."ID" AS "FIELD_ID",
    p."ID" AS "PROVIDER_ID",
    COALESCE(f."ORDER", 1) AS "ORDER",
    50306 AS "USER_ID",
    (
        SELECT COALESCE(MAX("ID"), 0)
        FROM metsystemfieldsconfiguration
    ) + ROW_NUMBER() OVER () AS "ID"
FROM metdataprovider p
         JOIN metfields f
              ON f."PROVIDER_ID" = p."ID"
WHERE p."NAME" = 'CONTACT_LIST'
  AND NOT EXISTS (
    SELECT 1
    FROM metsystemfieldsconfiguration m
    WHERE m."USER_ID" = 50306
      AND m."PROVIDER_ID" = p."ID"
      AND m."FIELD_ID" = f."ID"
);

-- Revert Query (if required)
DELETE FROM metsystemfieldsconfiguration m
WHERE m."USER_ID" = 50306
  AND NOT EXISTS (
    SELECT 1
    FROM BACKUP_8682_METSYSTEMFIELDSCONFIGURATION b
    WHERE b."ID" = m."ID"
);
===============================07/08/2026=================================
1) Query to migrate the data from metfcriteria to metffiltercriteria

INSERT INTO METFFILTERCRITERIA ("ID", "FILTER_ID", "CRITERIA_ID")
SELECT
    (SELECT COALESCE(MAX("ID"), 0) FROM METFFILTERCRITERIA)
        + ROW_NUMBER() OVER (ORDER BY m."ID"),
    m."FILTER_ID",
    m."ID"
FROM METFCRITERIA m
WHERE m."FILTER_ID" IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM METFFILTERCRITERIA x
    WHERE x."FILTER_ID" = m."FILTER_ID"
      AND x."CRITERIA_ID" = m."ID"
);

2) Query to update the sequence of metffiltercriteria
SELECT setval('internal_sql_db_pk_metffiltercriteria_id', (SELECT MAX("ID") FROM metffiltercriteria) + 1);

===========================================10/08/2026=====================================

1) To update default pipeline ID for users with incorrect pipeline ID (ID from different Org)

BEGIN;

WITH mapping AS (
    SELECT
        up."ID" AS pref_id,
        dp."ID" AS new_pipeline_id
    FROM omdouserpreference up
             JOIN mdopipeline mp ON mp."ID" = up."PIPELINE_ID"
             JOIN mdopipeline dp
                  ON dp."ORG_ID"     = up."ORG_ID"
                      AND dp."IS_DEFAULT" = true
    WHERE up."ORG_ID" != mp."ORG_ID"
    )
UPDATE omdouserpreference up
SET "PIPELINE_ID" = m.new_pipeline_id
    FROM mapping m
WHERE up."ID" = m.pref_id;

COMMIT;
==================================================================================================
1) Query 1:

CREATE OR REPLACE PROCEDURE migrate_ometsortconfig(batch_size INT DEFAULT 5000)
    LANGUAGE plpgsql
AS $$
DECLARE
v_last_id  BIGINT := 0;
    v_max_id   BIGINT;
    v_rows     INT;
BEGIN
SELECT MAX("ID") INTO v_max_id FROM OMETSortConfig;

WHILE v_last_id < v_max_id LOOP
            INSERT INTO OMSortConfig ("ID","ORG_ID","USER_ID","SOURCE_ID","VIEW_ID","CONFIG")
SELECT "ID","ORG_ID","USER_ID","SOURCE_ID","VIEW_ID","CONFIG"
FROM OMETSortConfig
WHERE "ID" > v_last_id AND "ID" <= v_last_id + batch_size
ORDER BY "ID"
    ON CONFLICT ("ID") DO NOTHING;   -- safe to re-run if it stops midway

GET DIAGNOSTICS v_rows = ROW_COUNT;
v_last_id := v_last_id + batch_size;

            RAISE NOTICE 'OMETSortConfig: migrated up to ID %, % rows this batch', v_last_id, v_rows;
COMMIT;
END LOOP;
END;
$$;

CALL migrate_ometsortconfig(5000);


2) Query 2:

CREATE OR REPLACE PROCEDURE migrate_ometkanbansortconfig(batch_size INT DEFAULT 5000)
    LANGUAGE plpgsql
AS $$
DECLARE
v_last_id  BIGINT := 0;
    v_max_id   BIGINT;
    v_rows     INT;
BEGIN
SELECT MAX("ID") INTO v_max_id FROM OMETKanbanSortConfig;

WHILE v_last_id < v_max_id LOOP
            INSERT INTO OMKanbanSortConfig ("ID","ORG_ID","SORT_CONFIG_ID","STAGE_ID")
SELECT "ID","ORG_ID","SORT_CONFIG_ID","STAGE_ID"
FROM OMETKanbanSortConfig
WHERE "ID" > v_last_id AND "ID" <= v_last_id + batch_size
ORDER BY "ID"
    ON CONFLICT ("ID") DO NOTHING;

GET DIAGNOSTICS v_rows = ROW_COUNT;
v_last_id := v_last_id + batch_size;

            RAISE NOTICE 'OMETKanbanSortConfig: migrated up to ID %, % rows this batch', v_last_id, v_rows;
COMMIT;
END LOOP;
END;
$$;

CALL migrate_ometkanbansortconfig(5000);


3) Query 3:

CREATE OR REPLACE PROCEDURE migrate_ometlistsortconfig(batch_size INT DEFAULT 5000)
    LANGUAGE plpgsql
AS $$
DECLARE
v_last_id  BIGINT := 0;
    v_max_id   BIGINT;
    v_rows     INT;
BEGIN
SELECT MAX("ID") INTO v_max_id FROM OMETListSortConfig;

WHILE v_last_id < v_max_id LOOP
            INSERT INTO OMListSortConfig ("ID","ORG_ID","SORT_CONFIG_ID","TYPE_ID")
SELECT "ID","ORG_ID","SORT_CONFIG_ID","TYPE_ID"
FROM OMETListSortConfig
WHERE "ID" > v_last_id AND "ID" <= v_last_id + batch_size
ORDER BY "ID"
    ON CONFLICT ("ID") DO NOTHING;

GET DIAGNOSTICS v_rows = ROW_COUNT;
v_last_id := v_last_id + batch_size;

            RAISE NOTICE 'OMETListSortConfig: migrated up to ID %, % rows this batch', v_last_id, v_rows;
COMMIT;
END LOOP;
END;
$$;

CALL migrate_ometlistsortconfig(5000);


4) Query 4:

CREATE OR REPLACE PROCEDURE migrate_ometlistpaginationstate(batch_size INT DEFAULT 5000)
    LANGUAGE plpgsql
AS $$
DECLARE
v_last_id  BIGINT := 0;
    v_max_id   BIGINT;
    v_rows     INT;
BEGIN
SELECT MAX("ID") INTO v_max_id FROM OMETListPaginationState;

WHILE v_last_id < v_max_id LOOP
            INSERT INTO OMListPaginationState ("ID","ORG_ID","USER_ID","TYPE_ID","START","COUNT")
SELECT "ID","ORG_ID","USER_ID","TYPE_ID","START","COUNT"
FROM OMETListPaginationState
WHERE "ID" > v_last_id AND "ID" <= v_last_id + batch_size
ORDER BY "ID"
    ON CONFLICT ("ID") DO NOTHING;

GET DIAGNOSTICS v_rows = ROW_COUNT;
v_last_id := v_last_id + batch_size;

            RAISE NOTICE 'OMETListPaginationState: migrated up to ID %, % rows this batch', v_last_id, v_rows;
COMMIT;
END LOOP;
END;
$$;

CALL migrate_ometlistpaginationstate(5000);

---------------------------------------------------------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS migrate_ometsortconfig(INT);
DROP PROCEDURE IF EXISTS migrate_ometkanbansortconfig(INT);
DROP PROCEDURE IF EXISTS migrate_ometlistsortconfig(INT);
DROP PROCEDURE IF EXISTS migrate_ometlistpaginationstate(INT);

---------------------------------------------------------------------------------------------------------------------------

SELECT setval('internal_sql_db_pk_omsortconfig_id',
              (SELECT MAX("ID") FROM OMSortConfig) + 1);

SELECT setval('internal_sql_db_pk_omkanbansortconfig_id',
              (SELECT MAX("ID") FROM OMETKanbanSortConfig) + 1);

SELECT setval('internal_sql_db_pk_omlistsortconfig_id',
              (SELECT MAX("ID") FROM OMListSortConfig) + 1);

SELECT setval('internal_sql_db_pk_omlistpaginationstate_id',
              (SELECT MAX("ID") FROM OMListPaginationState) + 1);

Note:
Server Restart required after executing the above sequence update query.
---------------------------------------------------------------------------------------------------------------------------
                                                                   ===========================================18/08/2026=====================================

                                                                   1) To Update Segment related role permissions for existing users

UPDATE
    OMCRMRole
SET
    "PERMISSIONS" = ("PERMISSIONS"::jsonb ||
                     '{"marketingCampaigns":{"marketingSegments":{"access":true,"view":true,"create":true,"edit":true,"delete":true}}}'::jsonb )::text
WHERE
    NOT ("PERMISSIONS"::jsonb ? 'marketingCampaigns');

-------------------------------------------------------------------------------------------

=======================================05/10/2026=================================================
QUERY:
UPDATE OMEMFromAddress AS fa
SET "IS_ACTIVE" = (se."SYNC_STATUS" IN (3, 5))
    FROM OMEMSyncedEmail AS se
WHERE fa."TYPE" = 3
  AND fa."ORG_ID" = se."ORG_ID"
  AND fa."USER_ID" = se."USER_ID"
  AND fa."EMAIL_ID" = se."EMAIL_ID";

-----------------------------------------------------------------------------------------------------


=======================================08/10/2026=================================================
QUERY:

Backup Query (run before the steps below):

CREATE TABLE OMQFQuotaForecastSummaryDeal_back_up AS
SELECT d.*
FROM OMQFQuotaForecastSummaryDeal d
WHERE EXISTS (SELECT 1
              FROM OMQFQuotaForecastSummaryDeal b
              WHERE b."SUMMARY_ID" = d."SUMMARY_ID" AND b."DEAL_ID" = d."DEAL_ID" AND b."ID" < d."ID")
   OR EXISTS (SELECT 1
              FROM OMQFUserQuotaForecastSummary l
                       JOIN OMQFUserQuota uq ON uq."ID" = l."USER_QUOTA_ID"
                       JOIN MDODEAL deal ON deal."ID" = d."DEAL_ID"
              WHERE l."SUMMARY_ID" = d."SUMMARY_ID" AND deal."OWNER_ID" <> uq."USER_ID");


CREATE TABLE OMQFQuotaForecastSummary_back_up AS
SELECT s.*
FROM OMQFQuotaForecastSummary s
WHERE s."ID" IN (SELECT l."SUMMARY_ID"
                 FROM OMQFUserQuotaForecastSummary l
                          JOIN OMQFUserQuota uq ON uq."ID" = l."USER_QUOTA_ID"
                          JOIN OMQFQuota q ON q."ID" = uq."QUOTA_ID"
                 WHERE q."METRIC" = 2);

1.Remove Duplicate Deal Mappings:
DELETE FROM OMQFQuotaForecastSummaryDeal a
    USING OMQFQuotaForecastSummaryDeal b
WHERE a."SUMMARY_ID" = b."SUMMARY_ID" AND a."DEAL_ID" = b."DEAL_ID" AND a."ID" > b."ID";

2.Remove Invalid Owner Deal Mappings:
DELETE FROM OMQFQuotaForecastSummaryDeal d
    USING OMQFUserQuotaForecastSummary l, OMQFUserQuota uq, MDODEAL deal
WHERE d."SUMMARY_ID" = l."SUMMARY_ID" AND l."USER_QUOTA_ID" = uq."ID"
  AND deal."ID" = d."DEAL_ID" AND deal."OWNER_ID" <> uq."USER_ID";

3.Recalculate Without-Won Count:
UPDATE OMQFQuotaForecastSummary s
SET "WITHOUT_WON" = (SELECT COUNT(*) FROM OMQFQuotaForecastSummaryDeal d WHERE d."SUMMARY_ID" = s."ID")
WHERE s."ID" IN (
    SELECT l."SUMMARY_ID"
    FROM OMQFUserQuotaForecastSummary l
             JOIN OMQFUserQuota uq ON uq."ID" = l."USER_QUOTA_ID"
             JOIN OMQFQuota q ON q."ID" = uq."QUOTA_ID"
    WHERE q."METRIC" = 2);

4.Validate Deal Mapping Counts:
SELECT s."ID", s."CATEGORY_ID", s."WITHOUT_WON", COUNT(d."DEAL_ID") AS mapped_deals
FROM OMQFQuotaForecastSummary s
         LEFT JOIN OMQFQuotaForecastSummaryDeal d ON d."SUMMARY_ID" = s."ID"
GROUP BY s."ID", s."CATEGORY_ID", s."WITHOUT_WON" ORDER BY s."ID";

5.Revert Query (if required):

INSERT INTO OMQFQuotaForecastSummaryDeal
SELECT b.*
FROM OMQFQuotaForecastSummaryDeal_back_up b
WHERE NOT EXISTS (SELECT 1 FROM OMQFQuotaForecastSummaryDeal d WHERE d."ID" = b."ID");


UPDATE OMQFQuotaForecastSummary s
SET "WITHOUT_WON" = b."WITHOUT_WON"
    FROM OMQFQuotaForecastSummary_back_up b
WHERE s."ID" = b."ID";

-----------------------------------------------------------------------------------------------------
-----------------------------------------27/10/2026--------------------------------------------------

1) To Update computation state for Segments:

UPDATE OMKSegment SET "COMPUTATION_STATE" = 3;

=======================================12/10/2026=================================================
-- These queries will be executed only in the QA and DEV
--Migration query for email add on services
--Take the back-up data
-- Back-ups

CREATE TABLE OMEMSmartBccAddress_back_up AS SELECT * FROM OMEMSmartBccAddress;
CREATE TABLE MEMParentBccAddress_back_up AS SELECT * FROM MEMParentBccAddress;
CREATE TABLE OMEMAutoForwardingAddress_back_up AS SELECT * FROM OMEMAutoForwardingAddress;
CREATE TABLE MEMParentAutoForwardingAddress_back_up AS SELECT * FROM MEMParentAutoForwardingAddress;
CREATE TABLE OMEMConversations_smartbcc_back_up AS
SELECT "ID", "IS_DELETED"
FROM OMEMConversations
WHERE "VARIANT" = 2 AND "IS_DELETED" = false;

-- Migration queries

UPDATE OMEMConversations
SET "IS_DELETED" = true
WHERE "VARIANT" = 2        -- EmailVariant.Type.SMART_BCC
  AND "IS_DELETED" = false;

-- Smart BCC
DELETE FROM OMEMSmartBccAddress;
DELETE FROM MEMParentBccAddress;

-- Team inbox auto-forwarding addresses
DELETE FROM OMEMAutoForwardingAddress;
DELETE FROM MEMParentAutoForwardingAddress;

-- Revert queries

-- 2. Restore parent tables first
INSERT INTO MEMParentBccAddress
SELECT * FROM MEMParentBccAddress_back_up;

INSERT INTO MEMParentAutoForwardingAddress
SELECT * FROM MEMParentAutoForwardingAddress_back_up;

-- 3. Restore child tables
INSERT INTO OMEMSmartBccAddress
SELECT * FROM OMEMSmartBccAddress_back_up;

INSERT INTO OMEMAutoForwardingAddress
SELECT * FROM OMEMAutoForwardingAddress_back_up;

-- 4. Undelete only the conversations this migration deleted

UPDATE OMEMConversations
SET "IS_DELETED" = false
WHERE "ID" IN (SELECT "ID" FROM OMEMConversations_smartbcc_back_up);
-----------------------------------------------------------------------------------------------------