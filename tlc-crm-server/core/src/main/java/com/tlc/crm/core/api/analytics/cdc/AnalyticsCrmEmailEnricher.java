package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMCOMPANY;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACDIMDEAL;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMCRMENTITYINSIGHTS;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.olap.api.dml.OlapTable;

import java.util.Objects;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;

/**
 * Enriches CRM Email CDC events with analytics specific data.
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 */
final class AnalyticsCrmEmailEnricher extends AbstractAnalyticsCdcEnricher{

    private AnalyticsCrmEmailEnricher() {
        super();
    }

    private static class Instance {
        private static final AnalyticsCrmEmailEnricher INSTANCE = new AnalyticsCrmEmailEnricher();
    }

    static AnalyticsCrmEmailEnricher getInstance() {
        return AnalyticsCrmEmailEnricher.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedRecords Map of JSON objects to be enriched which is grouped by table.
     * @return Collection of enriched records.
     */
    @Override
    public Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords) {
        final String tableName = groupedRecords.keySet().stream().findFirst().orElse(null);
        final Collection<EnrichedRecord> enrichedRecords = new HashSet<>();

        if (OMCRMENTITYINSIGHTS.TABLE.equalsIgnoreCase(tableName)) {
            final Collection<JsonObject> cdcEvents = groupedRecords.get(tableName);

            for (final JsonObject cdcEvent : cdcEvents) {
                final Integer entityTypeId = cdcEvent.optJsonObject(AnalyticsCdcKeys.AFTER).getInt(OMCRMENTITYINSIGHTS.ENTITY_TYPE);

                if (Objects.equals(Source.CONTACT.getId(), entityTypeId)) {
                    final Collection<TableDetail> tableDetails = metaHandleService.getTableDetails(ModuleType.getModuleTypeByName(AnalyticsCdcKeys.CONTACT).getTypeCode());

                    for (TableDetail tableDetail : tableDetails) {

                        if (Objects.equals(OACDIMCONTACT.TABLE, tableDetail.getTableName())) {
                            loadDimensionContact(tableDetail, cdcEvent, enrichedRecords);
                        }
                    }
                } else if (Objects.equals(Source.COMPANY.getId(), entityTypeId)) {
                    final Collection<TableDetail> tableDetails = metaHandleService.getTableDetails(ModuleType.getModuleTypeByName(AnalyticsCdcKeys.COMPANY).getTypeCode());

                    for (TableDetail tableDetail : tableDetails) {

                        if (Objects.equals(OACDIMCOMPANY.TABLE, tableDetail.getTableName())) {
                            buildDimensionCompany(tableDetail, cdcEvent, enrichedRecords);
                        }
                    }
                } else if (Objects.equals(Source.DEAL.getId(), entityTypeId)) {
                    final Collection<TableDetail> tableDetails = metaHandleService.getTableDetails(ModuleType.getModuleTypeByName(AnalyticsCdcKeys.DEAL).getTypeCode());

                    for (TableDetail tableDetail : tableDetails) {

                        if (Objects.equals(OACDIMDEAL.TABLE, tableDetail.getTableName())) {
                            loadDimensionDeal(tableDetail, cdcEvent, enrichedRecords);
                        }
                    }
                }
            }
        }

        return enrichedRecords;
    }

