package com.tlc.crm.notifications.marketplace.api.manager.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.marketplace.app.sql.resource.OMKPINSTALLEDAPPS;
import com.tlc.crm.notifications.marketplace.api.manager.GeneralTelephoneActionManager;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.sql.resource.MKPAPPS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.marketplace.model.Category.TELEPHONY;

/**
 * Implementation of {@link com.tlc.crm.notifications.marketplace.api.manager.GeneralTelephoneActionManager} that provides generic telephony
 * operations without dependency on specific providers (TeleCMI, MCube, etc.).
 * This class can be used by any component that needs to check telephony capabilities
 * without requiring knowledge of a specific provider.
 *
 * @author Kishore J
 * @version 1.0
 */
public class GeneralTelephoneActionManagerImpl implements GeneralTelephoneActionManager {

    private GeneralTelephoneActionManagerImpl() {
    }

    private static class InstanceHolder {
        private static final GeneralTelephoneActionManagerImpl INSTANCE = new GeneralTelephoneActionManagerImpl();
    }

    /**
     * Returns the singleton instance of {@link GeneralTelephoneActionManagerImpl}.
     *
     * @return the singleton instance
     */
    public static GeneralTelephoneActionManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId the organization identifier
     * @return {@code true} if a telephony app is installed, {@code false} otherwise
     */
    @Override
    public boolean anyAppInstalled(final long orgId) {
        final Table installedAppTable = Table.get(OMKPINSTALLEDAPPS.TABLE);
        final Table crmMarketplaceAppTable = Table.get(MKPCRMAPPS.TABLE);
        final CountQuery countQuery = CountQuery.get(installedAppTable);

        countQuery.addJoinClause(new JoinClause(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID),
                crmMarketplaceAppTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.CATEGORY), TELEPHONY.getId()))
                .and(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds())));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     *
     * {@inheritDoc}
     *
     * @param orgId the organization identifier
     * @return an {@link Optional} containing the installed telephony provider name,
     * or {@link Optional#empty()} if none is active
     */
    @Override
    public Optional<String> getInstalledProviderName(final long orgId) {
        final Table installedAppTable = Table.get(OMKPINSTALLEDAPPS.TABLE);
        final Table crmMarketplaceAppTable = Table.get(MKPCRMAPPS.TABLE);
        final Table marketplaceAppTable = Table.get(MKPAPPS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(installedAppTable);

        selectQuery.addSelectClause(marketplaceAppTable.getColumn(MKPAPPS.NAME));
        selectQuery.addJoinClause(List.of(
                new JoinClause(installedAppTable.getColumn(OMKPINSTALLEDAPPS.APP_ID), crmMarketplaceAppTable.getPKColumn()),
                new JoinClause(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.MARKETPLACE_APP_ID), marketplaceAppTable.getPKColumn())
        ));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(crmMarketplaceAppTable.getColumn(MKPCRMAPPS.CATEGORY), TELEPHONY.getId()))
                        .and(Criteria.in(installedAppTable.getColumn(OMKPINSTALLEDAPPS.STATE), AppState.getActiveConnectionIds()))
        );
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(marketplaceAppTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(row.get(MKPAPPS.NAME));
    }

    /**
     * Gets the {@link OrgDataStore} for the specified organization.
     *
     * @param orgId the organization identifier
     * @return the {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}

