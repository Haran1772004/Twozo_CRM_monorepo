package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;
import com.tlc.crm.core.field.contact.LifeCycleStatusFields;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Manages {@link LifeCycleStatus} related dropdown actions.
 *
 * @author Sathish
 * @version 1.0
 */
@Dropdown(name = "lifeCycleStatusList", fieldAlias = "lifeCycleStatus")
public class LifeCycleStatusList implements DropdownAction {

    private final LifeCycleStatusManager lifeCycleStatusManager;

    public LifeCycleStatusList() {
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final JsonObject data = dropdownRequest.data();
        final String dependentIdKey = LifeCycleStatusFields.DEPENDENT_ID.getName();
        final Long lifeCycleStageId = data.containsKey(dependentIdKey) ? data.getLong(dependentIdKey) : null;
        final Collection<LifeCycleStatus> lifeCycleStatuses = lifeCycleStatusManager.getLifeCycleStatusDropdownList(orgId,
                lifeCycleStageId, dropdownRequest.getLimit(), criteria);
        final OptionContainer optionContainer = new OptionContainer();

        if (!lifeCycleStatuses.isEmpty()) {
            final String valueKey = LifeCycleStatusFields.VALUE.getName();
            final String nameKey = LifeCycleStatusFields.NAME.getName();
            final String labelKey = LifeCycleStatusFields.LABEL.getName();
            final String defaultKey = LifeCycleStatusFields.IS_DEFAULT.getName();
            boolean isDefaultStatusLoaded = false;

            if (CrmRequest.ClientType.REST_API == dropdownRequest.getClientType()) {
                final String idKey = LifeCycleStageFields.ID.getName();

                for (final LifeCycleStatus lifeCycleStatus : lifeCycleStatuses) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(nameKey, lifeCycleStatus.getName()).put(idKey, lifeCycleStatus.id());
                    optionContainer.addOption(jsonObject);
                }
            } else {
                for (final LifeCycleStatus lifeCycleStatus : lifeCycleStatuses) {
                    final JsonObject lifeCycleStatusJson = Json.object();
                    final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

                    lifeCycleStatusJson.put(valueKey, lifeCycleStatus.id());
                    lifeCycleStatusJson.put(nameKey, lifeCycleStatus.getName());

                    if (Objects.nonNull(label)) {
                        lifeCycleStatusJson.put(labelKey, label.getId());
                    }

                    if (!isDefaultStatusLoaded) {
                        lifeCycleStatusJson.put(defaultKey, true);
                        isDefaultStatusLoaded = true;
                    }
                    optionContainer.addOption(lifeCycleStatusJson);
                }
            }
        }
        optionContainer.setTotalRecords(lifeCycleStatusManager.getTotalRecordsCount(orgId, lifeCycleStageId, criteria));
        return optionContainer;
    }
}
