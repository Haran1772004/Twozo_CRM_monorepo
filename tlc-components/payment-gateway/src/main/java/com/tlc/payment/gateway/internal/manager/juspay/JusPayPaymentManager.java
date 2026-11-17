package com.tlc.payment.gateway.internal.manager.juspay;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.sql.resource.TZAPPLICATION;

import com.tlc.entity.manager.TwozoApplicationManager;
import com.tlc.entity.model.TwozoApplication;

import com.tlc.payment.gateway.internal.status.PaymentGatewayErrorCodes;
import com.tlc.payment.gateway.manager.PaymentManager;
import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PayerLocalDetails;
import com.tlc.payment.gateway.sql.resource.MPGPAYER;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Optional;

/**
 * Manages payment operations for the JusPay gateway, including creation, deletion,
 * and lookup of payer and payment details.
 * <p>
 * Implements the {@link PaymentManager} interface and orchestrates persistence,
 * payment service communication, and application-payer relationships.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class JusPayPaymentManager implements PaymentManager {

    private final AdminDataStore adminDataStore;
    private final TwozoApplicationManager applicationManager;

    private JusPayPaymentManager() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
        applicationManager = TwozoApplicationManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final JusPayPaymentManager JUS_PAY_PAYMENT_MANAGER = new JusPayPaymentManager();
    }

    public static JusPayPaymentManager getInstance() {
        return InstanceHolder.JUS_PAY_PAYMENT_MANAGER;
    }

    /**
     * Retrieves the database table associated with payers for JusPay.
     *
     * @return MPGPAYER table object
     */
    public Table getPayerTable() {
        return Table.get(MPGPAYER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param payer The payer object containing details of the payer to be created.
     */
    @Override
    public void createPayer(final Payer payer) {
        final Table payerTable = getPayerTable();
        final PayerLocalDetails payerLocalDetails = payer.getLocalDetails();
        final CountQuery countQuery = CountQuery.get(payerTable);
        final WhereClause whereClause = buildPayerLocalDetailsCriteria(payerLocalDetails);

        countQuery.addJoinClause(new JoinClause(payerTable.getColumn(MPGPAYER.APPLICATION_ID), applicationManager.getTable().getPKColumn()));
        countQuery.setWhereClause(whereClause);

        if (0 > adminDataStore.get(countQuery)) {
            throw ErrorCode.get(PaymentGatewayErrorCodes.PAYER_ALREADY_EXISTS);
        }
        final Row payerRow = new Row(payerTable);
        final TwozoApplication application = payerLocalDetails.getApplication();

        payerRow.set(MPGPAYER.APPLICATION_ID, applicationManager.getByName(application.getName()).getId());
        payerRow.set(MPGPAYER.LOCAL_IDENTITY, payerLocalDetails.getLocalIdentity());
        payerRow.set(MPGPAYER.IDENTITY_TYPE, payerLocalDetails.getIdentityType().getName());
        payerRow.set(MPGPAYER.REFERENCE_ID, payer.getRefenceId());
        adminDataStore.addRow(payerRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param payer The payer object containing details of the payer to be deleted.
     */
    @Override
    public void deletePayer(final Payer payer) {
        final Table payerTable = getPayerTable();
        final PayerLocalDetails payerLocalDetails = payer.getLocalDetails();
        final String referenceId = getPayerReferenceId(payerLocalDetails);

        payer.setRefenceId(referenceId);
        final DeleteQuery deleteQuery = DeleteQuery.get(payerTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(payerTable.getColumn(MPGPAYER.REFERENCE_ID), referenceId)));
        adminDataStore.delete(deleteQuery);
    }

    /**
     * Retrieves the payer reference ID based on local payer details.
     *
     * @param payerLocalDetails details used to identify the payer
     * @return reference ID string
     * @throws ErrorCode if payer is not found
     */
    @Override
    public String getPayerReferenceId(final PayerLocalDetails payerLocalDetails) {
        final Table payerTable = getPayerTable();
        final SelectQuery selectQuery = SelectQuery.get(payerTable);

        selectQuery.addSelectClause(payerTable.getColumn(MPGPAYER.REFERENCE_ID));
        selectQuery.addJoinClause(new JoinClause(payerTable.getColumn(MPGPAYER.APPLICATION_ID), applicationManager.getTable().getPKColumn()));
        selectQuery.setWhereClause(buildPayerLocalDetailsCriteria(payerLocalDetails));
        final Row payerRow = adminDataStore.get(selectQuery).getRow(payerTable);

        return Optional.ofNullable(payerRow).map(row -> (String) row.get(MPGPAYER.REFERENCE_ID))
                .orElseThrow(() -> ErrorCode.get(PaymentGatewayErrorCodes.PAYER_NOT_FOUND));
    }

    /**
     * Builds the query criteria for payer local details.
     *
     * @param payerLocalDetails the local details of the payer
     * @return where clause representing criteria for query
     */
    private WhereClause buildPayerLocalDetailsCriteria(final PayerLocalDetails payerLocalDetails) {
        final Table payerTable = getPayerTable();

        return new WhereClause(Criteria.eq(applicationManager.getTable().getColumn(TZAPPLICATION.NAME), payerLocalDetails.getApplication().getName()))
                .and(Criteria.eq(payerTable.getColumn(MPGPAYER.IDENTITY_TYPE), payerLocalDetails.getIdentityType().getName()))
                .and(Criteria.eq(payerTable.getColumn(MPGPAYER.LOCAL_IDENTITY), payerLocalDetails.getLocalIdentity()));
    }
}
