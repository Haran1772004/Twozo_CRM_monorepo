package com.tlc.crm.entity.models.type;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.model.country.Country;

import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents a phone data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class Phone implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = Group.Delete.class, message = "i18n.entity.field.type.phoneId.required")
    private Long id;

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.orgId.required")
    private Long orgId;

    @SearchField(name = "type")
    private Type type;
    private Country country;

    @AuditEntityName
    @AuditField(aliasName = "number", displayName = "i18n.contact.fields.phones")
    @com.tlc.validator.type.Phone(groups = {Group.Create.class, Group.Update.class})
    @SearchField(name = "number")
    @NotEmpty(message = "i18n.entity.field.phone.number.invalid", groups = {Group.Create.class, Group.Update.class})
    private String number;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    public String getNumberWithCode() {

        if (Objects.isNull(country)) {
            return number;
        }
        final String code = country.getCode();

        if (number.startsWith(code)) {
            return number;
        }
        return StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, code, number);
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
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(null == country ? null : country.getCode(), number, orgId);
    }

    /**
     *{@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Phone) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    /**
     * <p>
     * Represents the phone types
     * </p>
     */
    public enum Type implements OptionJsonConverter<Type> {

        WORK("work", 1, "i18n.entity.phone.type.work"),
        HOME("home", 2, "i18n.entity.phone.type.home"),
        MOBILE("mobile", 3, "i18n.entity.phone.type.mobile"),
        WHATSAPP("whatsapp", 4, "i18n.entity.phone.type.whatsapp"),
        TELEGRAM("telegram", 5, "i18n.entity.phone.type.telegram"),
        PERSONAL("personal", 6, "i18n.entity.phone.type.personal"),
        OTHER("other", 7, "i18n.entity.phone.type.other");

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
         * Gets the name of the phone type
         * </p>>
         *
         * @return the name of phone type
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
         * Gets the phone type id
         * </p>
         *
         * @return the type id
         */
        public int getId() {
            return this.id;
        }

        /**
         * Gets the {@link Type} by typeId
         *
         * @param id represents the id of the phone type
         * @return {@link Type}
         */
        public static Type valueOf(final int id) {
            return switch (id) {
                case 2 -> HOME;
                case 3 -> MOBILE;
                case 4 -> WHATSAPP;
                case 5 -> TELEGRAM;
                case 6 -> PERSONAL;
                case 7 -> OTHER;
                default -> WORK;
            };
        }

        /**
         * Returns the {@link Type} corresponding to the given string value.
         * If the value is {@code null} or does not match any supported type,
         * {@link Type#WORK} is returned.
         *
         * @param value The type name to resolve
         * @return The matching {@link Type}, or {@link Type#WORK} if the value is invalid
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
         * @param type the {@code Type} object to be converted
         * @param converterConfig the {@code ConverterConfig} containing configuration details.
         * @return a {@code JsonObject} representing the converted type, with fields for value and name
         */
        @Override
        public JsonObject convertModelToOptionJson(final Type type, final ConverterConfig converterConfig) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(FieldKeys.VALUE.getName(), type.id);
            jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(type.displayName));

            return jsonObject;
        }
    }
}
