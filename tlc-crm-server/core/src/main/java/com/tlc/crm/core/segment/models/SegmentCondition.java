package com.tlc.crm.core.segment.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.internal.status.SegmentErrorCodes;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * <p>
 * A model that represents the ordered segment condition row.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentCondition {

    @NotNull
    private Type type;
    @NotNull
    private FilterConnector connector;
    @Valid
    private SegmentConditionCriteria conditionCriteria;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public FilterConnector getConnector() {
        return connector;
    }

    public void setConnector(final FilterConnector connector) {
        this.connector = connector;
    }

    public SegmentConditionCriteria getConditionCriteria() {
        return conditionCriteria;
    }

    public void setConditionCriteria(final SegmentConditionCriteria conditionCriteria) {
        this.conditionCriteria = conditionCriteria;
    }

    /**
     * <p>
     * Represents the type of entity a segment condition is evaluated against.
     * Each type belongs to a {@link Group} used for display/categorization purposes.
     * </p>
     *
     * @author Mohamed Yasar K
     */
    public enum Type {

        /**
         * <p>
         * Condition evaluated against a contact's property.
         * </p>
         */
        CONTACT_PROPERTY((short) 1, Group.PROPERTIES),

        /**
         * <p>
         * Condition evaluated against a company's property.
         * </p>
         */
        COMPANY_PROPERTY((short) 2, Group.PROPERTIES),
        ;

        private final short id;
        private final Group group;

        Type(final short id, final Group group) {
            this.id = id;
            this.group = group;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Resolves a {@link Type} from its numeric identifier.
         * </p>
         *
         * @param id the numeric identifier of the type
         * @return the matching {@link Type}
         */
        public static Type get(final short id) {
            return switch (id) {
                case 1 -> CONTACT_PROPERTY;
                case 2 -> COMPANY_PROPERTY;
                default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_SEGMENT_TYPE);
            };
        }

        public Group getGroup() {
            return group;
        }

        /**
         * <p>
         * Resolves the {@link Source} corresponding to a given {@link Type}.
         * </p>
         *
         * @param type the segment condition type, may be {@code null}
         * @return the matching {@link Source}, or {@code null} if {@code type} is {@code null}
         */
        public static Source toSource(final Type type) {
            if (Objects.isNull(type)) {
                return null;
            }
            return switch (type) {
                case CONTACT_PROPERTY -> Source.get(Source.CONTACT.getId());
                case COMPANY_PROPERTY -> Source.get(Source.COMPANY.getId());
            };
        }

        /**
         * <p>
         * Builds a display-friendly name by appending the localized group name
         * to the given base name.
         * </p>
         *
         * @param name                  the base name to append the group label to
         * @param i18nResolutionAdapter the adapter used to resolve the group's i18n key
         * @return the base name suffixed with the localized group name
         */
        public String getAlternateName(final String name, final I18nResolutionAdapter i18nResolutionAdapter) {
            return i18nResolutionAdapter.get(name) + StringUtil.Delimiter.EMPTY_SPACE.getDelimiter() + i18nResolutionAdapter.get(this.group.i18nKey);
        }

        /**
         * <p>
         * Checks whether this type represents a Contact Property condition.
         * </p>
         *
         * @return {@code true} if this type is {@link #CONTACT_PROPERTY};
         * {@code false} otherwise
         */
        public boolean isContactProperty() {
            return this == CONTACT_PROPERTY;
        }

        /**
         * <p>
         * Checks whether this type represents a Company Property condition.
         * </p>
         *
         * @return {@code true} if this type is {@link #COMPANY_PROPERTY};
         * {@code false} otherwise
         */
        public boolean isCompanyProperty() {
            return this == COMPANY_PROPERTY;
        }
    }

    /**
     * <p>
     * Represents a logical grouping of {@link Type} values, used for
     * categorizing and displaying condition types together.
     * </p>
     *
     * @author Mohamed Yasar K
     */
    public enum Group {

        /**
         * <p>
         * Group containing all property-based condition types
         * (e.g. contact and company properties).
         * </p>
         */
        PROPERTIES("properties", new I18nKey("i18n.segment.group.property.name")),
        ;

        private final String name;
        private final I18nKey i18nKey;

        Group(final String name, final I18nKey i18nKey) {
            this.name = name;
            this.i18nKey = i18nKey;
        }

        public String getName() {
            return name;
        }

        public I18nKey getI18nKey() {
            return i18nKey;
        }
    }
}