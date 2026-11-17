package com.tlc.crm.security.internal.permission.manager;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.IAMPACTION;
import com.tlc.crm.security.sql.resource.IAMPACTIONDEPENDENCY;
import com.tlc.crm.security.sql.resource.IAMPCATEGORY;
import com.tlc.crm.security.sql.resource.IAMPSUBCATEGORY;
import com.tlc.crm.security.sql.resource.IAMPSUBCATEGORYACTION;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link RolePermission} metadata related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PermissionMetaDataManager
 */
public class PermissionMetaDataManagerImpl implements PermissionMetaDataManager {

    protected final ReadableDataStore readableDataStore;

    private static class Instance {
        private static final PermissionMetaDataManagerImpl INSTANCE = new PermissionMetaDataManagerImpl();
    }

    protected PermissionMetaDataManagerImpl() {
        readableDataStore = DataStoreProvider.getReadableDataStore();
    }

    public static PermissionMetaDataManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the table of the category.
     * </p>
     *
     * @return IAMPCategory table.
     */
    private Table getCategoryTable() {
        return Table.get(IAMPCATEGORY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the sub category.
     * </p>
     *
     * @return IAMPSubCategory table.
     */
    private Table getSubCategoryTable() {
        return Table.get(IAMPSUBCATEGORY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the action.
     * </p>
     *
     * @return IAMPAction table.
     */
    private Table getActionTable() {
        return Table.get(IAMPACTION.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the sub category action.
     * </p>
     *
     * @return IAMPSubCategoryAction table.
     */
    private Table getSubCategoryActionTable() {
        return Table.get(IAMPSUBCATEGORYACTION.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the action dependencies.
     * </p>
     *
     * @return IAMPActionDependency table.
     */
    private Table getActionDependenciesTable() {
        return Table.get(IAMPACTIONDEPENDENCY.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Category} by using the category id.
     * </p>
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    @Override
    public Category getCategory(final Long categoryId) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(getCategoryTable().getPKColumn(), categoryId));

        return getCategories(Optional.of(whereClause)).stream().findFirst().get();
    }

    /**
     * <p>
     * Fetches the {@link SubCategory} by using the sub-category id.
     * </p>
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public SubCategory getSubCategory(final Long subCategoryId) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(getSubCategoryTable().getPKColumn(), subCategoryId));

        return getSubCategories(Optional.of(whereClause)).stream().findFirst().get();
    }

    /**
     * <p>
     * Fetches the {@link Action} by using the action id.
     * </p>
     *
     * @param actionId      Represents the id of the action.
     * @param subCategoryId Represents the sub-category id.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Action getAction(final Long actionId, final Long subCategoryId) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(getActionTable().getPKColumn(), actionId))
                .and(Criteria.eq(getSubCategoryActionTable().getColumn(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID), subCategoryId));

        return getActions(Optional.of(whereClause)).stream().findFirst().get();
    }

    /**
     * <p>
     * Provides all the {@link Category} of the permission.
     * </p>
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    @Override
    public Collection<Category> getAllCategories() {
        return getCategories(Optional.empty());
    }

    /**
     * <p>
     * Provides all the {@link SubCategory} of the permission.
     * </p>
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public Collection<SubCategory> getAllSubCategories() {
        return getSubCategories(Optional.empty());
    }

    /**
     * <p>
     * Provides all the {@link Action} of the permission.
     * </p>
     *
     * @return Collection of {@link Action} holds the details of the action.
     */
    @Override
    public Collection<Action> getAllActions() {
        return getActions(Optional.empty());
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryActionId Represents the id of the sub-category action.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Action getSubCategoryAction(final Long subCategoryActionId) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(getSubCategoryActionTable().getPKColumn(), subCategoryActionId));

        return getActions(Optional.of(whereClause)).stream().findFirst().get();
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped subcategory actions.
     */
    @Override
    public Map<Long, Map<Long, Long>> getSubCategoryActionIds() {
        final Table subCategoryActionTable = getSubCategoryActionTable();
        final DataContainer dataContainer = readableDataStore.get(subCategoryActionTable, (WhereClause) null);
        final Stream<Row> subCategoryActionRows = dataContainer.getRows(subCategoryActionTable);

        return subCategoryActionRows.collect(Collectors.groupingBy(row -> row.get(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID),
                Collectors.toMap(row -> row.get(IAMPSUBCATEGORYACTION.ACTION_ID), Row::getPKValue)));
    }

    /**
     * <p>
     * Provides the {@link Category} of the permission that matches the given criteria.
     * </p>
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    private Collection<Category> getCategories(final Optional<WhereClause> whereClause) {
        final Table categoryTable = getCategoryTable();
        final DataContainer dataContainer = readableDataStore.get(categoryTable, whereClause.orElseGet(() -> null));

        return dataContainer.getRows(categoryTable).map(this::convertRowIntoCategory).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Provides the {@link SubCategory} of the permission that matches the given criteria.
     * </p>
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    private Collection<SubCategory> getSubCategories(final Optional<WhereClause> whereClause) {
        final Table categoryTable = getCategoryTable();
        final Table subCategoryTable = getSubCategoryTable();
        final SelectQuery selectQuery = SelectQuery.get(categoryTable);
        final Column categoryIdColumn = subCategoryTable.getColumn(IAMPSUBCATEGORY.CATEGORY_ID);

        selectQuery.addJoinClause(new JoinClause(categoryTable.getPKColumn(), categoryIdColumn));
        whereClause.ifPresent(selectQuery::setWhereClause);
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return dataContainer.getRows(categoryTable).flatMap(categoryRow -> {
            final Category category = convertRowIntoCategory(categoryRow);
            final Stream<Row> subCategoryRows = dataContainer.getRows(subCategoryTable, new WhereClause(Criteria.eq(categoryIdColumn, categoryRow.getPKValue())));

            return subCategoryRows.map(subCategoryRow -> {
                final SubCategory subCategory = convertRowIntoSubCategory(subCategoryRow);

                subCategory.setCategory(category);
                return subCategory;
            });
        }).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Provides all the {@link Action} of the permission that matches the given criteria.
     * </p>
     *
     * @return Collection of {@link Action} holds the details of the action.
     */
    private Collection<Action> getActions(final Optional<WhereClause> whereClause) {
        final Table actionTable = getActionTable();
        final Table categoryTable = getCategoryTable();
        final Table subCategoryTable = getSubCategoryTable();
        final Table subCategoryActionTable = getSubCategoryActionTable();
        final SelectQuery selectQuery = SelectQuery.get(actionTable);

        whereClause.ifPresent(selectQuery::setWhereClause);
        selectQuery.addJoinClause(List.of(
                new JoinClause(actionTable.getPKColumn(), subCategoryActionTable.getColumn(IAMPSUBCATEGORYACTION.ACTION_ID)),
                new JoinClause(subCategoryActionTable.getColumn(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID), subCategoryTable.getPKColumn()),
                new JoinClause(subCategoryTable.getColumn(IAMPSUBCATEGORY.CATEGORY_ID), categoryTable.getPKColumn()),
                new JoinClause(subCategoryActionTable.getPKColumn(), getActionDependenciesTable()
                        .getColumn(IAMPACTIONDEPENDENCY.SUBCATEGORY_ACTION_ID), JoinClause.JoinType.LEFT)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);
        final Map<Long, Category> categories = dataContainer.getRows(categoryTable).collect(Collectors.toMap(Row::getPKValue, this::convertRowIntoCategory));
        final Map<Long, SubCategory> subCategories = dataContainer.getRows(subCategoryTable).collect(Collectors.toMap(Row::getPKValue, subCategoryRow -> {
            final SubCategory subCategory = convertRowIntoSubCategory(subCategoryRow);

            subCategory.setCategory(categories.get(subCategoryRow.get(IAMPSUBCATEGORY.CATEGORY_ID)));
            return subCategory;
        }));

        return dataContainer.getRows(subCategoryActionTable).map(subCategoryActionRow -> {
            final Action action = convertRowIntoAction(subCategoryActionRow, dataContainer);

            action.setSubCategory(subCategories.get(subCategoryActionRow.get(IAMPSUBCATEGORYACTION.SUB_CATEGORY_ID)));
            return action;
        }).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link Category}.
     * </p>
     *
     * @param categoryRow Holds the details of the category.
     * @return {@link Category}.
     */
    public Category convertRowIntoCategory(final Row categoryRow) {
        final Category category = new Category((String) categoryRow.get(IAMPCATEGORY.NAME));

        category.setId(categoryRow.getPKValue());
        return category;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link SubCategory}.
     * </p>
     *
     * @param subCategoryRow Holds the details of the sub-category.
     * @return {@link SubCategory}.
     */
    public SubCategory convertRowIntoSubCategory(final Row subCategoryRow) {
        final SubCategory subCategory = new SubCategory((String) subCategoryRow.get(IAMPSUBCATEGORY.NAME));
        final String requisites = subCategoryRow.get(IAMPSUBCATEGORY.REQUISITES);

        subCategory.setId(subCategoryRow.getPKValue());
        subCategory.setDisplayName(subCategoryRow.get(IAMPSUBCATEGORY.DISPLAY_NAME));

        if (Objects.nonNull(requisites)) {
            subCategory.setRequisites(requisites);
        }
        return subCategory;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link Action}.
     * </p>
     *
     * @param subCategoryActionRow Holds the details of the sub-category action.
     * @param dataContainer        Holds the information of the permission of role.
     * @return {@link Action}.
     */
    public Action convertRowIntoAction(final Row subCategoryActionRow, final DataContainer dataContainer) {
        final Row actionRow = dataContainer.getRow(getActionTable(), (Long) subCategoryActionRow.get(IAMPSUBCATEGORYACTION.ACTION_ID));
        final Table actionDependenciesTable = getActionDependenciesTable();
        final Criteria criteria = Criteria.eq(actionDependenciesTable.getColumn(IAMPACTIONDEPENDENCY.SUBCATEGORY_ACTION_ID), subCategoryActionRow.getPKValue());
        final Stream<Row> actionDependencyRows = dataContainer.getRows(actionDependenciesTable, new WhereClause(criteria));
        final String requisites = actionRow.get(IAMPACTION.REQUISITES);
        final Action action = new Action((String) actionRow.get(IAMPACTION.NAME));

        action.setId(actionRow.getPKValue());
        action.setDisplayName(actionRow.get(IAMPACTION.DISPLAY_NAME));
        actionDependencyRows.forEach(actionDependencyRow -> action.addDependency(convertRowIntoDependency(actionDependencyRow)));

        if (Objects.nonNull(requisites)) {
            action.setRequisites(requisites);
        }
        return action;
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link Action.Dependency}.
     * </p>
     *
     * @param actionDependencyRow Holds the details of the action dependencies.
     * @return {@link Action.Dependency}.
     */
    private Action.Dependency convertRowIntoDependency(final Row actionDependencyRow) {
        final Optional<String> optionalEssentials = Optional.ofNullable(actionDependencyRow.get(IAMPACTIONDEPENDENCY.REQUISITES));
        final Long dependentActionId = actionDependencyRow.get(IAMPACTIONDEPENDENCY.DEPENDENT_SUBCATEGORY_ACTION_ID);
        final Action subCategoryAction = getSubCategoryAction(dependentActionId);

        if (optionalEssentials.isPresent()) {
            final String requisites = optionalEssentials.get();

            return new Action.Dependency(subCategoryAction, requisites);
        }
        return new Action.Dependency(subCategoryAction);
    }
}