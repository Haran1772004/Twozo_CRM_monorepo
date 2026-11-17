package com.tlc.crm.entity.search.fields;

import com.tlc.commons.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Preset data of source fields for index for web client.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class PresetIndexFieldsWeb implements PresetIndexFields {

    private static final String CONTACT_FIELD = CommonSearchFields.CONTACT;
    private static final String COMPANY_FIELD = CommonSearchFields.COMPANY;
    private static final String DEAL_FIELD = CommonSearchFields.DEAL;
    private static final String CONTACT_COMPANY_ID_FIELD = StringUtil.joinStringsWithDot(CONTACT_FIELD, COMPANY_FIELD, CommonSearchFields.ID);
    private static final String CONTACT_COMPANY_NAME_FIELD = StringUtil.joinStringsWithDot(CONTACT_FIELD, COMPANY_FIELD, CommonSearchFields.NAME);
    private static final String DEAL_CONTACT_ID_FIELD = StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.ID);
    private static final String DEAL_CONTACT_NAME_FIELD = StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.NAME);
    private static final String DEAL_COMPANY_ID_FIELD = StringUtil.joinStringsWithDot(DEAL_FIELD, COMPANY_FIELD, CommonSearchFields.ID);
    private static final String DEAL_COMPANY_NAME_FIELD = StringUtil.joinStringsWithDot(DEAL_FIELD, COMPANY_FIELD, CommonSearchFields.NAME);
    private static final Collection<String> BASE_REQUIRED_FIELDS = List.of(CommonSearchFields.NAME, CommonSearchFields.ID,
            CommonSearchFields.ORG_ID, CommonSearchFields.OWNER_ID);
    private static final Collection<String> CONTACT_ENTITY_IGNORED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.OWNER_ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.TERRITORY_ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.PRIMARY_EMAIL),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.FOLLOWER_IDS));
    private static final Collection<String> COMPANY_ENTITY_IGNORED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.OWNER_ID),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.TERRITORY_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.FOLLOWER_IDS));
    private static final Collection<String> DEAL_ENTITY_IGNORED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.OWNER_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.TERRITORY_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.TERRITORY_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.OWNER_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, COMPANY_FIELD, CommonSearchFields.TERRITORY_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.OWNER_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CONTACT_FIELD, CommonSearchFields.FOLLOWER_IDS));
    private static final Collection<String> NOTE_FILE_REQUIRED_FIELDS = new ArrayList<>(BASE_REQUIRED_FIELDS);
    private static final Collection<String> COMPANY_ENTITY_REQUIRED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.ORG_ID),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.ID),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(COMPANY_FIELD, CommonSearchFields.WEBSITE));
    private static final Collection<String> CONTACT_ENTITY_REQUIRED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.ORG_ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.ID),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.EMAILS),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.PHONES),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.LINKEDIN),
            StringUtil.joinStringsWithDot(CONTACT_FIELD, CommonSearchFields.FACEBOOK));
    private static final Collection<String> DEAL_ENTITY_REQUIRED_FIELDS = List.of(
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.ORG_ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.ID),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.PIPELINE),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.PIPELINE_STAGE),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.DEAL_STATE),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.DEAL_VALUE),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.EXPECTED_CLOSE_DATE),
            StringUtil.joinStringsWithDot(DEAL_FIELD, CommonSearchFields.TOTAL_PRODUCT_COUNT));

    private static final Collection<String> CONTACT_REQUIRED_FIELDS;
    private static final Collection<String> COMPANY_REQUIRED_FIELDS;
    private static final Collection<String> DEAL_REQUIRED_FIELDS;
    private static final Collection<String> ACTIVITY_REQUIRED_FIELDS;
    private static final Collection<String> PRODUCT_REQUIRED_FIELDS;

    static {
        CONTACT_REQUIRED_FIELDS = new ArrayList<>();
        CONTACT_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        CONTACT_REQUIRED_FIELDS.addAll(List.of(CommonSearchFields.EMAILS, CommonSearchFields.PHONES,
                CommonSearchFields.ADDRESS, CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.FACEBOOK,
                CommonSearchFields.LINKEDIN, CommonSearchFields.NOTES));

        COMPANY_REQUIRED_FIELDS = new ArrayList<>();
        COMPANY_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        COMPANY_REQUIRED_FIELDS.addAll(List.of(CommonSearchFields.ADDRESS, CommonSearchFields.WEBSITE,
                CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NOTES));

        DEAL_REQUIRED_FIELDS = new ArrayList<>();
        DEAL_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        DEAL_REQUIRED_FIELDS.addAll( List.of(CommonSearchFields.DEAL_STATE, CommonSearchFields.DEAL_VALUE,
                CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.EXPECTED_CLOSE_DATE, CommonSearchFields.PIPELINE_STAGE,
                CommonSearchFields.PIPELINE, CommonSearchFields.TOTAL_PRODUCT_COUNT, CommonSearchFields.NOTES,
                CommonSearchFields.IS_ARCHIVED));

        PRODUCT_REQUIRED_FIELDS = new ArrayList<>();
        PRODUCT_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        PRODUCT_REQUIRED_FIELDS.add(CommonSearchFields.PRODUCT_CODE);

        ACTIVITY_REQUIRED_FIELDS = new ArrayList<>();
        ACTIVITY_REQUIRED_FIELDS.addAll(BASE_REQUIRED_FIELDS);
        ACTIVITY_REQUIRED_FIELDS.addAll(List.of(CommonSearchFields.END_TIME, CommonSearchFields.START_TIME,
                CommonSearchFields.IS_DONE, CommonSearchFields.STATUS, CommonSearchFields.ACTIVITY_TYPE, CommonSearchFields.NOTE));

        NOTE_FILE_REQUIRED_FIELDS.addAll(DEAL_ENTITY_REQUIRED_FIELDS);
    }

    private static class Instance {
        private static final PresetIndexFieldsWeb INSTANCE = new PresetIndexFieldsWeb();
    }

    private PresetIndexFieldsWeb() {
    }

    /**
     * <p>
     * Gets the {@link PresetIndexFieldsWeb} instance.
     * </p>
     *
     * @return {@link PresetIndexFieldsWeb} instance.
     */
    public static PresetIndexFieldsWeb getInstance() {
        return Instance.INSTANCE;
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

                addCompanyField(contactIndexRequiredFields);

                yield contactIndexRequiredFields;
            }
            case DEAL_FIELD -> {
                final Collection<String> dealIndexRequiredFields = new ArrayList<>(DEAL_REQUIRED_FIELDS);

                addRequiredContactAndCompanyFields(dealIndexRequiredFields);

                yield dealIndexRequiredFields;
            }
            case COMPANY_FIELD -> COMPANY_REQUIRED_FIELDS;
            case CommonSearchFields.PRODUCT -> PRODUCT_REQUIRED_FIELDS;
            case CommonSearchFields.ACTIVITY -> {
                final Collection<String> activityIndexRequiredFields = new ArrayList<>(ACTIVITY_REQUIRED_FIELDS);

                addRequiredEntitiesFields(activityIndexRequiredFields);

                yield activityIndexRequiredFields;
            }
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                final Collection<String> requiredFields = new ArrayList<>(BASE_REQUIRED_FIELDS);

                addRequiredEntitiesFields(requiredFields);

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
    public Collection<String> getRequiredFields(final String index) {

        return switch (index.toLowerCase()) {
            case CONTACT_FIELD -> CONTACT_REQUIRED_FIELDS;
            case DEAL_FIELD -> DEAL_REQUIRED_FIELDS;
            case COMPANY_FIELD -> COMPANY_REQUIRED_FIELDS;
            case CommonSearchFields.PRODUCT -> PRODUCT_REQUIRED_FIELDS;
            case CommonSearchFields.ACTIVITY -> ACTIVITY_REQUIRED_FIELDS;
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> NOTE_FILE_REQUIRED_FIELDS;
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the ignored fields when user has global access to all entities.
     * </p>
     *
     * @param index Represents the index name.
     * @return the ignored fields.
     */
    public Collection<String> getGlobalAccessIgnoredFields(final String index) {

        return switch (index.toLowerCase()) {
            case CONTACT_FIELD -> COMPANY_ENTITY_IGNORED_FIELDS;
            case DEAL_FIELD -> {
                final Collection<String> fields = new ArrayList<>();

                fields.addAll(CONTACT_ENTITY_IGNORED_FIELDS);
                fields.addAll(COMPANY_ENTITY_IGNORED_FIELDS);

                yield fields;
            }
            case CommonSearchFields.ACTIVITY, CommonSearchFields.NOTE, CommonSearchFields.FILE -> {
                final Collection<String> fields = new ArrayList<>();

                fields.addAll(DEAL_ENTITY_IGNORED_FIELDS);
                fields.addAll(CONTACT_ENTITY_IGNORED_FIELDS);
                fields.addAll(COMPANY_ENTITY_IGNORED_FIELDS);

                yield fields;
            }
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the entity fields.
     * </p>
     *
     * @param entity Represents the index name.
     * @return the required fields for the mobile client.
     */
    public Collection<String> getEntityFields(final String entity) {

        return switch (entity.toLowerCase()) {
            case CONTACT_FIELD -> CONTACT_ENTITY_REQUIRED_FIELDS;
            case DEAL_FIELD -> DEAL_ENTITY_REQUIRED_FIELDS;
            case COMPANY_FIELD -> COMPANY_ENTITY_REQUIRED_FIELDS;
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the contact's company fields.
     * </p>
     *
     * @return contact's company fields
     */
    private Collection<String> getContactCompanyFields() {
        final Collection<String> fields = new ArrayList<>();

        fields.add(CONTACT_COMPANY_ID_FIELD);
        fields.add(CONTACT_COMPANY_NAME_FIELD);

        return fields;
    }

    /**
     * <p>
     * Gets the deal's company fields.
     * </p>
     *
     * @return deal's company fields
     */
    private Collection<String> getDealCompanyFields() {
        final Collection<String> fields = new ArrayList<>();

        fields.add(DEAL_COMPANY_ID_FIELD);
        fields.add(DEAL_COMPANY_NAME_FIELD);

        return fields;
    }

    /**
     * <p>
     * Gets the deal's contact fields.
     * </p>
     *
     * @return deal's contact fields
     */
    private Collection<String> getDealContactFields() {
        final Collection<String> fields = new ArrayList<>();

        fields.add(DEAL_CONTACT_ID_FIELD);
        fields.add(DEAL_CONTACT_NAME_FIELD);

        return fields;
    }

    /**
     * <p>
     * Adds deal, contact & company fields.
     * </p>
     */
    private void addRequiredEntitiesFields(final Collection<String> fields) {
        addRequiredContactAndCompanyFields(fields);
        fields.add(DEAL_FIELD);
        fields.addAll(getDealContactFields());
        fields.addAll(getDealCompanyFields());
    }

    /**
     * <p>
     * Adds contact & company fields.
     * </p>
     */
    private void addRequiredContactAndCompanyFields(final Collection<String> fields) {
        addCompanyField(fields);
        fields.add(CONTACT_FIELD);
        fields.addAll(getContactCompanyFields());
    }

    /**
     * <p>
     * Adds company fields.
     * </p>
     */
    private void addCompanyField(final Collection<String> fields) {
        fields.add(COMPANY_FIELD);
    }
}
