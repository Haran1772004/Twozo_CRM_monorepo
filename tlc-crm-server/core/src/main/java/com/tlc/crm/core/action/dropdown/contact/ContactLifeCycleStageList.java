package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;
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
@StaticDropdown(name = "contactLifeCycleStageList")
public class ContactLifeCycleStageList extends AbstractDropdownAction {

    private final LifeCycleStageManager lifeCycleStageManager;
    private final ContactManager contactManager;

    public ContactLifeCycleStageList() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
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
        final long contactId = data.getLong(LifeCycleStageFields.CONTACT_ID.getName());
        final long totalRecords = lifeCycleStageManager.getStagesCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Optional<LifeCycleStage> contactLifeCycleStage = contactManager.getContactLifeCycleStage(orgId, contactId);
            final Long contactLifeCycleStageId = contactLifeCycleStage.map(LifeCycleStage::id).orElseGet(() -> null);
            final List<LifeCycleStage> lifeCycleStages = new ArrayList<>(lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, false,
                    dropdownLimit, dropdownCriteria));

            if (contactLifeCycleStage.isPresent() && !lifeCycleStages.isEmpty() && lifeCycleStages.stream()
                    .map(LifeCycleStage::id).noneMatch(id -> id.equals(contactLifeCycleStageId))) {
                lifeCycleStages.remove(lifeCycleStages.size() - 1);
                lifeCycleStages.add(1, contactLifeCycleStage.get());
            }
            final String valueKey = LifeCycleStageFields.VALUE.getName();
            final String nameKey = LifeCycleStageFields.NAME.getName();
            final String associatedKey = LifeCycleStageFields.IS_ASSOCIATED.getName();

            for (final LifeCycleStage lifeCycleStage : lifeCycleStages) {
                final JsonObject lifeCycleStageJson = Json.object();
                final Long id = lifeCycleStage.id();

                if (Objects.equals(id, contactLifeCycleStageId)) {
                    lifeCycleStageJson.put(associatedKey, true);
                }
                lifeCycleStageJson.put(valueKey, id);
                lifeCycleStageJson.put(nameKey, lifeCycleStage.getName());
                optionContainer.addOption(lifeCycleStageJson);
            }
        }
        optionContainer.setTotalRecords(lifeCycleStageManager.getTotalRecordsCount(orgId, dropdownCriteria));
        return optionContainer;
    }
}
