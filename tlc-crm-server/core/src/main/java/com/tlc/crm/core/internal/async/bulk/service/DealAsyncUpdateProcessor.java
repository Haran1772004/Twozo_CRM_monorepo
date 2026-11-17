package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link Deal} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class DealAsyncUpdateProcessor extends AsyncUpdateProcessor<Deal> {

    private final DealConverter dealConverter;
    private final DealManager dealManager;

    DealAsyncUpdateProcessor() {
        dealConverter = DealConverter.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param deal Represents the {@link Deal}
     */
    @Override
    protected void loadSummaryDetails(final Deal deal) {
        dealManager.loadSummaryDetails(deal.orgId(), deal);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.DEAL;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Deal} to be cloned
     * @return the cloned {@link Deal}
     */
    @Override
    protected Deal cloneModel(final long orgId, final Deal entity) {
        final Deal deal = super.cloneModel(orgId, entity);

        deal.setOrgId(orgId);
        deal.setId(entity.id());

        return deal;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param deal            Represents the model to be updated
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final Deal deal) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            dealConverter.setJsonToDeal(converterConfig, deal, dataJson);
        } else {
            dealConverter.convert(converterConfig, dataJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<Deal> getAbstractConfigManager() {
        return dealManager;
    }
}