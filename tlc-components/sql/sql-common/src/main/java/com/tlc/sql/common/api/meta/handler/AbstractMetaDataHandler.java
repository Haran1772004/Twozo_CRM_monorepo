package com.tlc.sql.common.api.meta.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.SchemaDataStore;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.MetaCache;
import com.tlc.sql.common.internal.parser.PatternResolver;
import com.tlc.sql.common.internal.parser.PatternResolverImpl;
import com.tlc.sql.common.internal.parser.XmlToDCConverter;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.resolver.TableResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractMetaDataHandler<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>, A extends AdminDataStore, S extends SchemaDataStore<C, T, D>> implements MetaDataHandler<C, T> {

    protected final A adminDataStore;
    protected final S schemaDataStore;
    protected final TableResolver tableResolver;

    protected AbstractMetaDataHandler(final A adminDataStore, final S schemaDataStore, final TableResolver tableResolver) {
        this.adminDataStore = Objects.requireNonNull(adminDataStore);
        this.schemaDataStore = Objects.requireNonNull(schemaDataStore);
        this.tableResolver = tableResolver;
    }

    public void loadMetaData(File file) {
        final Map<String, T> tableDefinitions = fetchTableDefinitions(file);
        loadMetaData(tableDefinitions);
    }

    public void loadMetaData(Map<String, T> tableDefinitions) {
        getMetaCache().put(tableDefinitions);
    }

    protected abstract MetaCache<T> getMetaCache();

    public void populateMetaData(File file) {
        final Map<String, T> tableDefinitions = fetchTableDefinitions(file);
        populateMetaData(tableDefinitions);
    }

    public void populateMetaData(final Map<String, T> tableDefinitionsMap) {
        final Collection<T> tableDefinitions = new ArrayList<>(tableDefinitionsMap.size());

        if(Objects.isNull(tableDefinitionsMap) || tableDefinitionsMap.isEmpty()){
            return;
        }

        tableDefinitions.addAll(tableDefinitionsMap.values());
        schemaDataStore.createTables(tableDefinitions);
        getMetaCache().put(tableDefinitionsMap);
    }

    public void unloadMetaData(Set<String> tables) {
        getMetaCache().remove(tables);
    }

    public Map<String, T> fetchTableDefinitions(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return fetchTableDefinitions(inputStream);
        } catch (Exception exp) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FILE, exp);
        }
    }

    public void populateData(List<File> files, Map<String, String> dynamicProp) {
        final PatternResolver patternResolver = new PatternResolverImpl(adminDataStore, tableResolver);
        final DataContainer dataContainer = fetchDataContainer(files, patternResolver, dynamicProp, XmlToDCConverter.Type.INITIALIZE);
        adminDataStore.commitChanges(dataContainer);
    }

    public DataContainer fetchDataContainer(List<File> files, PatternResolver patternResolver, Map<String, String> dynamicProp, XmlToDCConverter.Type type) {
        final DataContainer dataContainer = DataContainer.create();
        final XmlToDCConverter xmlToDCConverter = XmlToDCConverter.getInstance();
        for (File file : files) {
            final DataContainer xmlContainer = xmlToDCConverter.convert(file, patternResolver, dynamicProp::get, type, tableResolver);
            dataContainer.append(xmlContainer);
        }
        return dataContainer;
    }
}