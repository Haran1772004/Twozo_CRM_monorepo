package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.sql.resource.OACDIMUSER;
import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.cdc.CrmCdcPublisher;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.web.client.event.EventCallback;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Enriches CRM user CDC events with analytics specific data.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmUserEnricher extends AbstractAnalyticsCdcEnricher {

    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final CrmCdcPublisher crmCdcPublisher;

    private AnalyticsCrmUserEnricher() {
        this.iamUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.crmCdcPublisher = CrmCdcPublisher.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmUserEnricher USER_ENRICHER = new AnalyticsCrmUserEnricher();
    }

    static AnalyticsCrmUserEnricher getInstance() {
        return Instance.USER_ENRICHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedRecords Map of JSON objects to be enriched which is grouped by table.
     * @return Collection of enriched records.
     */
    @Override
    public Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(OMCRMUSER.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {
            final Map<Long, Long> userIds = new HashMap<>();
            final Map<Long, JsonObject> userJsonMap = new HashMap<>();
            Long orgId = null;

            for (final JsonObject crmUserJson : records) {
                final JsonObject cdcJson = crmUserJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                final long iamUserId = cdcJson.getLong(OMCRMUSER.IAM_USER_ID);
                orgId = cdcJson.getLong(OMCRMUSER.ORG_ID);

                userIds.put(iamUserId, cdcJson.getLong(OMCRMUSER.ID));
                userJsonMap.put(iamUserId, crmUserJson);
            }

            final EventCallback<Collection<User>> userFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Collection<EnrichedRecord> enrichedRecords = new HashSet<>();

                    for (final User user : users) {
                        final Long iamUserId = user.id();
                        final JsonObject crmUserJson = userJsonMap.get(iamUserId);
                        final JsonObject cdcJson = crmUserJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                        final JsonObject jsonObject = Json.object();
                        final DateTime createdTime = user.getCreatedTime();
                        final String dateTime = TemporalServiceProvider.get(Objects.requireNonNullElse(createdTime.getTimeStamp(), Instant.now().toEpochMilli())).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER);

                        jsonObject.put(OACDIMUSER.ID, userIds.get(iamUserId));
                        jsonObject.put(OACDIMUSER.ORG_ID, cdcJson.getLong(OMCRMUSER.ORG_ID));
                        jsonObject.put(OACDIMUSER.NAME, user.getFullName());
                        jsonObject.put(OACDIMUSER.EMAIL, user.getEmail());
                        jsonObject.put(OACDIMUSER.PHONE, user.getMobile());
                        jsonObject.put(OACDIMUSER.CREATED_AT, dateTime);
                        jsonObject.put(OACDIMUSER.STATE, user.getUserState().id());
                        jsonObject.put(OACDIMUSER.UPDATED_AT, dateTime);
                        jsonObject.put(OACDIMUSER.IS_DELETED, Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, crmUserJson.getString(AnalyticsCdcKeys.OPERATION)));
                        jsonObject.put(AnalyticsCdcKeys.TOPIC, OACDIMUSER.TABLE.toLowerCase());

                        enrichedRecords.add(new EnrichedRecord(crmUserJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID), jsonObject));
                    }
                    crmCdcPublisher.publish(enrichedRecords);
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("Failed to fetch IAM user details for UserIDs: {}. Error: {}", userIds.keySet(), throwable.getMessage());
                }
            };
            iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), userIds.keySet(), userFetchCallback);
        }

        return Collections.emptyList();
    }
}