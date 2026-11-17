package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.Map;

/**
 * Holds the necessary information required to retrieve the step option from the model.
 *
 * @param tlcModel     Refers the {@link TlcModel}.
 * @param stepOptionId Represents the step option id.
 * @param groupedUsers Represents the users grouped by its id.
 * @param source       Represents the source of the model.
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public record StepOptionInfoV2(TlcModel tlcModel, Long stepOptionId, Map<Long, User> groupedUsers, Source source) {

    /**
     * Gets the {@link TlcModel}.
     *
     * @param <T> Represents the type of the model.
     * @return The {@link TlcModel}.
     */
    public <T extends TlcModel> T model() {
        return (T) tlcModel;
    }

}
