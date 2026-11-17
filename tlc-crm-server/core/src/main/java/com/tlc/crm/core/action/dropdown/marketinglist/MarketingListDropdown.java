package com.tlc.crm.core.action.dropdown.marketinglist;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Manages the {@link MarketingList} related dropdown actions
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "marketingList")
public class MarketingListDropdown extends AbstractDropdownAction {

    private final MarketingListManager marketingListManager;
    private final MarketingListContactManager marketingListContactManager;

    public MarketingListDropdown() {
        marketingListManager = MarketingListManager.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
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
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final boolean includeContactCount = dropdownRequest.data().optBoolean(MarketingListFields.INCLUDE_CONTACT_COUNT, false);
        final long totalRecords = marketingListManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<MarketingList> marketingLists = marketingListManager.getDropdownList(orgId, dropdownLimit.getLimitClause(), dropdownCriteria);

            if (includeContactCount) {
                final Map<Long, Long> contactCountMap = marketingListContactManager.getContactCountMap(orgId, marketingLists.stream().map(MarketingList::id).toList());

                for (final MarketingList marketingList : marketingLists) {
                    final Long marketingListId = marketingList.id();
                    final JsonObject option = Json.object();

                    option.put(OptionKeys.VALUE, marketingListId);
                    option.put(OptionKeys.NAME, marketingList.getName());
                    option.put(MarketingListFields.CONTACT_COUNT, contactCountMap.getOrDefault(marketingListId, 0L));
                    optionContainer.addOption(option);
                }
            } else {
                for (final MarketingList marketingList : marketingLists) {
                    optionContainer.addOption(marketingList.id(), marketingList.getName());
                }
            }
        }

        return optionContainer;
    }
}