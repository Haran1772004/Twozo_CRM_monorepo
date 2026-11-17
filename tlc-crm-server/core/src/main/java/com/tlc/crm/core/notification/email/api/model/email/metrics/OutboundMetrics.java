package com.tlc.crm.core.notification.email.api.model.email.metrics;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * An entity that encapsulates the metrics data of an outbound email
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class OutboundMetrics implements TlcModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private int openedCount;
    private int clickedCount;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setOpenedCount(final int openedCount) {
        this.openedCount = openedCount;
    }

    public void setClickedCount(final int clickedCount) {
        this.clickedCount = clickedCount;
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
        return null;
    }

    public int getOpenedCount() {
        return openedCount;
    }

    public int getClickedCount() {
        return clickedCount;
    }
}
