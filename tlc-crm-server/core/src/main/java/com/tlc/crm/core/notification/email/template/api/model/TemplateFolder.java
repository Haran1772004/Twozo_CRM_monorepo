package com.tlc.crm.core.notification.email.template.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.notification.email.template.status.TemplateFolderErrorCodes;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/**
 * <p>
 * A model represents the {@link TemplateFolder}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateFolder implements EmailNotificationModel {

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.folder.invalid.name")
    @Size(max = 50, groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.folder.name.max.length")
    private String name;
    private AccessType accessType;
    private Collection<EntityMailTemplate> templates;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.email.template.folder.invalid.ownerId")
    private long ownerId;
    private Visibility visibility;
    private boolean preventShare;
    private Collection<CrmUser> crmUsers;
    private Collection<Team> teams;
    private Collection<Territory> territories;
    private CrmUser owner;
    private Long createdTime;

    public TemplateFolder() {
    }

    /**
     * <p>
     * Represents the different types of access levels
     * </p>
     *
     * @author PandiarajkumarG
     * @version 1.0
     */
    public enum AccessType {
        CAN_VIEW((short) 1),
        CAN_EDIT((short) 2);

        private final short id;

        AccessType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static AccessType getAccessTypeById(final short id) {
            return switch (id) {
                case 1 -> CAN_VIEW;
                case 2 -> CAN_EDIT;
                default -> throw ErrorCode.get(TemplateFolderErrorCodes.INVALID_ACCESS_TYPE,
                        "i18n.template.folder.invalid.access.type");
            };
        }
    }

    /**
     * <p>
     * Represents the different filter actions for folder
     * </p>
     */
    public enum FilterAction {
        ALL("allTemplates"),
        CREATED_BY_ME("createdByMe"),
        SHARED_WITH_ME("sharedWithMe");

        private final String type;

        FilterAction(final String type) {
            this.type = type;
        }

        public String getFilterActionType() {
            return type;
        }

        public static FilterAction getFilterAction(final short id) {
            return switch (id) {
                case 1 -> ALL;
                case 2 -> CREATED_BY_ME;
                case 3 -> SHARED_WITH_ME;
                default -> throw ErrorCode.get(TemplateFolderErrorCodes.INVALID_FILTER_TYPE,
                        "i18n.template.folder.invalid.filter.type");
            };
        }
    }

    /**
     * <p>
     * Represents the template folder visibility
     * </p>
     *
     * @author Venkatesh Nambiyar
     * @version 1.0
     */
    public enum Visibility {
        JUST_ME((short) 1, "justMe"),
        EVERYONE((short) 2, "everyOne"),
        DESIGNATED((short) 3, "designated");

        private final short id;
        private final String name;

        Visibility(final short id, final String name) {
            this.id = id;
            this.name = name;
        }

        public short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * <p>
         * Retrieves a visibility based on the provided name
         * </p>
         *
         * @param name Represents the visibility name
         * @return the {@link Visibility}
         */
        public static Visibility getVisibility(final String name) {
            return switch (name.toLowerCase()) {
                case "justme" -> JUST_ME;
                case "everyone" -> EVERYONE;
                case "designated" -> DESIGNATED;
                default -> throw ErrorCode.get(TemplateFolderErrorCodes.INVALID_VISIBILITY_TYPE,
                        "i18n.template.folder.invalid.visibility.type");
            };
        }

        /**
         * <p>
         * Retrieves a visibility based on the provided id
         * </p>
         *
         * @param id Represents the visibility id
         * @return the {@link Visibility}
         */
        public static Visibility get(final short id) {
            return switch (id) {
                case 1 -> JUST_ME;
                case 2 -> EVERYONE;
                case 3 -> DESIGNATED;
                default -> throw ErrorCode.get(TemplateFolderErrorCodes.INVALID_VISIBILITY_TYPE,
                        "i18n.template.folder.invalid.visibility.type");
            };
        }
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setAccessType(final AccessType accessType) {
        this.accessType = accessType;
    }

    public void setTemplates(final Collection<EntityMailTemplate> templates) {
        this.templates = templates;
    }

    public void setOwnerId(final long ownerId) {
        this.ownerId = ownerId;
    }

    public void setVisibility(final Visibility visibility) {
        this.visibility = visibility;
    }

    public void setPreventShare(final boolean preventShare) {
        this.preventShare = preventShare;
    }

    public void setCrmUsers(final Collection<CrmUser> crmUsers) {
        this.crmUsers = crmUsers;
    }

    public void setTeams(final Collection<Team> teams) {
        this.teams = teams;
    }

    public void setTerritories(final Collection<Territory> territories) {
        this.territories = territories;
    }

    public void setOwner(final CrmUser owner) {
        this.owner = owner;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public Collection<EntityMailTemplate> getTemplates() {
        return templates;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public String getName() {
        return name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean preventShare() {
        return preventShare;
    }

    public Collection<CrmUser> getCrmUsers() {
        return crmUsers;
    }

    public Collection<Team> getTeams() {
        return teams;
    }

    public Collection<Territory> getTerritories() {
        return territories;
    }

    public CrmUser getOwner() {
        return owner;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
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
}
