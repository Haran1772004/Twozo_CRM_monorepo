---------------------------------------------------------------------------------------------------------------

DO $$ DECLARE batch_size INTEGER := 100; last_id BIGINT := 0; current_max_id BIGINT; inserted_count INTEGER; total_inserted INTEGER := 0; BEGIN LOOP SELECT
    MAX("ID")
INTO
    current_max_id
FROM
    (SELECT
        m."ID"
    FROM
        METFCRITERIA m
    WHERE
        m."ID" > last_id
        AND m."FILTER_ID" IS NOT NULL
    ORDER BY
        m."ID" LIMIT batch_size) batch; EXIT
WHEN current_max_id IS NULL; INSERT
                             INTO
                                 METFFILTERCRITERIA
                             ("ID", "FILTER_ID", "CRITERIA_ID") SELECT
                                                                    nextval('internal_sql_db_pk_metffiltercriteria_id'),
                                                                    m."FILTER_ID",
                                                                    m."ID"
                             FROM
                                 METFCRITERIA m
                             WHERE
                                 m."ID" > last_id
                               AND m."ID" <= current_max_id
                               AND m."FILTER_ID" IS NOT NULL
                               AND NOT EXISTS (
                                 SELECT
                                     1
                                 FROM
                                     METFFILTERCRITERIA x
                                 WHERE
                                     x."FILTER_ID" = m."FILTER_ID"
                                   AND x."CRITERIA_ID" = m."ID"
                             ); GET DIAGNOSTICS inserted_count = ROW_COUNT; total_inserted := total_inserted + inserted_count; RAISE NOTICE 'Batch: ID % - %, Inserted: %, Total: %', last_id, current_max_id, inserted_count, total_inserted; last_id := current_max_id; COMMIT;
END LOOP; RAISE NOTICE 'Completed. Total records inserted: %', total_inserted;
END $$;

** Update the Sequence

SELECT setval('internal_sql_db_pk_metffiltercriteria_id', (SELECT MAX("ID") FROM METFFILTERCRITERIA) + 100);
----------------------------------------------------------------------------------------------------------------------------------------------

=======================================03/10/2026=============================================
QUERY:
-- BackUp Query:
CREATE TABLE mdostageprogress_backup_6103 AS
SELECT b.*,
       a."PIPELINE_STAGE_ID" AS "DEAL_PIPELINE_STAGE_ID"
FROM mdostageprogress b
         JOIN mdodeal a
              ON a."ID" = b."DEAL_ID"
WHERE a."PIPELINE_STAGE_ID" IS DISTINCT FROM b."PIPELINE_STAGE_ID"
    AND a."ORG_ID" = 6103
    AND b."END_TIME" IS NULL;

-- UPDATE Query:
update mdostageprogress b
set "PIPELINE_STAGE_ID" = a."PIPELINE_STAGE_ID"
    from mdodeal a
where a."ID" = b."DEAL_ID" and a."PIPELINE_STAGE_ID" IS DISTINCT FROM b."PIPELINE_STAGE_ID" and a."ORG_ID" = 6103 and b."END_TIME" is null;

-- Verification Query::
select a."ID" , a."TITLE" from mdodeal a join mdostageprogress b on a."ID" = b."DEAL_ID" where a."PIPELINE_STAGE_ID" != b."PIPELINE_STAGE_ID" and a."ORG_ID" = 6103 and b."END_TIME" is null;

-- Revert Query:
UPDATE mdostageprogress b
SET "PIPELINE_STAGE_ID" = bk."PIPELINE_STAGE_ID"
    FROM mdostageprogress_backup_6103 bk
WHERE b."ID" = bk."ID";

------------------------------------------------------------------------------------------------------------------------
===========================================27/08/2026=======================================================================
-- Backup Query
CREATE TABLE BACKUP_8718_OMETFILTER AS
SELECT *
FROM "ometfilter"
WHERE "ID" IN (156806, 156807, 156808, 156809, 156810)
  AND "ORG_ID" = 89410
  AND "NAME" = '60352';

-- Fix Query
DELETE FROM "ometfilter"
WHERE "ID" IN (156806, 156807, 156808, 156809, 156810)
  AND "ORG_ID" = 89410
  AND "NAME" = '60352';

-- Revert Query (if required)
INSERT INTO "ometfilter"
SELECT *
FROM BACKUP_8718_OMETFILTER;

-----------------------------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------------------

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
======================================================================================================================
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
--------------------------------------------------------------------------------------------------------------------------

------------------------- 07/08/2026 -----------------------------
-- Max count in the product code is not shown
-- NOTE: Execute this query only in production env .
    DO $$
DECLARE
rows_updated INT;
    batch_size INT := 5000;
    total_processed INT := 0;
    max_id INT;
    current_id INT := 0;
BEGIN
SELECT MAX(a."ID") INTO max_id
FROM ometfieldconfig a
WHERE a."FIELD_ID" IN (
    SELECT "ID"
    FROM ometfields
    WHERE "ALIAS_NAME" IN ('productCode', 'skuNumber')
)
  AND jsonb_typeof(a."PRESET_CONFIG"::jsonb -> 'maxLength') = 'object';
WHILE current_id <= COALESCE(max_id, 0) LOOP
UPDATE ometfieldconfig a
SET "PRESET_CONFIG" =
        jsonb_set(
                jsonb_set(
                        a."PRESET_CONFIG"::jsonb,
                        '{maxLength}',
                        (a."PRESET_CONFIG"::jsonb -> 'maxLength' -> 'value'),
                        true
                ),
                '{maxLengthMessage}',
                COALESCE(
                        a."PRESET_CONFIG"::jsonb -> 'maxLength' -> 'maxLengthMessage',
                        '"i18n.fields.violation.message.max.length"'::jsonb
                ),
                true
        )::text
WHERE a."FIELD_ID" IN (
    SELECT "ID"
    FROM ometfields
    WHERE "ALIAS_NAME" IN ('productCode', 'skuNumber')
    )
  AND a."ID" > current_id
  AND a."ID" <= current_id + batch_size
  AND jsonb_typeof(a."PRESET_CONFIG"::jsonb -> 'maxLength') = 'object';
GET DIAGNOSTICS rows_updated = ROW_COUNT;
total_processed := total_processed + rows_updated;
        current_id := current_id + batch_size;
END LOOP;
END $$;

------------------------------------------------------------------------------------------------------------------------
==============================================06/08/2026 ==================================================
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

--------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------------------------