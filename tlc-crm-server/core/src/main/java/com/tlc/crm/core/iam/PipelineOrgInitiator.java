package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An action performed by After the organization is established,the default pipeline for that particular organization is generated.
 * </p>
 *
 * @author karthickV
 * @version 1.0
 */
@OrgInitiator(name = "pipeline")
public class PipelineOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineOrgInitiator.class);

    private final PipelineManager pipelineManager;

    public PipelineOrgInitiator() {
        pipelineManager = PipelineManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final long userId = crmUser.id();
        final Pipeline pipeline = new Pipeline();
        LOGGER.info("Org Init Action: Pipeline initialization started. Org ID: {}, User ID: {}", orgId, userId);

        pipeline.setOrgId(orgId);
        pipeline.setName(PipelineFields.GENERAL.getName());
        pipeline.setDefault(true);
        pipeline.setOrder((short) 1);

        for (short index = 1; index <= 3; index++) {
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setOrgId(orgId);
            pipelineStage.setOrder(index);

            switch (PipelineStage.StageCategory.valueOf(index)) {
                case NEW -> {
                    pipelineStage.setDefault(true);
                    pipelineStage.setName(PipelineStageFields.NEW.getName());
                    pipelineStage.setProbability(100.0);
                }
                case WON -> {
                    pipelineStage.setName(PipelineStageFields.WON.getName());
                    pipelineStage.setProbability(100.0);
                }
                case LOST -> {
                    pipelineStage.setName(PipelineStageFields.LOST.getName());
                    pipelineStage.setProbability(0.0);
                }
            }
            pipeline.addPipelineStages(pipelineStage);
        }
        pipelineManager.create(pipeline);
        final Long pipelineId = pipeline.id();

        pipelineManager.loadUserPreferredPipeline(orgId, userId, pipelineId);
        crmUser.setPipelineId(pipelineId);
        pipelineManager.saveEntity(true, crmUser);
        LOGGER.info("Org Init Action: Pipeline initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
