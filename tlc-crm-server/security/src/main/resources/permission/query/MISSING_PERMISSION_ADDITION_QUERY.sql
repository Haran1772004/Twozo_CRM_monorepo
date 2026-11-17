UPDATE OMCRMRole
SET "PERMISSIONS" = jsonb_set(
        "PERMISSIONS"::jsonb,
        '{analytics}',
        (
            SELECT COALESCE(OMCRMRole."PERMISSIONS"::jsonb -> 'analytics', '{}'::jsonb) || jsonb_object_agg(key, value)
            FROM (
                     SELECT key, value
                     FROM jsonb_each('{
              "contacts": {"access": true, "scope": "viewEditAndManage"},
              "companies": {"access": true, "scope": "viewEditAndManage"},
              "deals": {"access": true, "scope": "viewEditAndManage"},
              "dealLineItems": {"access": true, "scope": "viewEditAndManage"},
              "emails": {"access": true, "scope": "viewEditAndManage"},
              "phoneCalls": {"access": true, "scope": "viewEditAndManage"},
              "calls": {"access": true, "scope": "viewEditAndManage"},
              "salesGoals": {"access": true, "scope": "viewEditAndManage"},
              "activities": {"access": true, "scope": "viewEditAndManage"}
            }'::jsonb)
                 ) AS targetPermissions
            WHERE NOT (COALESCE(OMCRMRole."PERMISSIONS"::jsonb -> 'analytics', '{}'::jsonb) ? key)
        ),
        true
                    )
WHERE "NAME" = 'Sales Representative'
  AND "PERMISSIONS"::jsonb ? 'analytics'
  AND NOT (("PERMISSIONS"::jsonb -> 'analytics') ?& array['contacts','companies','deals','dealLineItems','emails','phoneCalls','calls','salesGoals','activities']);
