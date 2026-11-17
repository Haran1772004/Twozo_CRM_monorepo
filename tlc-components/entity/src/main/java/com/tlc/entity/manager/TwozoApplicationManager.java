package com.tlc.entity.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.manager.ApplicationManager;
import com.tlc.commons.sql.resource.TZAPPLICATION;

import com.tlc.entity.model.TwozoApplication;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Objects;

/**
 * Manages the DB related actions of the {@link TwozoApplication}.
 *
 * @author Sathish
 * @version 1.0
 */
public class TwozoApplicationManager implements ApplicationManager<TwozoApplication> {

    private final AdminDataStore adminDataStore;

    private TwozoApplicationManager() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static final class InstanceHolder {
        private static final TwozoApplicationManager TWOZO_APPLICATION_MANAGER = new TwozoApplicationManager();
    }

    public static TwozoApplicationManager getInstance() {
        return InstanceHolder.TWOZO_APPLICATION_MANAGER;
    }

    public Table getTable() {
        return Table.get(TZAPPLICATION.TABLE);
    }

    /**
     * Retrieves an Application by its unique identifier.
     *
     * @param id The unique id of the Application
     * @return The Application instance corresponding to the given id, or null if not found
     */
    @Override
    public TwozoApplication get(final long id) {
        final Table applicationTable = getTable();
        final Row applicationRow = adminDataStore.get(applicationTable, id);

        if (Objects.isNull(applicationRow)) {
            throw ErrorCode.get(ErrorCodes.APPLICATION_NOT_FOUND);
        }
        return convertRowToApplication(applicationRow);
    }

    /**
     * Retrieves an Application by its name.
     *
     * @param appName The name of the Application
     * @return The Application instance corresponding to the given name, or null if not found
     */
    @Override
    public TwozoApplication getByName(final String appName) {
        final Table applicationTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(applicationTable.getColumn(TZAPPLICATION.NAME), appName));
        final Row applicationRow = adminDataStore.get(applicationTable, whereClause).getRow(applicationTable);

        if (Objects.isNull(applicationRow)) {
            throw ErrorCode.get(ErrorCodes.APPLICATION_NOT_FOUND);
        }
        return convertRowToApplication(applicationRow);
    }

    /**
     * Converts the {@link Row} into {@link TwozoApplication}.
     *
     * @param applicationRow Holds the application details.
     * @return {@link TwozoApplication}.
     */
    private TwozoApplication convertRowToApplication(final Row applicationRow) {
        final TwozoApplication twozoApplication = new TwozoApplication();

        twozoApplication.setId(applicationRow.getPKValue());
        twozoApplication.setName(applicationRow.get(TZAPPLICATION.NAME));
        return twozoApplication;
    }
}