    /**
     * Loads dimension contact data from the given JSON objects into enriched records.
     *
     * @param tableDetail  ClickHouse table details for dimension contact.
     * @param emailInsight  JSON object containing contact data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    void loadDimensionContact(final TableDetail tableDetail, final JsonObject emailInsight, final Collection<EnrichedRecord> enrichedRecords) {
        final JsonObject cdcJson = emailInsight.getJsonObject(AnalyticsCdcKeys.AFTER);
        final JsonObject jsonObject = buildEntityJson(OACDIMCONTACT.TABLE, tableDetail, emailInsight, cdcJson);

        if (Objects.isNull(jsonObject)) {
            return;
        }

        final long orgId = cdcJson.getLong(MCOCONTACT.ORG_ID);

        secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
            final Long emailMessageCount = cdcJson.getLong(OMCRMENTITYINSIGHTS.EMAIL_MESSAGE_COUNT);
            final Long lastEmailSent = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_SENT);
            final Long lastEmailReceived = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_RECEIVED);

            jsonObject.put(OACDIMCONTACT.EMAIL_MESSAGE_COUNT, Objects.isNull(emailMessageCount) ? NumericalConstant.ZERO : emailMessageCount);

            if (Objects.nonNull(lastEmailSent)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_SENT, TemporalServiceProvider.get(lastEmailSent).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }

            if (Objects.nonNull(lastEmailReceived)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_RECEIVED, TemporalServiceProvider.get(lastEmailReceived).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }
        });

        jsonObject.put(AnalyticsCdcKeys.TOPIC, tableDetail.getTableName().toLowerCase());
        enrichedRecords.add(new EnrichedRecord(emailInsight.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID), jsonObject));
    }

    /**
     * Loads dimension company data from the given JSON objects into enriched records.
     *
     * @param tableDetail  ClickHouse table details for dimension company.
     * @param emailInsight  JSON object containing deal data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void buildDimensionCompany(final TableDetail tableDetail, final JsonObject emailInsight, final Collection<EnrichedRecord> enrichedRecords) {
        final JsonObject cdcJson = emailInsight.getJsonObject(AnalyticsCdcKeys.AFTER);
        final long orgId = cdcJson.getLong(MCOCOMPANY.ORG_ID);
        final JsonObject jsonObject = buildEntityJson(OACDIMCOMPANY.TABLE, tableDetail, emailInsight, cdcJson);

        if (Objects.isNull(jsonObject)) {
            return;
        }

        secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
            final Long emailMessageCount = cdcJson.getLong(OMCRMENTITYINSIGHTS.EMAIL_MESSAGE_COUNT);
            final Long lastEmailSent = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_SENT);
            final Long lastEmailReceived = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_RECEIVED);

            jsonObject.put(OACDIMCONTACT.EMAIL_MESSAGE_COUNT, Objects.isNull(emailMessageCount) ? NumericalConstant.ZERO : emailMessageCount);

            if (Objects.nonNull(lastEmailSent)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_SENT, TemporalServiceProvider.get(lastEmailSent).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }

            if (Objects.nonNull(lastEmailReceived)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_RECEIVED, TemporalServiceProvider.get(lastEmailReceived).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }
        });

        jsonObject.put(AnalyticsCdcKeys.TOPIC, tableDetail.getTableName().toLowerCase());
        enrichedRecords.add(new EnrichedRecord(emailInsight.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID), jsonObject));
    }

    /**
     * Loads dimension deal data from the given JSON objects into enriched records.
     *
     * @param tableDetail  ClickHouse table details for dimension deal.
     * @param emailInsight  JSON object containing deal data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void loadDimensionDeal(final TableDetail tableDetail, final JsonObject emailInsight, final Collection<EnrichedRecord> enrichedRecords) {
        final JsonObject cdcJson = emailInsight.getJsonObject(AnalyticsCdcKeys.AFTER);
        final JsonObject jsonObject = buildEntityJson(OACDIMDEAL.TABLE, tableDetail, emailInsight, cdcJson);

        if (Objects.isNull(jsonObject)) {
            return;
        }

        final long orgId = cdcJson.getLong(OMCRMENTITYINSIGHTS.ORG_ID);

        secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
            final Long emailMessageCount = cdcJson.getLong(OMCRMENTITYINSIGHTS.EMAIL_MESSAGE_COUNT);
            final Long lastEmailSent = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_SENT);
            final Long lastEmailReceived = cdcJson.getNullableValue(OMCRMENTITYINSIGHTS.LAST_EMAIL_RECEIVED);

            jsonObject.put(OACDIMCONTACT.EMAIL_MESSAGE_COUNT, Objects.isNull(emailMessageCount) ? NumericalConstant.ZERO : emailMessageCount);

            if (Objects.nonNull(lastEmailSent)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_SENT, TemporalServiceProvider.get(lastEmailSent).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }

            if (Objects.nonNull(lastEmailReceived)) {
                jsonObject.put(OACDIMCONTACT.LAST_EMAIL_RECEIVED, TemporalServiceProvider.get(lastEmailReceived).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
            }
        });

        jsonObject.put(AnalyticsCdcKeys.TOPIC, tableDetail.getTableName().toLowerCase());
        enrichedRecords.add(new EnrichedRecord(emailInsight.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID), jsonObject));
    }

    /**
     * Loads dimension deal data from the given JSON objects into enriched records.
     *
     * @param tableName         ClickHouse table name for dimension deal.
     * @param tableDetail       ClickHouse table details for dimension deal.
     * @param emailInsight      JSON object containing deal data.
     * @param cdcJson           JSON object containing CDC data.
     */
    private JsonObject buildEntityJson(final String tableName, final TableDetail tableDetail, final JsonObject emailInsight,
                                       final JsonObject cdcJson) {
        final Table table = OlapTable.get(tableDetail.getTableName());
        final Long entityId = cdcJson.getLong(OMCRMENTITYINSIGHTS.ENTITY_ID);

        if (Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, emailInsight.getString(AnalyticsCdcKeys.OPERATION))) {
             return getRowJson(tableResolver.resolve(tableName), emailInsight.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MDODEAL.ID));
        } else {
            final JsonObject entityJson = getRowJson(table, entityId);

            return Objects.isNull(entityJson) ? null : entityJson;
        }
    }
}
