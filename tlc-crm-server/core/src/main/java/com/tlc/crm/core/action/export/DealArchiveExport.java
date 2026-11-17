package com.tlc.crm.core.action.export;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * Handles export operations for archived deals
 *
 * @author kishore J
 * @version 1.0
 */
@DataExporter(name = "dealArchive")
public class DealArchiveExport extends DealExportAction {

    private final DealManager dealManager;

    public DealArchiveExport() {
        dealManager = DealManager.getInstance();
    }

    /**
     * <p>
     * Applies the default criteria for the entity list action
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    @Override
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(dealManager.getTable().getColumn(MDODEAL.STATUS), Deal.Status.ARCHIVED.getId())));
    }

    /**
     * {@inheritDoc}
     *
     * @param config the field configuration settings
     * @return {@code true} if the configuration is enabled for web, otherwise {@code false}
     */
    protected boolean isEnabled(final FieldConfig.Config config) {
        return config.isEnabledForDealArchiveList();
    }
}
