package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.core.api.contact.model.ContactDealInfo;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.entity.dropdown.DropdownCriteria;

/**
 * <p>
 * Represents the page handler for the {@link ContactDealPageHandler}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ContactDealPageHandler extends SourceMobilePageHandler<ContactDealInfo> {

    private final ParticipantManager participantManager;
    private final DropdownCriteria dropdownCriteria;

    public ContactDealPageHandler(final int start, final int limit, final DropdownCriteria dropdownCriteria) {
        this.recordStartingIndex = start;
        this.recordsPerPage = limit;
        this.dropdownCriteria = dropdownCriteria;
        this.participantManager = ParticipantManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page<ContactDealInfo>}
     */
    @Override
    public Page<ContactDealInfo> fetchPageData() {
        final Page<ContactDealInfo> page = new Page<>();

        page.setData(participantManager.getContactDealInfoList(getOrgId(), getEntityId(), getRecordStartingIndex(), getRecordsPerPage(), dropdownCriteria));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return participantManager.getNonPrimaryContactsCount(getOrgId(), getEntityId(), dropdownCriteria);
    }
}
