package com.tlc.crm.core.action.entityinsights;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * <p>
 * REST admin action to trigger insights data for existing entities.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/entity/insights/mgmt")
public class EntityInsightsManagement extends CrmSecureAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInsightsManagement.class);

    private static final String RECOMPUTE = "recompute";
    private static final String STARTED = "started";
    private static final String STATUS = "status";
    private static final String TYPE = "type";

    private final EntityInsightsManager entityInsightsManager;

    public EntityInsightsManagement() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TYPE);

        if (RECOMPUTE.equals(type)) {
            recomputeInsights(response);
        } else {
            throw ErrorCode.get(ErrorCodes.UNKNOWN_INPUT, "i18n.common.invalid.request.type");
        }
    }

    /**
     * <p>
     * Triggers a full recompute of all entity insights and updates the response status.
     * </p>
     *
     * @param response the {@link CrmResponse} to update with the operation status
     */
    private void recomputeInsights(final CrmResponse response) {
        LOGGER.info("Starting entity insights recompute at epoch: {}", Instant.now().toEpochMilli());

        try {
            entityInsightsManager.recomputeAllEntityInsights();
            response.put(STATUS, STARTED);
        } catch (final Exception exception) {
            LOGGER.error("Entity insights recompute for existing entities failed, message: {}", exception.getMessage(), exception);

            throw ErrorCode.get(CoreErrorCodes.INSIGHTS_DATA_LOAD_ACTION_FAILED);
        }
    }
}