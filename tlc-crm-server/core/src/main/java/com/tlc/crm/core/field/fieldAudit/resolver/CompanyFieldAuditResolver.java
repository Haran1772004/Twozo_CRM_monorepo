package com.tlc.crm.core.field.fieldAudit.resolver;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOMADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCOMCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCOMDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MCOMMONETARYFIELDS;
import com.tlc.crm.core.sql.resource.MTCOMPANYTAG;
import com.tlc.crm.core.features.api.tag.SourceTagManager;

import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.sql.resource.OMETOPTION;
import com.tlc.crm.entity.sql.resource.OMTTAG;

import com.tlc.crm.fields.sql.resource.OMETADDRESS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles field edit history value loading, resolution, and comparison for {@link Company} entities.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class CompanyFieldAuditResolver extends AbstractFieldAuditResolver {

    private final MonetaryManager monetaryManager;

    private CompanyFieldAuditResolver() {
        this.monetaryManager = MonetaryManager.getInstance();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link CompanyFieldAuditResolver}.
     * </p>
     *
     * @return The singleton instance of {@link CompanyFieldAuditResolver}
     */
    public static CompanyFieldAuditResolver getInstance() {
        return Instance.COMPANY_FIELD_HISTORY_RESOLVER;
    }

    /**
     * Using Initialization-on-demand holder idiom for lazy-loaded, thread-safe singleton
     */
    private static class Instance {
        private static final CompanyFieldAuditResolver COMPANY_FIELD_HISTORY_RESOLVER = new CompanyFieldAuditResolver();
    }

    /**
     * {@inheritDoc}
     */
    public Table getTable() {
        return Table.get(MCOCOMPANY.TABLE);
    }

    /**
     * Gets the company option table
     *
     * @return {@link Table}
     */
    public Table getCompanyOptionTable() {
        return Table.get(MCOMDROPDOWNFIELDS.TABLE);
    }

    /**
     * Gets the company monetary table
     *
     * @return {@link Table}
     */
    public Table getCompanyMonetaryTable() {
        return Table.get(MCOMMONETARYFIELDS.TABLE);
    }

    /**
     * Gets the company address table
     *
     * @return {@link Table}
     */
    public Table getCompanyAddressTable() {
        return Table.get(MCOMADDRESSFIELDS.TABLE);
    }

    /**
     * <p>
     * Gets the company fields table
     * </p>
     *
     * @return {@link Table} of company fields
     */
    public Table getCompanyFieldsTable() {
        return Table.get(MCOMCOMPANYFIELDS.TABLE);
    }

    /**
     * <p>
     * Loads field edit history entries for the given company update into the target container.
     * </p>
     */
    public void loadFieldAudit(final Company company, final Row oldRow, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Long orgId = company.orgId();
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.COMPANY.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final Collection<FieldInfo> allFields = fieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE);
        final Map<String, String> existingCompanyValues = new HashMap<>();
        final Map<String, String> newCompanyValues = buildNewCompanySystemFieldValues(company, config);
        final Map<String, String> oldCustomValues = FieldAuditHandler.extractFieldValues(company.getExistingValueHolder(), allFields, config);
        final Map<String, String> newCustomValues = FieldAuditHandler.extractFieldValues(company.getValueHolder(), allFields, config);

        if (Objects.nonNull(oldRow)) {
            loadOldSystemFieldValues(config, oldRow, existingCompanyValues, sourceContainer);
        }
        FieldAuditHandler.resolveNewCustomFieldValues(config, company.getValueHolder(), allFields, newCustomValues,
                (field, rawValue) -> resolveCustomFieldValue(orgId, field, rawValue, field.storeType()));

        oldCustomValues.forEach(existingCompanyValues::putIfAbsent);
        newCustomValues.forEach(newCompanyValues::putIfAbsent);

        fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.COMPANY, company.id(), company.getUpdatedBy().id(),
                company.getFieldAuditInfo(), allFields, existingCompanyValues, newCompanyValues, targetContainer);
    }

    /**
     * <p>
     * Loads old system field values from the existing company row for field edit history comparison.
     * </p>
     */
    private void loadOldSystemFieldValues(final FieldTrackingConfig config, final Row oldRow, final Map<String, String> oldValues, final DataContainer sourceContainer) {
        final String nameField = CompanyFields.NAME;
        final String websiteField = CompanyFields.WEBSITE;
        final String employeeCountField = CompanyFields.EMPLOYEE_COUNT;
        final String sicCodeField = CompanyFields.SIC_CODE;
        final String linkedinField = CompanyFields.LINKEDIN;
        final String twitterField = CompanyFields.TWITTER;
        final String facebookField = CompanyFields.FACEBOOK;
        final String descriptionField = CompanyFields.DESCRIPTION;

        putIfTrackEnabled(config, oldRow, oldValues, nameField, MCOCOMPANY.NAME);
        putIfTrackEnabled(config, oldRow, oldValues, websiteField, MCOCOMPANY.WEBSITE);
        putIfTrackEnabled(config, oldRow, oldValues, employeeCountField, MCOCOMPANY.NUMBER_OF_EMPLOYEE);
        putIfTrackEnabled(config, oldRow, oldValues, sicCodeField, MCOCOMPANY.SIC_CODE);
        putIfTrackEnabled(config, oldRow, oldValues, linkedinField, MCOCOMPANY.LINKEDIN);
        putIfTrackEnabled(config, oldRow, oldValues, twitterField, MCOCOMPANY.TWITTER);
        putIfTrackEnabled(config, oldRow, oldValues, facebookField, MCOCOMPANY.FACEBOOK);
        putIfTrackEnabled(config, oldRow, oldValues, descriptionField, MCOCOMPANY.DESCRIPTION);
        final Long orgId = oldRow.getOrgId();
        final String salesOwnerField = CompanyFields.SALES_OWNER;
        final String territoryField = CompanyFields.TERRITORY;
        final String parentCompanyField = CompanyFields.PARENT_COMPANY;
        final String annualRevenueField = CompanyFields.ANNUAL_REVENUE;
        final String organizationStatusField = CompanyFields.ORGANIZATION_STATUS;
        final String industryTypeField = CompanyFields.INDUSTRY_TYPE;
        final String businessTypeField = CompanyFields.BUSINESS_TYPE;
        final String addressField = CompanyFields.ADDRESS;
        final String tagsField = CompanyFields.TAGS;

        if (config.isFieldTracked(salesOwnerField)) {
            final Long ownerId = oldRow.get(MCOCOMPANY.OWNER_ID);

            if (Objects.nonNull(ownerId)) {
                putIfNonNull(oldValues, salesOwnerField, userService.getUser(orgId, ownerId).getFullName());
            }
        }

        if (config.isFieldTracked(territoryField)) {
            final Long territoryId = oldRow.get(MCOCOMPANY.TERRITORY_ID);

            if (Objects.nonNull(territoryId)) {
                final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
                final Row territoryRow = sourceContainer.getRow(territoryTable, new WhereClause(Criteria.eq(territoryTable.getColumn(OMCRMTERRITORY.ID), territoryId)));

                putIfNonNull(oldValues, territoryField, Objects.isNull(territoryRow) ? null : territoryRow.get(OMCRMTERRITORY.NAME));
            }
        }
        final List<String> trackedComplexFields = new ArrayList<>();

        addFieldIfTrackEnabled(config, parentCompanyField, trackedComplexFields);
        addFieldIfTrackEnabled(config, addressField, trackedComplexFields);
        addFieldIfTrackEnabled(config, annualRevenueField, trackedComplexFields);
        addFieldIfTrackEnabled(config, organizationStatusField, trackedComplexFields);
        addFieldIfTrackEnabled(config, industryTypeField, trackedComplexFields);
        addFieldIfTrackEnabled(config, businessTypeField, trackedComplexFields);
        addFieldIfTrackEnabled(config, tagsField, trackedComplexFields);

        if (trackedComplexFields.isEmpty()) {
            return;
        }
        final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(orgId, Source.COMPANY.getName(), trackedComplexFields);

        if (fieldInfoMap.containsKey(parentCompanyField)) {
            final Table companyFieldsTable = getCompanyFieldsTable();
            final Row companyFieldRow = sourceContainer.getRow(companyFieldsTable, new WhereClause(Criteria.eq(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.FIELD_ID),
                    fieldInfoMap.get(parentCompanyField).id())));

            if (Objects.nonNull(companyFieldRow)) {
                final Table companyTable = getTable();
                final Long companyId = companyFieldRow.get(MCOMCOMPANYFIELDS.COMPANY_ID);
                final Row row = sourceContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(), companyId)));

                putIfNonNull(oldValues, parentCompanyField, Objects.isNull(row) ? CompanyManager.getInstance().getName(orgId, companyId) : row.get(MCOCOMPANY.NAME));
            }
        }

        if (fieldInfoMap.containsKey(addressField)) {
            final Table companyAddressTable = getCompanyAddressTable();
            final Row companyAddressRow = sourceContainer.getRow(companyAddressTable,
                    new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.FIELD_ID), fieldInfoMap.get(addressField).id())));

            if (Objects.nonNull(companyAddressRow)) {
                final Table addressTable = addressManager.getTable();
                final Row addressRow = sourceContainer.getRow(addressTable, new WhereClause(Criteria.eq(addressTable.getColumn(OMETADDRESS.ID),
                        companyAddressRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

                putIfNonNull(oldValues, addressField, Objects.isNull(addressRow) ? null : addressManager.convertRowToModel(addressRow).toString());
            }
        }

        if (fieldInfoMap.containsKey(annualRevenueField)) {
            final Table companyMonetaryTable = getCompanyMonetaryTable();
            final Row companyMonetaryRow = sourceContainer.getRow(companyMonetaryTable,
                    new WhereClause(Criteria.eq(companyMonetaryTable.getColumn(MCOMMONETARYFIELDS.FIELD_ID), fieldInfoMap.get(annualRevenueField).id())));

            if (Objects.nonNull(companyMonetaryRow)) {
                final Table monetaryTable = monetaryManager.getTable();
                final Row monetaryRow = sourceContainer.getRow(monetaryTable, new WhereClause(Criteria.eq(monetaryTable.getColumn(MCOMMONETARYFIELDS.ID),
                        companyMonetaryRow.get(MCOMMONETARYFIELDS.MONETARY_ID))));

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    if (Objects.nonNull(monetary)) {
                        putIfNonNull(oldValues, annualRevenueField, Monetary.getValueWithSymbol(monetary.getCurrency().getSymbol(), monetary.getValue().doubleValue()));
                    }
                }
            }
        }

        if (fieldInfoMap.containsKey(tagsField)) {
            final Table companyTagTable = SourceTagManager.getInstance().getCompanyTagTable();
            final Table tagTable = SourceTagManager.getInstance().getTable();
            final List<Long> tagIds = sourceContainer.getRows(companyTagTable, new WhereClause(Criteria.eq(companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID), oldRow.getPKValue())))
                    .filter(Objects::nonNull).sorted(Comparator.comparing(Row::getPKValue))
                    .map(row -> (Long) row.get(MTCOMPANYTAG.TAG_ID)).toList();

            if (!tagIds.isEmpty()) {
                putIfNonNull(oldValues, tagsField, sourceContainer.getRows(tagTable, new WhereClause(Criteria.in(tagTable.getColumn(OMTTAG.ID), tagIds)))
                        .filter(Objects::nonNull).map(row -> (String) row.get(OMTTAG.NAME)).collect(Collectors.joining(",")));
            }
        }
        getOptionFieldsOldValues(oldValues, sourceContainer, organizationStatusField, fieldInfoMap);
        getOptionFieldsOldValues(oldValues, sourceContainer, industryTypeField, fieldInfoMap);
        getOptionFieldsOldValues(oldValues, sourceContainer, businessTypeField, fieldInfoMap);
    }

    /**
     * <p>
     * Builds a map of current company system field values for field edit history comparison.
     * </p>
     */
    private Map<String, String> buildNewCompanySystemFieldValues(final Company company, final FieldTrackingConfig config) {
        final Long orgId = company.orgId();
        final Map<String, String> newValues = new HashMap<>();
        final CrmUser salesOwner = company.getSalesOwner();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();
        final Option organizationStatus = company.getOrganizationStatus();
        final Option industryType = company.getIndustryType();
        final Option businessType = company.getBusinessType();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final Address address = company.getAddress();
        final String salesOwnerKey = CompanyFields.SALES_OWNER;
        final String parentCompanyKey = CompanyFields.PARENT_COMPANY;
        final String territoryKey = CompanyFields.TERRITORY;
        final String orgStatusKey = CompanyFields.ORGANIZATION_STATUS;
        final String industryKey = CompanyFields.INDUSTRY_TYPE;
        final String businessKey = CompanyFields.BUSINESS_TYPE;
        final String annualRevenueKey = CompanyFields.ANNUAL_REVENUE;
        final String tagsKey = CompanyFields.TAGS;

        putIfNonNull(newValues, CompanyFields.NAME, company.getName());
        putIfNonNull(newValues, CompanyFields.WEBSITE, company.getWebsite());
        putIfNonNull(newValues, CompanyFields.EMPLOYEE_COUNT, company.getEmployeeCount());
        putIfNonNull(newValues, CompanyFields.SIC_CODE, company.getSicCode());
        putIfNonNull(newValues, CompanyFields.DESCRIPTION, company.getDescription());
        putIfNonNull(newValues, CompanyFields.LINKEDIN, company.getLinkedIn());
        putIfNonNull(newValues, CompanyFields.TWITTER, company.getTwitter());
        putIfNonNull(newValues, CompanyFields.FACEBOOK, company.getFacebook());
        putIfNonNull(newValues, CompanyFields.ADDRESS, address);
        final SummaryInfo summaryInfo = company.getSummaryInfo();

        if (Objects.nonNull(summaryInfo) && config.isFieldTracked(tagsKey)) {
            putIfNonNull(newValues, tagsKey, resolveTagNames(orgId, summaryInfo));
        }

        if (config.isFieldTracked(salesOwnerKey) && Objects.nonNull(salesOwner)) {
            final String fullName = salesOwner.getFullName();

            putIfNonNull(newValues, salesOwnerKey, Objects.isNull(fullName) ? userService.getUser(orgId, salesOwner.id()).getFullName() : fullName);
        }

        if (config.isFieldTracked(parentCompanyKey) && Objects.nonNull(parentCompany)) {
            final String parentCompanyName = parentCompany.getName();

            putIfNonNull(newValues, parentCompanyKey, Objects.isNull(parentCompanyName) ? CompanyManager.getInstance().getName(orgId, parentCompany.id()) : parentCompanyName);
        }

        if (config.isFieldTracked(territoryKey) && Objects.nonNull(territory)) {
            final String territoryName = territory.getName();

            putIfNonNull(newValues, territoryKey, Objects.isNull(territoryName) ? resolveTerritoryName(orgId, territory.id()) : territoryName);
        }

        if (config.isFieldTracked(orgStatusKey) && Objects.nonNull(organizationStatus)) {
            putIfNonNull(newValues, orgStatusKey, resolveOptionName(orgId, organizationStatus));
        }

        if (config.isFieldTracked(industryKey) && Objects.nonNull(industryType)) {
            putIfNonNull(newValues, industryKey, resolveOptionName(orgId, industryType));
        }

        if (config.isFieldTracked(businessKey) && Objects.nonNull(businessType)) {
            putIfNonNull(newValues, businessKey, resolveOptionName(orgId, businessType));
        }

        if (config.isFieldTracked(annualRevenueKey) && Objects.nonNull(annualRevenue)) {
            putIfNonNull(newValues, annualRevenueKey, Monetary.getValueWithSymbol(annualRevenue.getCurrency().getSymbol(), annualRevenue.getValue().doubleValue()));
        }
        return newValues;
    }

    /**
     * Loads old values for option type fields from the source container for field edit history comparison.
     */
    private void getOptionFieldsOldValues(final Map<String, String> oldValues, final DataContainer sourceContainer, final String field,
            final Map<String, FieldInfo> fieldInfoMap) {
        if (!fieldInfoMap.containsKey(field)) {
            return;
        }
        final Table companyOptionTable = getCompanyOptionTable();
        final Table optionTable = OptionManager.getInstance().getTable();
        final Row companyOptionRow = sourceContainer.getRow(companyOptionTable,
                new WhereClause(Criteria.eq(companyOptionTable.getColumn(MCOMDROPDOWNFIELDS.FIELD_ID), fieldInfoMap.get(field).id())));

        if (Objects.nonNull(companyOptionRow)) {
            final Row optionRow = sourceContainer.getRow(optionTable, new WhereClause(Criteria.eq(optionTable.getColumn(OMETOPTION.ID), companyOptionRow.get(MCOMDROPDOWNFIELDS.OPTION_ID))));

            putIfNonNull(oldValues, field, Objects.isNull(optionRow) ? null : optionRow.get(OMETOPTION.OPTION));
        }
    }
}