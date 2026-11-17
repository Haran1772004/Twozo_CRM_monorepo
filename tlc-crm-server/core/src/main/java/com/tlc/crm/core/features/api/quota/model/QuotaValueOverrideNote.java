package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.validator.TlcModel;
import jakarta.validation.Valid;

/**
 * A model that represents the quota value override summary with note details
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaValueOverrideNote implements TlcModel {

    private Long id;
    private Long orgId;
    private Long summaryId;

    @Valid
    private final Note note;

    public QuotaValueOverrideNote(final Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }

    public Long getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(final Long summaryId) {
        this.summaryId = summaryId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }
}
