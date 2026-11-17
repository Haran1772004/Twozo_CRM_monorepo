package com.tlc.crm.entity.filter.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the filter data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see TlcModel
 */
public class Filter implements TlcModel, Cloneable {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.filter.invalid.name")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.filter.invalid.name")
    private String name;
    private String aliasName;
    @Valid
    private Collection<FilterCriteria> filterCriteriaList;
    @Valid
    private FilterVisibility filterVisibility;
    @Valid
    private FilterProvider filterProvider;
    @Valid
    private CrmUser creator;
    private Permissions permissions;
    private final Category category;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private boolean favorite;
    private boolean isDefault;
    private boolean isRecentlyApplied;

    public Filter() {
        this(Category.COMMON);
    }
    
    public Filter(final Category category) {
        this.category = category;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;

        if (Objects.isNull(aliasName) && Objects.nonNull(id)) {
            aliasName = id.toString();
        }
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    public FilterProvider getFilterProvider() {
        return filterProvider;
    }

    public void setFilterProvider(final FilterProvider filterProvider) {
        this.filterProvider = filterProvider;
    }

    public Collection<FilterCriteria> getCriteriaList() {
        return filterCriteriaList;
    }

    public void setCriteriaList(final Collection<FilterCriteria> filterCriteriaList) {
        this.filterCriteriaList = filterCriteriaList;
    }

    public void addCriteria(final FilterCriteria filterCriteria) {
        this.filterCriteriaList = Objects.isNull(this.filterCriteriaList) ? new ArrayList<>() : this.filterCriteriaList;

        this.filterCriteriaList.add(filterCriteria);
    }

    public FilterVisibility getFilterVisibility() {
        return filterVisibility;
    }

    public void setFilterVisibility(final FilterVisibility filterVisibility) {
        this.filterVisibility = filterVisibility;
    }

    public CrmUser getCreator() {
        return creator;
    }

    public void setCreator(final CrmUser creator) {
        this.creator = creator;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(final Permissions permissions) {
        this.permissions = permissions;
    }

    public Category getCategory() {
        return category;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isRecentlyApplied() {
        return isRecentlyApplied;
    }

    public void setRecentlyApplied(final boolean isRecentlyUsed) {
        this.isRecentlyApplied = isRecentlyUsed;
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public Filter clone() {
        try {
            return (Filter) super.clone();
        } catch (CloneNotSupportedException e) {
            throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED, e);
        }
    }

    public record Permissions(boolean isEditable, boolean isDeletable, boolean isShareable) {

        public Permissions() {
            this(true, true, true);
        }
    }
    
    public enum Category {
        COMMON((short) 1), SALES_OWNER((short) 2), HOME_PAGE((short) 3);

        private final short id;

        Category(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static Category get(final short id) {
            return switch (id) {
                case 1 -> COMMON;
                case 2 -> SALES_OWNER;
                case 3 -> HOME_PAGE;
                default -> throw ErrorCode.get(FilterErrorCodes.FILTER_CATEGORY_NOT_FOUND, "i18n.filter.category.not.found");
            };
        }

        /**
         * <p>
         * Checks whether the filter category needs value resolution
         * </p>
         *
         * @return True if the filter need value resolution, false otherwise
         */
        public boolean needValueResolution() {
            return switch (this) {
                case COMMON, HOME_PAGE -> true;
                default -> false;
            };
        }

        /**
         * <p>
         * Checks whether the filter category is eligible for updating the recently applied filter.
         * </p>
         *
         * @return True if the category is eligible for updating the recently applied filter, false otherwise
         */
        public boolean isEligibleForRecentFilterUpdate() {
            return switch (this) {
                case COMMON, SALES_OWNER -> true;
                default -> false;
            };
        }
    }
}
