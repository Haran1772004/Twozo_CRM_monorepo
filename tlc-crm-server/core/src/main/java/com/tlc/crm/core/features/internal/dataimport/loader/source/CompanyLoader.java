package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.builder.model.CompanyBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.EntityLoader;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Loader to load the {@link Company} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 2.0
 */
public final class CompanyLoader extends EntityLoader<Company> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyLoader.class);

    private final CompanyManager companyManager;
    private final CompanyBuilder companyBuilder;
    private final CompanyConverter companyConverter;

    private CompanyLoader() {
        super(Source.COMPANY);
        this.companyManager = CompanyManager.getInstance();
        this.companyBuilder = CompanyBuilder.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
    }

    private static final class Instance {
        private static final CompanyLoader COMPANY_LOADER = new CompanyLoader();
    }

    /**
     * Gets the instance of {@link CompanyLoader}
     *
     * @return {@link CompanyLoader} instance
     */
    public static CompanyLoader getInstance() {
        return Instance.COMPANY_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link Company} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link Company} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Map<FieldInfo, Map<Object, SourceRecord>> indexedData = sourceRecordContainer.indexData(source);
        final Map<Integer, Company> convertedModels = new HashMap<>();
        final Map<Integer, Collection<Integer>> mergedIndexMap = sourceRecordContainer.getMergedIndicesWithinTheSheet(source);

        loadIndicesInfo(sourceRecordContainer, dataHolder.sourceModelContainer());

        if (!indexedData.isEmpty()) {
            final Long orgId = dataHolder.orgId();
            final long importId = errorTracker.getImportId();
            final Map<String, FieldInfo> companyFields = importFieldService.groupDefaultFieldsByAliasName(orgId, source);
            final Map<Object, SourceRecord> entityIdRecordMap = indexedData.computeIfAbsent(companyFields.get(CompanyFields.ID), idMap -> new HashMap<>());

            if (!entityIdRecordMap.isEmpty()) {
                LOGGER.debug("Data import: Model loading - Company models are updating, import ID: {}, org ID: {}", importId, orgId);
                final long modelUpdateStartTime = System.currentTimeMillis();
                loadUpdatableModels(dataHolder, convertedModels, entityIdRecordMap, errorTracker);
                LOGGER.debug("Data import: Model loading - Company models are updated in {}ms, import ID: {}, org ID: {}", System.currentTimeMillis() - modelUpdateStartTime, importId, orgId);
            }
        }
        loadModels(dataHolder, mergedIndexMap, convertedModels, errorTracker);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param entity Refers the model to be cloned.
     * @return The cloned {@link Company}
     */
    @Override
    protected Company cloneModel(final long orgId, final Company entity) {
        final Company company = super.cloneModel(orgId, entity);

        company.setOrgId(orgId);
        company.setId(entity.id());

        return company;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CompanyBuilder}
     */
    @Override
    protected CompanyBuilder getModelBuilder() {
        return companyBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CompanyConverter}
     */
    @Override
    protected EntityConverter<Company> getConverter() {
        return companyConverter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param id    Represents the ID
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @return  The Optional {@link Company}
     */
    @Override
    protected Optional<Company> get(final long orgId, final long id, final SourceModelContainer sourceModelContainer) {
        final DataContainer dataContainer = sourceModelContainer.getImportDataContainerHolder().getRelatedDataContainer(source, id);
        final Table table = Table.get(MCOCOMPANY.TABLE);
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        if (Objects.isNull(row)) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        return Optional.of(companyManager.convertRowToModel(row, false, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Refers the organization id.
     * @param company Refers the model.
     */
    @Override
    protected void loadEntitySummary(final long orgId, final Company company) {
        companyManager.loadSummaryDetails(orgId, company);
    }
}
