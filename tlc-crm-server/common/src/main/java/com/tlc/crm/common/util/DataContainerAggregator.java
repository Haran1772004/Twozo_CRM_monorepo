package com.tlc.crm.common.util;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Utility class for aggregating multiple select queries into a single {@link DataContainer}.
 * </p>
 *
 * @author Maharaja
 * @version 1.0
 */
public final class DataContainerAggregator {

    private DataContainerAggregator() {
    }

    /**
     * <p>
     * Fetches the data container by executing the given select queries
     * </p>
     *
     * @param orgId         Represents the org ID
     * @param selectQueries Represents the collection of {@link SelectQuery}
     * @return The {@link DataContainer}
     */
    public static DataContainer getContainer(final Long orgId, final Collection<SelectQuery> selectQueries) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        DataContainer dataContainer = null;
        boolean isDataContainerNull = true;

        for (final SelectQuery selectQuery : selectQueries) {

            if (isDataContainerNull) {
                dataContainer = orgDataStore.get(selectQuery);
                isDataContainerNull = false;
            } else {
                dataContainer.append(orgDataStore.get(selectQuery));
            }
        }

        return Objects.isNull(dataContainer) ? DataContainer.create() : dataContainer;
    }
}