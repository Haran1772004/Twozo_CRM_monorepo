package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.SqlServiceRegistry;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;
import com.tlc.sql.olap.OlapDatabaseInitializer;
import com.tlc.sql.oltp.OltpDatabaseInitializer;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.net.URL;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Initialize the SQL module, loading meta and data XML files along with other associated services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public final class SqlInitializer extends AbstractModuleInitializer {

    private static final String META_FILE_NAME = "meta.xml";
    private static final String DATA_FILE_NAME = "data.xml";

    private final ServiceRegistry<SQLService> sqlServiceRegistry;

    private SqlInitializer() {
        this.sqlServiceRegistry = SqlServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public void initialize() throws IOException {
        if (isServiceEnabled() && isServiceEnabled(ComponentsStarterFields.DATABASE_RESOURCE)) {
            final OltpDatabaseInitializer oltpDatabaseInitializer = OltpDatabaseInitializer.get();
            final OlapDatabaseInitializer olapDatabaseInitializer = OlapDatabaseInitializer.get();
            final boolean isOlapServiceEnabled = isServiceEnabled(ComponentsStarterFields.OLAP);

            oltpDatabaseInitializer.initialize(getProperties(ComponentsStarterFields.DATABASE).getNestedConfigs(ComponentsStarterFields.OLTP),
                    getProperties(ComponentsStarterFields.DATABASE_RESOURCE));

            if (isOlapServiceEnabled) {
                olapDatabaseInitializer.initialize(getProperties(ComponentsStarterFields.DATABASE).getNestedConfigs(ComponentsStarterFields.OLAP),
                        getProperties(ComponentsStarterFields.DATABASE_RESOURCE));
                sqlServiceRegistry.register(olapDatabaseInitializer.getSqlService());
            }

            sqlServiceRegistry.register(oltpDatabaseInitializer.getSqlService());

            for (final Map.Entry<Modules, Collection<Resource>> groupedSqlResources : fetchSqlResources().entrySet()) {
                final Modules modules = groupedSqlResources.getKey();
                final SqlRegistrarData sqlRegistrarData = new SqlRegistrarData();

                URL metaUrl = null;
                URL dataUrl = null;

                for (final Resource resource : groupedSqlResources.getValue()) {

                    if (Objects.equals(META_FILE_NAME, resource.getFilename())) {
                        metaUrl = resource.getURL();

                        sqlRegistrarData.setLastModifiedTime(resource.lastModified());
                    } else if (Objects.equals(DATA_FILE_NAME, resource.getFilename())) {
                        dataUrl = resource.getURL();

                        sqlRegistrarData.setLastModifiedTime(resource.lastModified());
                    }
                }

                sqlRegistrarData.setModuleName(modules.getName());
                sqlRegistrarData.setModuleId(modules.getId());
                sqlRegistrarData.setMetaFileUrl(metaUrl);
                sqlRegistrarData.setDataFileUrl(dataUrl);

                oltpDatabaseInitializer.register(sqlRegistrarData);

                if (isOlapServiceEnabled) {
                    olapDatabaseInitializer.register(sqlRegistrarData);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.DATABASE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return sqlServiceRegistry;
    }

    /**
     * Retrieves the meta and data {@link Resource} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its sql meta and data resources.
     */
    private Map<Modules, Collection<Resource>> fetchSqlResources() {
        final Map<Modules, Collection<Resource>> sortedSQlResourcesByModule = new LinkedHashMap<>();

        try {
            final Collection<Resource> resources = resourceScanner.scanResources("resources/sql/", "*.xml");
            final Map<Modules, Collection<Resource>> segregateResourcesByModule = segregateResourcesByModule(resources);

            segregateResourcesByModule.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                    .forEachOrdered(element -> sortedSQlResourcesByModule.put(element.getKey(), element.getValue()));
        } catch (IOException exception) {
            LOGGER.error("Failed to load SQL resources. Reason: {}", exception.getMessage(), exception);
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }

        return sortedSQlResourcesByModule;
    }
}
