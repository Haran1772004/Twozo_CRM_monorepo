package com.tlc.crm.common.iam.api.model.account.cancellation;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.internal.status.AccountCancellationErrorCodes;
import com.tlc.crm.common.validator.Group;
import com.tlc.validator.TlcModel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * This entity that holds the survey data for the account cancellation
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class Survey implements TlcModel {

    @Size(max = 255, groups = {Group.AddSurvey.class}, message = "i18n.survey.retention.reason.length.exceeded")
    @NotNull(groups = {Group.AddSurvey.class}, message = "i18n.survey.retention.reason.required")
    private String retentionReason;

    @NotNull(groups = {Group.AddSurvey.class}, message = "i18n.survey.alternate.product.required")
    private AlternateProduct alternateProduct;

    @Size(max = 255, groups = {Group.AddSurvey.class}, message = "i18n.survey.alternate.product.name.length.exceeded")
    private String alternateProductName;

    @NotNull(groups = {Group.AddSurvey.class}, message = "i18n.survey.reconsideration.required")
    private Reconsideration reconsideration;
    private JsonObject surveyJson;

    public Survey(final String retentionReason, final AlternateProduct alternateProduct, final Reconsideration reConsideration) {
        this.retentionReason = retentionReason;
        this.alternateProduct = alternateProduct;
        this.reconsideration = reConsideration;
    }

    public AlternateProduct getAlternateProduct() {
        return alternateProduct;
    }

    public String getAlternateProductName() {
        return alternateProductName;
    }

    public void setAlternateProductName(final String alternateProductName) {
        this.alternateProductName = alternateProductName;
    }

    public void setSurveyJson(final JsonObject surveyJson) {
        this.surveyJson = surveyJson;
    }

    public JsonObject getSurveyJson() {
        return surveyJson;
    }

    @Override
    public Long id() {
        return null;
    }

    @Override
    public Long orgId() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * This enum contains the alternate product types
     * </p>
     *
     * @author Muthu kumar V
     * @version 1.0
     */
    public enum AlternateProduct {

        PIPEDRIVE((short) 1),
        HUBSPOT((short) 2),
        ZOHO((short) 3),
        OTHERS((short) 4)
        ;

        final short id;

        AlternateProduct(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static AlternateProduct getAlternateProduct(final Short id) {
            return switch (id) {
                case 1 -> AlternateProduct.PIPEDRIVE;
                case 2 -> AlternateProduct.HUBSPOT;
                case 3 -> AlternateProduct.ZOHO;
                case 4 -> AlternateProduct.OTHERS;
                default -> throw ErrorCode.get(AccountCancellationErrorCodes.ALTERNATE_PRODUCT_NOT_FOUND, "i18n.survey.alternate.product.not.found");
            };
        }
    }

    /**
     * <p>
     * This enum contains the reconsideration types
     * </p>
     *
     * @author Muthu kumar v
     * @version 1.0
     */
    public enum Reconsideration {

        YES((short) 1),
        MAY_BE((short) 2),
        NO((short) 3),
        DEFINITELY_NO((short) 4)
        ;

        final short id;

        Reconsideration(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static Reconsideration getReConsiderationType(final Short id) {
            return switch (id) {
                case 1 -> Reconsideration.YES;
                case 2 -> Reconsideration.MAY_BE;
                case 3 -> Reconsideration.NO;
                case 4 -> Reconsideration.DEFINITELY_NO;
                default -> throw ErrorCode.get(AccountCancellationErrorCodes.RECONSIDERATION_NOT_FOUND, "i18n.survey.reconsideration.not.found");
            };
        }
    }
}