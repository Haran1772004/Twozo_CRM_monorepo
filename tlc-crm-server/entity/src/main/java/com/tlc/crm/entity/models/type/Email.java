package com.tlc.crm.entity.models.type;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents an email data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class Email implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = Group.Delete.class, message = "i18n.entity.field.type.emailId.required")
    private Long id;

    @AuditEntityName
    @AuditField(aliasName = "value", displayName = "i18n.contact.fields.emails")
    @EmailAlertField(
            name = "email",
            fieldType = EmailAlertFieldType.NUMBER,
            placeholderTemplates = {EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION},
            recipientTemplates = {EmailNotificationTemplate.ACTIVITY_GUEST_INVITATION},
            requirePreviousValue = true
    )
    @SearchField(name = "value")
    @com.tlc.validator.type.Email(groups = {Group.Create.class, Group.Update.class})
    @NotEmpty(message = "i18n.entity.field.email.invalid.address", groups = {Group.Create.class, Group.Update.class})
    private String value;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.orgId.required")
    private Long orgId;

    @SearchField(name = "type")
    private Type type;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
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

    /**
     * <p>
     * Represents the email types
     * </p>
     */
    public enum Type implements OptionJsonConverter<Type> {

        WORK("work", 1, "i18n.entity.email.type.work"),
        PERSONAL("personal", 2, "i18n.entity.email.type.personal"),
        HOME("home", 3, "i18n.entity.email.type.home"),
        OTHER("other", 4, "i18n.entity.email.type.other");

        private final String name;
        private final int id;
        private final String displayName;

        Type(final String name, final int id, final String displayName) {
            this.name = Objects.requireNonNull(name);
            this.id = id;
            this.displayName = displayName;
        }

        /**
         * <p>
         * Gets the name of the email type
         * </p>
         *
         * @return the name of email type
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Gets the display name
         * </p>
         *
         * @return the display name of email type
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * <p>
         * Gets the email type id
         * </p>
         *
         * @return the type id
         */
        public int getId() {
            return this.id;
        }

        /**
         * Gets the {@link Type} by id
         *
         * @param id represents the id of the email type
         * @return {@link Type}
         */
        public static Type valueOf(final int id) {
            return switch (id) {
                case 2 -> PERSONAL;
                case 3 -> HOME;
                case 4 -> OTHER;
                default -> WORK;
            };
        }

        /**
         * Returns the {@link Email.Type} corresponding to the given string value.
         * If the value is {@code null} or does not match any supported type,
         * {@link Email.Type#WORK} is returned.
         *
         * @param value The type name to resolve
         * @return The matching {@link Email.Type}, or {@link Email.Type#WORK} if the value is invalid
         */
        public static Type get(final String value) {
            if (value == null) {
                return WORK;
            }

            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                return WORK;
            }
        }

        /**
         * Converts the specified {@code Type} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
         *
         * @param type            the {@code Type} object to be converted
         * @param converterConfig the {@code ConverterConfig} containing configuration details
         * @return a {@code JsonObject} representing the converted type with fields for value and name
         */
        @Override
        public JsonObject convertModelToOptionJson(final Type type, final ConverterConfig converterConfig) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(FieldKeys.VALUE.getName(), type.id);
            jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(type.displayName));

            return jsonObject;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Email) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }
}
