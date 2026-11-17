package com.tlc.crm.core.api.contact.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.dropdown.contact.ContactLifecycleStage;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model that represents the life cycle stage rule details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LifeCycleRuleDetails implements TlcModel {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private LifeCycleStage stage;

    private LifeCycleStatus status;

    private final LifeCycleRuleType type;

    public LifeCycleRuleDetails(final LifeCycleRuleType lifeCycleRuleType) {
        this.type = lifeCycleRuleType;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public LifeCycleRuleType getType() {
        return type;
    }

    public LifeCycleStage getStage() {
        return stage;
    }

    public void setStage(final LifeCycleStage stage) {
        this.stage = stage;
    }

    public LifeCycleStatus getStatus() {
        return status;
    }

    public void setStatus(final LifeCycleStatus status) {
        this.status = status;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Provides different types of the lifecycle stage update rules.
     * </p>
     */
    public enum LifeCycleRuleType {

        DEAL_CREATION((short) 1, ContactLifecycleStage.SALES_QUALIFIED_LEAD),
        DEAL_WON((short) 2, ContactLifecycleStage.CUSTOMER),
        ;

        private final short id;
        private final ContactLifecycleStage defaultContactLifecycleStage;

        LifeCycleRuleType(final short id, final ContactLifecycleStage defaultContactLifecycleStage) {
            this.id = id;
            this.defaultContactLifecycleStage = defaultContactLifecycleStage;
        }

        public short getId() {
            return id;
        }

        public static LifeCycleRuleType get(final short id) {
            return switch (id) {
                case 1 -> DEAL_CREATION;
                case 2 -> DEAL_WON;
                default -> throw ErrorCode.get(ContactErrorCodes.INVALID_LIFECYCLE_STAGE_RULE);
            };
        }

        public ContactLifecycleStage getDefaultContactLifecycleStage() {
            return defaultContactLifecycleStage;
        }
    }
}
