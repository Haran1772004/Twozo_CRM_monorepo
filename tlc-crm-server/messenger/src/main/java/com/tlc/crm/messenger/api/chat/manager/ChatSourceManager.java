package com.tlc.crm.messenger.api.chat.manager;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.messenger.config.manager.AbstractMessengerManager;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.l10n.model.country.Country;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages chat-source related data base actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ChatSourceManager extends AbstractMessengerManager {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final DealManager dealManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final MonetaryManager monetaryManager;
    private final FieldService fieldService;

    private ChatSourceManager() {
        dealManager = DealManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        contactManager = ContactManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        companyManager = CompanyManager.getInstance();
        fieldService = FieldService.getInstance();
    }

    private static final class Instance {
        private static final ChatSourceManager FB_SOURCE_CHAT_MANAGER = new ChatSourceManager();
    }

    /**
     * <p>
     * Gets {@link ChatSourceManager}.
     * </p>
     *
     * @return {@link ChatSourceManager}.
     */
    public static ChatSourceManager getInstance() {
        return Instance.FB_SOURCE_CHAT_MANAGER;
    }

    /**
     * <p>
     * Checks whether the chat exists.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param dealId Deal's id.
     * @return whether the chat exists.
     */
    public boolean isDealExists(final Long orgId, final Long dealId) {
        return dealManager.exists(orgId, dealId);
    }

    /**
     * <p>
     * Checks whether the chat exists.
     * </p>
     *
     * @param orgId     Organization's id.
     * @param contactId Contact's id.
     * @return whether the chat exists.
     */
    public boolean isContactExists(final Long orgId, final Long contactId) {
        return contactManager.exists(orgId, contactId);
    }

    /**
     * <p>
     * Gets {@link Deal}.
     * </p>
     *
     * @param orgId  Organization's id.
     * @param dealId Deal id.
     * @return {@link Deal}.
     */
    public Optional<Deal> getDeal(final Long orgId, final Long dealId) {
        final Table dealTable = dealManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table pipelineTable = pipelineManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Column pipelineIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);
        final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column monetaryColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column pipelinePKColumn = pipelineTable.getPKColumn();
        final Column pipelineStagePKColumn = pipelineStageTable.getPKColumn();
        final Column monetaryPKColumn = monetaryTable.getPKColumn();
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Column ownerIdColumn = dealTable.getColumn(MDODEAL.OWNER_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(pipelineIdColumn, pipelinePKColumn),
                new JoinClause(pipelineStageIdColumn, pipelineStagePKColumn), new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryPKColumn)));

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.ORG_ID), pipelineStageIdColumn,
                dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), pipelineIdColumn, pipelineTable.getColumn(MDOPIPELINE.NAME),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), monetaryTable.getColumn(OMETMONETARY.ORG_ID),
                monetaryColumn, dealStateColumn, ownerIdColumn, dealTable.getColumn(MDODEAL.STATUS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        final DataContainer dataContainer = getSecureReadableOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        } else {
            final Deal deal = new Deal();
            final Row dealRow = dataContainer.getRow(dealTable);
            final Row pipelineRow = dataContainer.getRow(pipelineTable, new WhereClause(Criteria.eq(pipelinePKColumn,
                    dealRow.get(MDODEAL.PIPELINE_ID))));
            final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelineStagePKColumn, dealRow.get(MDODEAL.PIPELINE_STAGE_ID))));
            final Row monetaryRow = dataContainer.getRow(monetaryTable, new WhereClause(Criteria.eq(monetaryPKColumn,
                    dealRow.get(MDODEAL.MONETARY_ID))));
            final Long salesOwnerId = dealRow.get(ownerIdColumn);
            final Integer stateId = dealRow.get(dealStateColumn);

            if (Objects.nonNull(monetaryRow)) {
                final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
                deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
            }

            if (Objects.nonNull(pipelineRow)) {
                final Pipeline pipeline = new Pipeline();

                pipeline.setId(pipelineRow.getPKValue());
                pipeline.setName(pipelineRow.get(MDOPIPELINE.NAME));
                deal.setPipeline(pipeline);
            }

            if (Objects.nonNull(pipelineStageRow)) {
                final PipelineStage pipelineStage = new PipelineStage();

                pipelineStage.setId(pipelineStageRow.getPKValue());
                pipelineStage.setName(pipelineStageRow.get(MDOPIPELINESTAGE.NAME));
                deal.setPipelineStage(pipelineStage);
            }

            if (Objects.nonNull(stateId)) {
                deal.setState(Deal.State.getStateById(stateId));
            }
            deal.setId(dealRow.get(MDODEAL.ID));
            deal.setOrgId(dealRow.get(MDODEAL.ORG_ID));
            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
            deal.setOwnerId(salesOwnerId);
            deal.setStatus(dealRow.get(MDODEAL.STATUS));

            return Optional.of(deal);
        }
    }

    /**
     * <p>
     * Gets {@link Contact}.
     * </p>
     *
     * @param orgId     Organization's id.
     * @param contactId Contact id.
     * @return {@link Contact}.
     */
    public Optional<Contact> getContact(final Long orgId, final Long contactId) {
        final Long companyFieldId = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                MessengerFields.COMPANY).get(MessengerFields.COMPANY).id();
        final Table contactTable = contactManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column entityIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), leftJoin),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactPkColumn, entityIdColumn, leftJoin),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), emailTable.getColumn(OMETEMAIL.VALUE),
                contactTable.getColumn(MCOCONTACT.LINKEDIN), contactTable.getColumn(MCOCONTACT.FACEBOOK),
                emailTable.getColumn(OMETEMAIL.TYPE), phoneTable.getColumn(OMETPHONE.NUMBER),
                phoneTable.getColumn(OMETPHONE.TYPE), phoneTable.getColumn(OMETPHONE.COUNTRY_ID),
                companyTable.getColumn(MCOCOMPANY.NAME), companyPkColumn));
        selectQuery.addSelectClause(contactCompanyTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, contactId)));
        final DataContainer dataContainer = getSecureReadableOrgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        } else {
            final Contact contact = new Contact();
            final Row contactRow = dataContainer.getRow(contactTable);
            final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));
            final Row companyRow = Objects.isNull(contactCompanyRow) ? null : dataContainer.getRow(companyTable, (Long) contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID));
            final Row emailRow = dataContainer.getRow(emailTable);
            final Row phoneRow = dataContainer.getRow(phoneTable);

            contact.setOrgId(orgId);
            contact.setId(contactId);
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
            contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));

            if (Objects.nonNull(companyRow)) {
                final Company company = new Company();

                company.setOrgId(orgId);
                company.setId(companyRow.getPKValue());
                company.setName(companyRow.get(MCOCOMPANY.NAME));
                contact.setCompany(company);
            }

            if (Objects.nonNull(emailRow)) {
                final Email primaryEmail = new Email();
                final Integer type = emailRow.get(OMETPHONE.TYPE);

                primaryEmail.setId(emailRow.getPKValue());
                primaryEmail.setValue(emailRow.get(OMETEMAIL.VALUE));

                if (Objects.nonNull(type)) {
                    primaryEmail.setType(Email.Type.valueOf(type));
                }
                contact.setPrimaryEmail(primaryEmail);
            }

            if (Objects.nonNull(phoneRow)) {
                final Phone primaryPhone = new Phone();
                final Integer countryId = phoneRow.get(OMETPHONE.COUNTRY_ID);
                final Integer type = phoneRow.get(OMETPHONE.TYPE);

                primaryPhone.setId(phoneRow.getPKValue());
                primaryPhone.setNumber(phoneRow.get(OMETPHONE.NUMBER));

                if (Objects.nonNull(type)) {
                    primaryPhone.setType(Phone.Type.valueOf(type));
                }

                if (Objects.nonNull(countryId)) {
                    primaryPhone.setCountry(Country.getCountryById(countryId));
                }
                contact.setPrimaryPhone(primaryPhone);
            }
            return Optional.of(contact);
        }
    }
}