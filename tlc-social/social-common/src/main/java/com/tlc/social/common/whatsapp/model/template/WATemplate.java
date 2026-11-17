package com.tlc.social.common.whatsapp.model.template;

import com.tlc.commons.code.ErrorCode;

import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.commons.language.Language;
import com.tlc.social.common.model.SocialEntity;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A model which represents the Whatsapp message template
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplate implements SocialEntity {

    private Long id;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.external.id.not.found")
    private Long externalTemplateId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.name.not.found")
    @Size(max = 512, groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.name.length.exceeded")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.category.not.found")
    private Category category;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.sub.category.not.found")
    private TemplateSubCategory templateSubCategory;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.parameter.format.not.found")
    private ParameterFormat parameterFormat;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.status.not.found")
    private Status status;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.language.not.found")
    private Language language;

    @Valid
    private Header header;

    @Valid
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.body.not.found")
    private Body body;

    @Valid
    private Footer footer;

    @Valid
    private Collection<Button> buttons;
    private Long createdAt;
    private Long updatedAt;

    public Header getHeader() {
        return header;
    }

    public void setHeader(final Header header) {
        this.header = header;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(final Body body) {
        this.body = body;
    }

    public Footer getFooter() {
        return footer;
    }

    public void setFooter(final Footer footer) {
        this.footer = footer;
    }

    public Collection<Button> getButtons() {
        return buttons;
    }

    public void addButton(final Button button) {
        this.buttons = Objects.isNull(buttons) ? new ArrayList<>() : buttons;

        buttons.add(button);
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(final Language language) {
        this.language = language;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getExternalTemplateId() {
        return externalTemplateId;
    }

    public void setExternalTemplateId(final Long externalTemplateId) {
        this.externalTemplateId = externalTemplateId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public ParameterFormat getParameterFormat() {
        return parameterFormat;
    }

    public void setParameterFormat(final ParameterFormat parameterFormat) {
        this.parameterFormat = parameterFormat;
    }

    public TemplateSubCategory getTemplateSubCategory() {
        return templateSubCategory;
    }

    public void setTemplateSubCategory(final TemplateSubCategory templateSubCategory) {
        this.templateSubCategory = templateSubCategory;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * {@inheritDoc}
     *
     * @return the id
     */
    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(SocialErrorCodes.ORG_ID_NOT_SUPPORTED, "i18n.social.model.org.id.not.supported");
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * An enum which represents the category of the template
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum Category {

        MARKETING((short) 1),
        UTILITY((short) 2),
        AUTHENTICATION((short) 3);

        private final short id;

        Category(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Category based on the provided id}
         * </p>
         *
         * @param id Represents the id of the category
         * @return the {@link Category}
         */
        public static Category getById(final short id) {
            return switch (id) {
                case 1 -> MARKETING;
                case 2 -> UTILITY;
                case 3 -> AUTHENTICATION;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_CATEGORY, "i18n.social.whatsapp.template.unsupported.category");
            };
        }
    }

    /**
     * An enum which represents the sub category of the utility template
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum UtilitySubCategory implements TemplateSubCategory {

        CUSTOM((short) 1),
        CALL_PERMISSIONS_REQUEST((short) 2);

        private final short id;

        UtilitySubCategory(final short id) {
            this.id = id;
        }

        /**
         * {@inheritDoc}
         *
         * @return the id
         */
        @Override
        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link UtilitySubCategory based on the provided id}
         * </p>
         *
         * @param id Represents the id of the sub category
         * @return the {@link UtilitySubCategory}
         */
        public static UtilitySubCategory getById(final short id) {
            return switch (id) {
                case 1 -> CUSTOM;
                case 2 -> CALL_PERMISSIONS_REQUEST;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_SUB_CATEGORY, "i18n.social.whatsapp.template.unsupported.sub.category");
            };
        }
    }

    /**
     * An enum which represents the sub category of the marketing template
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum MarketingSubCategory implements TemplateSubCategory {

        CUSTOM((short) 1),
        CATALOGUE((short) 2),
        CALL_PERMISSIONS_REQUEST((short) 3);

        private final short id;

        MarketingSubCategory(final short id) {
            this.id = id;
        }

        /**
         * {@inheritDoc}
         *
         * @return the id
         */
        @Override
        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link MarketingSubCategory based on the provided id}
         * </p>
         *
         * @param id Represents the id of the sub category
         * @return the {@link MarketingSubCategory}
         */
        public static MarketingSubCategory getById(final short id) {
            return switch (id) {
                case 1 -> CUSTOM;
                case 2 -> CATALOGUE;
                case 3 -> CALL_PERMISSIONS_REQUEST;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_SUB_CATEGORY, "i18n.social.whatsapp.template.unsupported.sub.category");
            };
        }
    }

    /**
     * An enum which represents the parameter format in the template
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum ParameterFormat {

        NAMED((short) 1),
        POSITIONAL((short) 2);

        private final short id;

        ParameterFormat(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link ParameterFormat based on the provided id}
         * </p>
         *
         * @param id Represents the id of the parameter format
         * @return the {@link ParameterFormat}
         */
        public static ParameterFormat getById(final short id) {
            return switch (id) {
                case 1 -> NAMED;
                case 2 -> POSITIONAL;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_PARAM_FORMAT, "i18n.social.whatsapp.template.unsupported.parameter.format");
            };
        }
    }

    /**
     * An enum which represents the status of the template
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum Status {

        PENDING((short) 1),
        APPROVED((short) 2),
        REJECTED((short) 3),
        PAUSED((short) 4),
        DISABLED((short) 5),
        ARCHIVED((short) 6),
        FLAGGED((short) 7),
        IN_APPEAL((short) 8),
        LIMIT_EXCEEDED((short) 9),
        REINSTATED((short) 10),
        PENDING_DELETION((short) 11),
        LOCKED((short) 12),
        ;

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Status based on the provided id}
         * </p>
         *
         * @param id Represents the id of the status
         * @return the {@link Status}
         */
        public static Status getById(final short id) {
            return switch (id) {
                case 1 -> PENDING;
                case 2 -> APPROVED;
                case 3 -> REJECTED;
                case 4 -> PAUSED;
                case 5 -> DISABLED;
                case 6 -> ARCHIVED;
                case 7 -> FLAGGED;
                case 8 -> IN_APPEAL;
                case 9 -> LIMIT_EXCEEDED;
                case 10 -> REINSTATED;
                case 11 -> PENDING_DELETION;
                case 12 -> LOCKED;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_STATUS, "i18n.social.whatsapp.template.unsupported.status");
            };
        }
    }
}