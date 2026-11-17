package com.tlc.crm.core.action.segment;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.list.SegmentContactSQLListInterpreter;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.core.sql.resource.OMKSSEGMENTCONTACT;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.web.WebAction;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Lists contacts for a selected segment by applying the segment criteria at runtime.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS, validateFieldPermission = true)
@WebAction(path = "/segment/contact/list")
public class SegmentContactListAction extends EntityListAction {

    private static final String SEGMENT_ID = "segmentId";
    private static final String SEARCH_TEXT_KEY = "searchText";

    private final ContactManager contactManager;

    public SegmentContactListAction() {
        contactManager = ContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the segment contact list interpreter for the given request.
     * </p>
     *
     * @param orgId                 Represents the organization ID.
     * @param userId                Represents the user ID.
     * @param request               Represents the request {@link JsonObject}.
     * @param provider              Represents the {@link Provider}.
     * @param joinLoader            Represents the {@link JoinLoader}.
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return The {@link SegmentContactSQLListInterpreter}.
     */
    @Override
    public SegmentContactSQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                     final Provider provider, final JoinLoader joinLoader,
                                                                     final I18nResolutionAdapter i18NResolutionAdapter) {
        return new SegmentContactSQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    protected Provider getProvider() {
        return Provider.CONTACT_LIST;
    }

    /**
     * <p>
     * Checks whether the field is enabled for the segment contact list.
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config}.
     * @param source Represents the {@link Source}.
     * @return True if the field is enabled, false otherwise.
     */
    @Override
    public boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isMandatoryField() || config.isEnabledForSegmentContactList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson,
                                       final EntityListInterpreter listInterpreter, final Provider provider) {
        final Table contactTable = contactManager.getTable();
        final Table segmentContactTable = Table.get(OMKSSEGMENTCONTACT.TABLE);

        listInterpreter.setWhereClause(new WhereClause(
                Criteria.eq(segmentContactTable.getColumn(OMKSSEGMENTCONTACT.SEGMENT_ID), requestJson.getLong(SEGMENT_ID)))
        );
        listInterpreter.addJoinClause(List.of(
                new JoinClause(contactTable.getPKColumn(), segmentContactTable.getColumn(OMKSSEGMENTCONTACT.CONTACT_ID)))
        );

        if (requestJson.containsKey(SEARCH_TEXT_KEY)) {
            final String searchText = requestJson.getString(SEARCH_TEXT_KEY);
            final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
            final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);

            listInterpreter.setWhereClause(new WhereClause(
                    Criteria.contains(firstNameColumn, searchText))
                    .or(Criteria.contains(lastNameColumn, searchText))
                    .or(Criteria.contains(firstNameColumn.concatWithTrim(lastNameColumn.trim()), searchText)));
        }
        super.applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    public Provider getProvider(final ListRequestData listRequestData) {
        return Provider.CONTACT_LIST;
    }

    /**
     * <p>
     * Gets the list type used for segment contact list persistence.
     * </p>
     *
     * @return The {@link ListType}.
     */
    @Override
    protected ListType getListType() {
        return ListType.SEGMENT_CONTACT_LIST;
    }

    /**
     * <p>
     * Resolves the limit JSON object from the request JSON.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param userId      Represents the user ID.
     * @param requestJson Represents the request {@link JsonObject}.
     * @return The limit {@link JsonObject}.
     */
    @Override
    protected JsonObject resolveLimitJson(final long orgId, final long userId,
                                          final JsonObject requestJson) {
        final JsonObject limitJson = requestJson.optJsonObject(ListActionFields.LIMIT);

        if (Objects.nonNull(limitJson)) {
            return limitJson;
        }

        return Json.object()
                .put(ListActionFields.START, NumericalConstant.ONE)
                .put(ListActionFields.COUNT, NumericalConstant.TWENTY_FIVE);
    }
}
