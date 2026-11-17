package com.tlc.crm.webform.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.commons.util.TypeChecker;

import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.DmlUtil;

import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.Template;
import com.tlc.crm.webform.api.model.FormStyle;
import com.tlc.crm.webform.api.model.FieldStyle;
import com.tlc.crm.webform.api.model.SubmitButtonStyle;
import com.tlc.crm.webform.sql.resource.MWFSUBMITACTION;
import com.tlc.crm.webform.sql.resource.MWFTEMPLATE;
import com.tlc.crm.webform.sql.resource.MWFTEMPLATEELEMENTS;
import com.tlc.crm.webform.sql.resource.MWFTEMPLATEFIELDS;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides the {@link Template} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TemplateManager extends AbstractReadableConfigManager<Template> {

    private final WebFormManager webFormManager;

    private static class Instance {
        private static final TemplateManager INSTANCE = new TemplateManager();
    }

    private TemplateManager() {
        webFormManager = WebFormManager.getInstance();
    }

    /**
     * Gets the {@link TemplateManager} instance.
     *
     * @return {@link TemplateManager}.
     */
    public static TemplateManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MWFTEMPLATE.TABLE);
    }

    /**
     * Gets the template field table.
     *
     * @return The {@link Table} of template field.
     */
    private Table getTemplateFieldTable() {
        return Table.get(MWFTEMPLATEFIELDS.TABLE);
    }

    /**
     * Gets the template element table.
     *
     * @return The {@link Table} of template element.
     */
    private Table getTemplateElementTable() {
        return Table.get(MWFTEMPLATEELEMENTS.TABLE);
    }

    /**
     * Gets all the templates.
     *
     * @return The Collection of {@link Template}.
     */
    public Collection<Template> getAllTemplates() {
        final Table table = getTable();
        final DataContainer dataContainer = readableDataStore().get(table,
                new WhereClause(Criteria.notEq(table.getPKColumn(), null)));

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param row Row that holds the information of {@link Template}.
     * @return The {@link Template}.
     */
    @Override
    public Template convertRowToModel(final Row row) {
        final Long id = row.getPKValue();
        final Table templateTable = getTable();
        final Table formStyleTable = webFormManager.getFormStyleTable();
        final Table fieldStyleTable = webFormManager.getFieldStyleTable();
        final Table submitButtonStyleTable = webFormManager.getSubmitButtonStyleTable();
        final Table styletable = webFormManager.getStyleTable();
        final Table submitActionTable = webFormManager.getSubmitActionTable();
        final Table templateFieldTable = getTemplateFieldTable();
        final Table templateElementTable = getTemplateElementTable();
        final SelectQuery selectQuery = SelectQuery.get(formStyleTable);
        final Column pkColumn = templateTable.getPKColumn();

        selectQuery.addJoinClause(DmlUtil.getJoins(formStyleTable, fieldStyleTable, submitButtonStyleTable,
                styletable, templateTable, submitActionTable, templateFieldTable));
        selectQuery.addJoinClause(List.of(new JoinClause(pkColumn, templateElementTable.getColumn(MWFTEMPLATEELEMENTS.TEMPLATE_ID),
                JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(styletable, templateElementTable, templateFieldTable, formStyleTable,
                fieldStyleTable, submitButtonStyleTable, submitActionTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pkColumn, id)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return new Template(id, row.get(MWFTEMPLATE.NAME), row.get(MWFTEMPLATE.DESCRIPTION),
                row.get(MWFTEMPLATE.SUBMIT_BUTTON_NAME), getFormStyle(dataContainer), getFieldStyle(dataContainer),
                getSubmitButtonStyle(dataContainer), getSubmitAction(dataContainer), getTemplateFieldProperties(dataContainer));
    }

    /**
     * Gets the submitButton style row from the data container and converts into {@link SubmitButtonStyle}.
     *
     * @param dataContainer A container that holds the template details.
     * @return {@link SubmitButtonStyle} details.
     */
    private SubmitButtonStyle getSubmitButtonStyle(final DataContainer dataContainer) {
        final Row submitButtonStyleRow = dataContainer.getRow(webFormManager.getSubmitButtonStyleTable());

        return Objects.nonNull(submitButtonStyleRow)
                ? webFormManager.convertSubmitButtonStyleToModel(submitButtonStyleRow) : null;
    }

    /**
     * Gets the field style row from the data container and converts into {@link FieldStyle}.
     *
     * @param dataContainer A container that holds the template details.
     * @return {@link FieldStyle} details.
     */
    private FieldStyle getFieldStyle(final DataContainer dataContainer) {
        final Row fieldStyleRow = dataContainer.getRow(webFormManager.getFieldStyleTable());

        return Objects.nonNull(fieldStyleRow) ? webFormManager.convertFieldStyleToModel(fieldStyleRow) : null;
    }

    /**
     * Gets the form style row from the data container and converts into {@link FormStyle}.
     *
     * @param dataContainer A container that holds the template details.
     * @return {@link FormStyle} details.
     */
    private FormStyle getFormStyle(final DataContainer dataContainer) {
        final Row formStyleRow = dataContainer.getRow(webFormManager.getFormStyleTable());

        return Objects.nonNull(formStyleRow) ? webFormManager.convertFormStyleRowToModel(formStyleRow) : null;
    }

    /**
     * Fetches all the {@link FormFieldProperty} from the provided data container.
     *
     * @param dataContainer A container that holds the template details.
     * @return Collection of {@link FormFieldProperty}.
     */
    private Collection<FormFieldProperty> getTemplateFieldProperties(final DataContainer dataContainer) {
        final Collection<FormFieldProperty> formFieldProperties = dataContainer.getRows(getTemplateFieldTable()).filter(Objects::nonNull)
                .map(row -> webFormManager.convertFormFieldToModel(row, true)).collect(Collectors.toList());

        formFieldProperties.addAll(dataContainer.getRows(getTemplateElementTable()).filter(Objects::nonNull)
                .map(row -> webFormManager.convertFormElementToModel(row, true)).toList());

        return formFieldProperties;
    }

    /**
     * Gets the submit action row from the data container and converts into {@link SubmitAction}.
     *
     * @param dataContainer A container that holds the template details.
     * @return {@link SubmitAction} details.
     */
    private SubmitAction getSubmitAction(final DataContainer dataContainer) {
        final Row submitActionRow = dataContainer.getRow(webFormManager.getSubmitActionTable());

        if (Objects.nonNull(submitActionRow)) {
            final SubmitAction submitAction = new SubmitAction();
            final Object value = submitActionRow.get(MWFSUBMITACTION.THANK_YOU_MESSAGE);
            final String redirectUrl = submitActionRow.get(MWFSUBMITACTION.REDIRECT_URL);
            final I18nKey i18nKey = TypeChecker.isString(value) ? new I18nKey((String) value) : (I18nKey) value;

            submitAction.setId(submitActionRow.getPKValue());
            submitAction.setI18nMessage(i18nKey);
            submitAction.setThankYouMessageEnabled(Objects.nonNull(i18nKey));
            submitAction.setRedirectUrl(redirectUrl);
            submitAction.setRedirectEnabled(Objects.nonNull(redirectUrl));
            submitAction.setEmailNotificationEnabled(submitActionRow.get(MWFSUBMITACTION.IS_EMAIL_NOTIFICATION_ENABLED));
            submitAction.setContactCreationEnabled(submitActionRow.get(MWFSUBMITACTION.IS_CONTACT_CREATION_ENABLED));
            submitAction.setDealCreationWithContactEnabled(submitActionRow.get(MWFSUBMITACTION.IS_DEAL_CREATION_WITH_CONTACT_ENABLED));

            return submitAction;
        }

        return null;
    }

    /**
     * <p>
     * Gets the template object from given template id.
     * </p>
     *
     * @param templateId Represents the templateId
     * @return The {@link Template}
     */
    public Template getTemplate(final Long templateId) {
        return convertRowToModel(readableDataStore().get(getTable(), templateId));
    }

}