package com.tlc.crm.core.features.action.call.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;

import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.converter.CallLogConverter;
import com.tlc.crm.core.field.call.CallLogFields;

import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.security.permission.annotation.authorizer.PhoneAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.common.models.User;
import com.tlc.sql.common.api.dml.Operator;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;

/**
 * This class handles the management of call log, including fetching call details and listing call records.
 * It extends {@link CrmConfigAction} to provide CRUD services.
 *
 * @author Sathish
 * @version 1.0
 */
@PhoneAuthorizer(subCategories = SubCategory.Type.CALL_LOGS)
@WebAction(path = "/call/log/mgmt")
public class CallLogManagement extends AsyncBulkConfigAction<CallLog> {

    private static final String ACTION_NAME = "callLog";
    private final CallLogManager callLogManager;
    private final CallLogConverter callLogConverter;

    public CallLogManagement() {
        callLogManager = CallLogManager.getInstance();
        callLogConverter = CallLogConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final String type = requestJson.getString(FieldKeys.TYPE.getName());

        switch (type) {
            case "list" -> {
                final String criteriaKey = CallLogFields.CRITERIA.getName();
                final String searchText = data.optString(CallLogFields.SEARCH_TEXT.getName(), null);
                final int start = data.getInt(CallLogFields.START.getName());
                final CriteriaDetailsHolder criteriaDetailsHolder = data.containsKey(criteriaKey)
                        ? buildCriteriaDetails(data.getJsonArray(criteriaKey)) : new CriteriaDetailsHolder();
                final long totalCount = callLogManager.getTotalCount(orgId, searchText, criteriaDetailsHolder);
                final JsonArray jsonArray = Json.array();

                if (start <= totalCount) {
                    final int limit = data.getInt(CallLogFields.LIMIT.getName());
                    final LimitConfig limitConfig = new LimitConfig(start, limit);
                    final Collection<CallLog> callLogs = callLogManager.get(orgId, searchText, criteriaDetailsHolder, limitConfig);
                    final ConverterConfigBuilder converterConfigBuilder = ConverterConfigBuilder.getInstance();

                    for (final CallLog callLog : callLogs) {
                        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, null);

                        jsonArray.put(callLogConverter.convert(converterConfig, callLog));
                    }
                    crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalCount);
                } else {
                    crmResponse.put(FieldKeys.HAS_MORE.getName(), false);
                }
                crmResponse.put(FieldKeys.LIST.getName(), jsonArray);
                crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalCount);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<CallLog> getConfigManager() {
        return callLogManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallLog convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = new ConverterConfig(request.orgId(), request.userId(), request.locale(), false, jsonObject);

        return callLogConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallLog convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = new ConverterConfig(request.orgId(), request.userId(), request.locale(), true, jsonObject);

        return callLogConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final CallLog callLog) {
        final ConverterConfig converterConfig = ConverterConfigBuilder.getInstance().build(request, false, null);

        return callLogConverter.convert(converterConfig, callLog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUser(final CallLog callLog, final Collection<User> users) {
        final CrmUser createdBy = callLog.getCreatedBy();

        for (final User user : users) {
            if (Objects.equals(createdBy.getIamId(), user.id())) {
                createdBy.setUser(user);
            }
        }
    }

    /**
     * Builds a collection of filtering criteria for call history retrieval.
     *
     * @param criteriaJson JSON array containing field-value pairs for filtering.
     * @return A {@link CriteriaDetailsHolder} objects to be used for filtering.
     */
    private CriteriaDetailsHolder buildCriteriaDetails(final JsonArray criteriaJson) {
        final CriteriaDetailsHolder criteriaDetailsHolder = new CriteriaDetailsHolder();
        final int size = criteriaJson.size();

        for (int index = 0; index < size; index++) {
            final JsonObject criteria = criteriaJson.getJsonObject(index);
            final String field = criteria.getString(FieldKeys.FIELD.getName());
            final Object value = criteria.opt(FieldKeys.VALUE.getName());
            final Operator operator = Objects.isNull(value) ? Operator.NOT_EQUAL : Operator.EQUAL;

            criteriaDetailsHolder.addCriteria(new CriteriaDetails(field, value, operator, criteria.getBoolean(FieldKeys.IS_MANDATORY.getName())));
        }
        return criteriaDetailsHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigActionName() {
        return ACTION_NAME;
    }
}
