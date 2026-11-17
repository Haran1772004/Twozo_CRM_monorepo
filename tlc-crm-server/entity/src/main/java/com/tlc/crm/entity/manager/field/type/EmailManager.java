package com.tlc.crm.entity.manager.field.type;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Email.Type;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Email} related DB actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class EmailManager extends AbstractConfigManager<Email> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailManager.class);

    private static class Instance {
        private static final EmailManager INSTANCE = new EmailManager();
    }

    private EmailManager() {

    }

    /**
     * Gets the instance of {@link EmailManager}
     *
     * @return {@link EmailManager}
     */
    public static EmailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMETEMAIL.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   represents the collection of email IDS
     * @return {@link Collection<Email>}
     */
    @Override
    public Collection<Email> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(EntityErrorCodes.EMAIL_NOT_FOUND, String.format("Org ID : %d, Entity ID : %s", orgId, ids));
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the Organisation ID
     * @param ids   which contains the collection of email IDs
     * @return
     */
    @Override
    public Collection<Email> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param email which contains the email data
     * @return {@link Email} which contains the email data
     */
    @Override
    public Email create(final Email email) {
        final DataContainer targetContainer = DataContainer.create();

        loadEmailIntoContainer(true, email, targetContainer);
        orgDataStore(email.orgId()).commitChanges(targetContainer);

        return email;
    }

    /**
     * <p>
     * Loads the email details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param email           An entity that holds the email attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadEmailIntoContainer(final boolean isCreateEvent, final Email email, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(email.orgId()).get(getTable(), List.of(email.id()));

        loadEmailIntoContainer(isCreateEvent, email, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the email details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param email           An entity that holds the email attributes
     * @param sourceContainer Holds the user defined and system field values of existing email
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadEmailIntoContainer(final boolean isCreateEvent, final Email email,
                                        final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table emailTable = getTable();
        final Long id = email.id();
        final Optional<Row> optionalRow = Objects.isNull(sourceContainer) ? Optional.empty() :
                Optional.ofNullable(sourceContainer.getRow(emailTable, id)).map(Row::new);
        final Row emailRow = isCreateEvent || optionalRow.isEmpty() ? new Row(emailTable) : optionalRow.get();

        if (emailRow.isNewRow()) {
            emailRow.setOrgId(email.orgId());
            email.setId(emailRow.getPKValue());
        }
        emailRow.set(OMETEMAIL.TYPE, email.getType().getId());
        emailRow.set(OMETEMAIL.VALUE, email.getValue());

        targetContainer.updateRow(emailRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  represents the Organisation ID
     * @param emails represents the {@link Collection<Email>}
     * @return {@link Map} which contains the response data
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Email> emails) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = emails.stream().map(Email::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        emails.forEach(email -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadEmailIntoContainer(false, email, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(email.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Email update failed Org ID : {} ID : {} reason : {}", orgId, email.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed")));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link Email}
     * </p>
     *
     * @param row Email row to be converted
     * @return {@link Email}
     */
    public Email convertRowToModel(final Row row) {
        final Email email = new Email();
        final Integer type = row.get(OMETEMAIL.TYPE);

        email.setId(row.getPKValue());
        email.setOrgId(row.getOrgId());
        email.setValue(row.get(OMETEMAIL.VALUE));

        if (Objects.nonNull(type)) {
            email.setType(Type.valueOf(type));
        }

        return email;
    }
}