package com.tlc.crm.entity.listeners.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.cache.UserCache;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.FilterProvider;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.entity.service.list.ProviderService;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A Listener that listens create/update/delete actions of user entry.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserRowListener implements CompleteRowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRowListener.class);
    private static final String EQUAL = "equal";

    private final FilterManager filterManager;
    private final ProviderService providerService;
    private final ComparatorService comparatorService;
    private final FieldSourceService fieldSourceService;
    private final FilterFieldService filterFieldService;
    private final UserService userService;
    private final UserCache userCache;

    private final static class Instance {
        private static final UserRowListener USER_ROW_LISTENER = new UserRowListener();
    }

    public static UserRowListener getInstance() {
        return Instance.USER_ROW_LISTENER;
    }

    private UserRowListener() {
        filterManager = FilterManager.getInstance();
        providerService = ProviderService.getInstance();
        comparatorService = ComparatorService.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        filterFieldService = FilterFieldService.getInstance();
        userService = UserService.getInstance();
        this.userCache = UserCache.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param userRows Refers the collection of user rows.
     */
    @Override
    public void addRows(final Collection<Row> userRows) {
        for (final Provider provider : Provider.getAll()) {
            for (final Row userRow : userRows) {
                final Long userId = userRow.getPKValue();
                final Long orgId = userRow.getOrgId();
                final Filter filter = new Filter(Filter.Category.SALES_OWNER);
                final FilterProvider filterProvider = new FilterProvider();
                final FilterVisibility filterVisibility = new FilterVisibility();

                filterVisibility.setOrgId(orgId);
                filterVisibility.setVisibility(Visibility.EVERY_ONE);
                filterVisibility.setVisibilityPermission(VisibilityPermission.CAN_VIEW);
                filterVisibility.setPreventShare(true);
                filterProvider.setId(providerService.getIdByProvider(provider));
                filter.setCreator(userService.getUser(orgId, userId));
                filter.setName(userId.toString());
                filter.setDefault(true);
                filter.setFilterProvider(filterProvider);
                filter.addCriteria(buildCriteria(orgId, userId, provider));
                filter.setFilterVisibility(filterVisibility);
                filter.setOrgId(orgId);

                try {
                    filterManager.create(filter);
                } catch (Exception exception) {
                    LOGGER.error("Filter create action failed. Org ID: {}, User ID: {}, Provider: {}" , orgId, userId, provider.name());
                }
            }
        }
    }

    /**
     * <p>
     * Listens to the update operation and removes the users from the cache.
     * </p>
     *
     * @param rows Represents the collection of row data
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        rows.forEach(row -> userCache.removeUser(row.getOrgId(), row.getPKValue()));
    }

    /**
     * {@inheritDoc}
     *
     * @param userRows Refers the collection of user rows.
     */
    @Override
    public void deleteRows(final Collection<Row> userRows) {
        final Collection<Long> userIds = new HashSet<>();
        Long orgId = null;

        for (final Row userRow : userRows) {
            if (Objects.isNull(orgId)) {
                orgId = userRow.getOrgId();
            }
            userIds.add(userRow.getPKValue());
        }

        if (Objects.nonNull(orgId)) {
            filterManager.deleteSalesOwnerFilter(orgId, userIds);
        }
    }

    /**
     * <p>
     * Builds the criteria for the sales owner filter.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param userId   Represents the user id.
     * @param provider Represents the data list provider.
     * @return {@link FilterCriteria}.
     */
    private FilterCriteria buildCriteria(final long orgId, final long userId, final Provider provider) {
        final FilterCriteria filterCriteria = new FilterCriteria();
        final CriteriaValue criteriaValue = new CriteriaValue();
        final Source source = provider.getSource();
        final FieldSource fieldSource = fieldSourceService.getSource(source.getName());
        final Collection<CriteriaField> criteriaFields = filterFieldService.getFieldsByName(orgId,
                Source.isPrimarySource(source) ? FilterFields.SALES_OWNER.getName() : ActivityFields.ASSIGNEE_ID.getName(), true);

        final Optional<CriteriaField> optionalField = criteriaFields.stream().filter(field ->
                Objects.equals(field.getSourceId(), fieldSource.id())).findFirst();

        filterCriteria.setCriteriaField(optionalField.orElseThrow(() -> ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND)));

        criteriaValue.setValueEntityId(userId);
        filterCriteria.setComparator(comparatorService.getByName(EQUAL));
        filterCriteria.addCriteriaValue(criteriaValue);
        filterCriteria.setFieldSource(fieldSource);
        filterCriteria.setConnector(FilterConnector.AND);

        return filterCriteria;
    }
}
