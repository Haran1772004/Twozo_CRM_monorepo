package com.tlc.crm.entity.search.api.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Model which represents the viewed entries during search
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see TlcModel
 */
public class ViewedEntries implements TlcModel {

    @NotNull(groups = {Group.Create.class}, message = "i18n.search.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class}, message = "i18n.search.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.search.user.invalid.id")
    private Long userId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.search.index.invalid")
    private String index;

    @NotNull(groups = {Group.Create.class}, message = "i18n.search.text.invalid")
    private String searchText;

    public Long getUserId() {
        return userId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(final String index) {
        this.index = index;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Object identity() {
        return null;
    }
}