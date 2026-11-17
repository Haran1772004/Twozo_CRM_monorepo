package com.tlc.crm.entity.list.filter.source;

import com.tlc.sql.common.api.dml.Table;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Offers the source mapping groups to identify which mapper table joins the two sources.
 * <p>
 * (eg: In the case of Contact and Company, we require a mapper table to join these two sources)
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum SourceTableMappings {

    CONTACT_COMPANY(true, "company", "MCONCompanyFields", FilterSourceType.CONTACT, FilterSourceType.COMPANY),
    DEAL_COMPANY(true, "company", "MDOCompanyFields", FilterSourceType.DEAL, FilterSourceType.COMPANY),
    CONTACT_DEAL(false, "deal", "MDOParticipant", FilterSourceType.CONTACT, FilterSourceType.DEAL),
    CONTACT_ACTIVITY(false, "activity", "MACContactActivity", FilterSourceType.CONTACT, FilterSourceType.ACTIVITY),
    COMPANY_ACTIVITY(false, "activity", "MACCompanyActivity", FilterSourceType.COMPANY, FilterSourceType.ACTIVITY),
    DEAL_ACTIVITY(false, "activity", "MACDealActivity", FilterSourceType.DEAL, FilterSourceType.ACTIVITY),
    CONTACT_NOTE(false, "note", "OMNTContactNote", FilterSourceType.CONTACT, FilterSourceType.NOTE),
    COMPANY_NOTE(false, "note", "OMNTCompanyNote", FilterSourceType.COMPANY, FilterSourceType.NOTE),
    DEAL_NOTE(false, "note", "OMNTDealNote", FilterSourceType.DEAL, FilterSourceType.NOTE),
    NOTE_ACTIVITY(false, "note", "MACNoteActivity", FilterSourceType.NOTE, FilterSourceType.ACTIVITY),
    CONTACT_EMAIL(false, "email", "OMECContactConversations", FilterSourceType.CONTACT, FilterSourceType.EMAIL),
    COMPANY_EMAIL(false, "email", "OMECCompanyConversations", FilterSourceType.COMPANY, FilterSourceType.EMAIL),

    COMPANY_CONTACT(true, "company", "MCONCompanyFields", FilterSourceType.COMPANY, FilterSourceType.CONTACT),
    COMPANY_DEAL(true, "company", "MDOCompanyFields", FilterSourceType.COMPANY, FilterSourceType.DEAL),
    DEAL_CONTACT(false, "deal", "MDOParticipant", FilterSourceType.DEAL, FilterSourceType.CONTACT),
    ACTIVITY_CONTACT(false, "activity", "MACContactActivity", FilterSourceType.ACTIVITY, FilterSourceType.CONTACT),
    ACTIVITY_COMPANY(false, "activity", "MACCompanyActivity", FilterSourceType.ACTIVITY, FilterSourceType.COMPANY),
    ACTIVITY_DEAL(false, "activity", "MACDealActivity", FilterSourceType.ACTIVITY, FilterSourceType.DEAL),
    NOTE_CONTACT(false, "note", "OMNTContactNote",  FilterSourceType.NOTE, FilterSourceType.CONTACT),
    NOTE_COMPANY(false, "note", "OMNTCompanyNote", FilterSourceType.NOTE, FilterSourceType.COMPANY),
    NOTE_DEAL(false, "note", "OMNTDealNote", FilterSourceType.NOTE, FilterSourceType.DEAL),
    ACTIVITY_NOTE(false, "note", "MACNoteActivity", FilterSourceType.ACTIVITY, FilterSourceType.NOTE),
    EMAIL_CONTACT(false, "email", "OMECContactConversations", FilterSourceType.EMAIL, FilterSourceType.CONTACT),
    EMAIL_COMPANY(false, "email", "OMEMCompanyConversations", FilterSourceType.EMAIL, FilterSourceType.COMPANY),
    ;

    private static final Map<FilterSourceType, Map<FilterSourceType, SourceTableMappings>> SOURCE_TABLE_MAPPINGS;

    private final boolean isPersistentMapperTable;
    private final String fieldName;
    private final String mapperTableName;
    private final FilterSourceType primarySource;
    private final FilterSourceType secondarySource;

    static {
        SOURCE_TABLE_MAPPINGS = new HashMap<>();

        for (final SourceTableMappings sourceTableMappings : values()) {
            SOURCE_TABLE_MAPPINGS.computeIfAbsent(sourceTableMappings.primarySource, mappings -> new HashMap<>())
                    .put(sourceTableMappings.secondarySource, sourceTableMappings);
        }
    }

    SourceTableMappings(final boolean isPersistentMapperTable, final String fieldName, final String mapperTableName, final FilterSourceType primarySource,
                        final FilterSourceType secondarySource) {
        this.isPersistentMapperTable = isPersistentMapperTable;
        this.fieldName = fieldName;
        this.mapperTableName = mapperTableName;
        this.primarySource = primarySource;
        this.secondarySource = secondarySource;
    }

    /**
     * <p>
     * Provides the {@link SourceTableMappings} for the given primary and secondary sources.
     * </p>
     *
     * @param primarySource   Represents the primary source.
     * @param secondarySource Represents the secondary source.
     * @return {@link SourceTableMappings}.
     */
    public static SourceTableMappings get(final FilterSourceType primarySource, final FilterSourceType secondarySource) {
        if (SOURCE_TABLE_MAPPINGS.containsKey(primarySource)) {
            final Map<FilterSourceType, SourceTableMappings> sourceMappings = SOURCE_TABLE_MAPPINGS.get(primarySource);

            return sourceMappings.get(secondarySource);
        }

        return null;
    }

    public Table getMapperTable() {
        return Table.get(mapperTableName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public FilterSourceType getPrimarySource() {
        return primarySource;
    }

    public boolean isPersistentMapperTable() {
        return isPersistentMapperTable;
    }
}
