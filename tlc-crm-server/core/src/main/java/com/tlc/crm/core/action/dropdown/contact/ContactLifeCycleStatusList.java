package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.LifeCycleStatusFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages contact {@link LifeCycleStage} related dropdown actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "contactLifeCycleStatusList")
public class ContactLifeCycleStatusList extends AbstractDropdownAction {

    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final ContactManager contactManager;

    public ContactLifeCycleStatusList() {
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        contactManager = ContactManager.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link OptionContainer} for the requested dropdown values.
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final JsonObject data = dropdownRequest.data();
        final long contactId = data.getLong(LifeCycleStatusFields.CONTACT_ID.getName());
        final long lifeCycleStageId = data.getLong(LifeCycleStatusFields.LIFE_CYCLE_STAGE_ID.getName());
        final long totalRecords = lifeCycleStatusManager.getTotalRecordsCount(orgId, lifeCycleStageId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Optional<LifeCycleStatus> contactLifeCycleStatus = contactManager.getContactLifeCycleStatus(orgId, contactId, lifeCycleStageId);
            final Long contactLifeCycleStatusId = contactLifeCycleStatus.map(LifeCycleStatus::id).orElseGet(() -> null);
            final List<LifeCycleStatus> lifeCycleStatuses = new ArrayList<>(lifeCycleStatusManager.getLifeCycleStatusDropdownList(orgId,
                    lifeCycleStageId, dropdownLimit, dropdownCriteria));

            if (contactLifeCycleStatus.isPresent() && !lifeCycleStatuses.isEmpty() && lifeCycleStatuses.stream()
                    .map(LifeCycleStatus::id).noneMatch(id -> id.equals(contactLifeCycleStatusId))) {
                lifeCycleStatuses.remove(lifeCycleStatuses.size() - 1);
                lifeCycleStatuses.add(1, contactLifeCycleStatus.get());
            }
            final String valueKey = LifeCycleStatusFields.VALUE.getName();
            final String nameKey = LifeCycleStatusFields.NAME.getName();
            final String associatedKey = LifeCycleStatusFields.IS_ASSOCIATED.getName();
            final String labelKey = LifeCycleStatusFields.LABEL.getName();
            final String defaultKey = LifeCycleStatusFields.IS_DEFAULT.getName();
            boolean isDefaultStatusLoaded = false;

            for (final LifeCycleStatus lifeCycleStatus : lifeCycleStatuses) {
                final JsonObject lifeCycleStatusJson = Json.object();
                final Long id = lifeCycleStatus.id();

                if (Objects.equals(id, contactLifeCycleStatusId)) {
                    lifeCycleStatusJson.put(associatedKey, true);
                }
                lifeCycleStatusJson.put(valueKey, id);
                lifeCycleStatusJson.put(nameKey, lifeCycleStatus.getName());
                final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

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
        optionContainer.setTotalRecords(totalRecords);
        return optionContainer;
    }
}
