package com.tlc.crm.entity.search.fields;

import com.tlc.commons.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Preset data of source fields for index for mobile client.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class PresetIndexFieldsMobile implements PresetIndexFields {

    private static final String CONTACT_FIELD = CommonSearchFields.CONTACT;
    private static final String COMPANY_FIELD = CommonSearchFields.COMPANY;
    private static final String DEAL_FIELD = CommonSearchFields.DEAL;
    private static final Collection<String> CONTACT_ENTITY_REQUIRED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.ORG_ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.NAME));
    private static final Collection<String> COMPANY_ENTITY_REQUIRED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.ORG_ID),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.ID),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.NAME));
    private static final Collection<String> BASE_REQUIRED_FIELDS = List.of(CommonSearchFields.NAME, CommonSearchFields.ID,
            CommonSearchFields.ORG_ID, CommonSearchFields.OWNER_ID);

    private static final Collection<String> CONTACT_REQUIRED_FIELDS;
    private static final Collection<String> COMPANY_REQUIRED_FIELDS;
    private static final Collection<String> DEAL_REQUIRED_FIELDS;
    private static final Collection<String> ACTIVITY_REQUIRED_FIELDS;
    private static final Collection<String> PRODUCT_REQUIRED_FIELDS;

    static {
        CONTACT_REQUIRED_FIELDS = new ArrayList<>();
        CONTACT_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        CONTACT_REQUIRED_FIELDS.add(CommonSearchFields.PROFILE_URL);

        COMPANY_REQUIRED_FIELDS = new ArrayList<>();
        COMPANY_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        COMPANY_REQUIRED_FIELDS.addAll(List.of(CommonSearchFields.PROFILE_URL, CommonSearchFields.WEBSITE));

        DEAL_REQUIRED_FIELDS = new ArrayList<>();
        DEAL_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        DEAL_REQUIRED_FIELDS.addAll(List.of(CommonSearchFields.PIPELINE_STAGE, CommonSearchFields.DEAL_STATE,
                CommonSearchFields.DEAL_VALUE, CommonSearchFields.IS_ARCHIVED));

        PRODUCT_REQUIRED_FIELDS = new ArrayList<>();
        PRODUCT_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);

        ACTIVITY_REQUIRED_FIELDS = new ArrayList<>();
        ACTIVITY_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        ACTIVITY_REQUIRED_FIELDS.addAll( List.of(CommonSearchFields.START_TIME, CommonSearchFields.IS_DONE, CommonSearchFields.STATUS,
                CommonSearchFields.ACTIVITY_TYPE, CommonSearchFields.CHECK_IN_INFO, CommonSearchFields.CHECK_OUT_INFO,
                CommonSearchFields.CHECK_IN_TIME, CommonSearchFields.CHECK_OUT_TIME));
    }

    private static class Instance {
        private static final PresetIndexFieldsMobile INSTANCE = new PresetIndexFieldsMobile();
    }

    private PresetIndexFieldsMobile() {
    }

    /**
     * <p>
     * Gets the {@link PresetIndexFieldsMobile} instance.
     * </p>
     *
     * @return {@link PresetIndexFieldsMobile} instance.
     */
    public static PresetIndexFieldsMobile getInstance() {
        return PresetIndexFieldsMobile.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the required fields when user has global access to all entities.
     * </p>
     *
     * @param index Represents the index name.
     * @return the required fields.
     */
    public Collection<String> getGlobalAccessRequiredFields(final String index) {

        return switch (index.toLowerCase()) {
            case CONTACT_FIELD -> {
                final Collection<String> contactIndexRequiredFields = new ArrayList<>(CONTACT_REQUIRED_FIELDS);

                contactIndexRequiredFields.addAll(COMPANY_ENTITY_REQUIRED_FIELDS);

                yield contactIndexRequiredFields;
            }
            case DEAL_FIELD -> {
                final Collection<String> dealIndexRequiredFields = new ArrayList<>(DEAL_REQUIRED_FIELDS);

                dealIndexRequiredFields.addAll(CONTACT_ENTITY_REQUIRED_FIELDS);
                dealIndexRequiredFields.addAll(COMPANY_ENTITY_REQUIRED_FIELDS);

                yield dealIndexRequiredFields;
            }
            case COMPANY_FIELD -> COMPANY_REQUIRED_FIELDS;
            case CommonSearchFields.PRODUCT -> PRODUCT_REQUIRED_FIELDS;
            case CommonSearchFields.ACTIVITY -> ACTIVITY_REQUIRED_FIELDS;
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                final Collection<String> requiredFields = new ArrayList<>(BASE_REQUIRED_FIELDS);

                requiredFields.addAll(List.of(CommonSearchFields.ENTITY_SOURCE, CommonSearchFields.ENTITY_SOURCE_ID,
                        CommonSearchFields.ENTITY_SOURCE_NAME));

                yield requiredFields;
            }
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the required fields.
     * </p>
     *
     * @param index Represents the index name.
     * @return the required fields.
     */
    public Collection<String> getGlobalAccessIgnoredFields(final String index) {
        return null;
    }


    /**
     * <p>
     * Gets the required fields.
     * </p>
     *
     * @param index Represents the index name.
     * @return the required fields.
     */
    public Collection<String> getRequiredFields(final String index) {

        return switch (index) {
            case "contact" -> CONTACT_REQUIRED_FIELDS;
            case "deal" -> DEAL_REQUIRED_FIELDS;
            case "product" -> PRODUCT_REQUIRED_FIELDS;
            case "activity" -> ACTIVITY_REQUIRED_FIELDS;
            case "company" -> COMPANY_REQUIRED_FIELDS;
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the required fields for an entity.
     * </p>
     *
     * @param entity Represents the index name.
     * @return the entity fields.
     */
    public Collection<String> getEntityFields(final String entity) {

        return switch (entity.toLowerCase()) {
            case CONTACT_FIELD -> CONTACT_ENTITY_REQUIRED_FIELDS;
            case COMPANY_FIELD -> COMPANY_ENTITY_REQUIRED_FIELDS;
            default -> null;
        };
    }
}
