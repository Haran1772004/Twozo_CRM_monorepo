package com.tlc.crm.core.notification.email.template.api.builder;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.sql.resource.OMEMTAG;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATE;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERTEAM;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERTERRITORY;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERUSER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATETAG;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Builds the query for Email Templates
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public class EmailTemplateQueryBuilder {

    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;

    private EmailTemplateQueryBuilder() {
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    private static class Instance {
        private static final EmailTemplateQueryBuilder INSTANCE = new EmailTemplateQueryBuilder();
    }

    /**
     * <p>
     * Gets the {@link EmailTemplateQueryBuilder} Instance
     * </p>
     *
     * @return {@link EmailTemplateQueryBuilder}
     */
    public static EmailTemplateQueryBuilder getInstance() {
        return EmailTemplateQueryBuilder.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OMEMTEMPLATEFOLDERUSER} table
     * </p>
     *
     * @return the template folder user table
     */
    private Table getTemplateFolderUser() {
        return Table.get(OMEMTEMPLATEFOLDERUSER.TABLE);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} based on the {@link TemplateFolder.FilterAction} and search text
     * </p>
     *
     * @param userId       Represents the user ID
     * @param folderTable  Represents the template folder table
     * @param filterAction Represents the {@link TemplateFolder.FilterAction}
     * @param searchText   Represents the search text
     * @return {@link WhereClause} with necessary criteria based on the filter action and search text
     */
    public WhereClause buildFilterActionCriteria(final long userId, final Table folderTable, final TemplateFolder.FilterAction filterAction, final String searchText) {
        final WhereClause whereClause = switch (filterAction) {
            case CREATED_BY_ME -> new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId));
            case SHARED_WITH_ME -> new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE),
                    (int) TemplateFolder.Visibility.EVERYONE.getId())).and(Criteria.notEq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId))
                    .or(getSharedFolderCriteria(userId, folderTable));
            case ALL -> getAllFolderCriteria(userId, folderTable);
        };

        return Objects.isNull(searchText) ? whereClause : appendSearchCriteria(searchText, whereClause, folderTable);
    }

    /**
     * <p>
     * Gets the {@link WhereClause} to get all template folders
     * </p>
     *
     * @param userId      Represents the user ID
     * @param folderTable Represents the template folder table
     * @return {@link WhereClause}
     */
    public WhereClause getAllFolderCriteria(final long userId, final Table folderTable) {
        return new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId))
                .or(new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), (int) TemplateFolder.Visibility.EVERYONE.getId())))
                .or(getSharedFolderCriteria(userId, folderTable));
    }

    /**
     * <p>
     * Gets the {@link WhereClause} for shared folders
     * </p>
     *
     * @param userId Represents the user ID
     * @return {@link WhereClause}
     */
    private WhereClause getSharedFolderCriteria(final long userId, final Table folderTable) {
        final Column folderPkColumn = folderTable.getPKColumn();

        return new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), (int) TemplateFolder.Visibility.DESIGNATED.getId()))
                .and(new WhereClause(Criteria.in(folderPkColumn, getUserFolderQuery(userId, folderTable, folderPkColumn)))
                        .or(Criteria.in(folderPkColumn, getTeamFolderQuery(userId, folderTable, folderPkColumn)))
                        .or(Criteria.in(folderPkColumn, getTerritoryFolderQuery(userId, folderTable, folderPkColumn))));
    }

    /**
     * <p>
     * Appends search criteria to the given {@link WhereClause}, matching folders whose name, or any
     * template name inside the folder, or any tag name on those templates starts with the search text.
     * All three checks are performed at DB query level so pagination is applied on already-filtered data.
     * </p>
     *
     * @param searchText  Represents the search text (prefix match, case-insensitive)
     * @param whereClause Represents the existing {@link WhereClause} from the filter action
     * @param folderTable Represents the template folder table
     * @return {@link WhereClause} with search criteria appended
     */
    private WhereClause appendSearchCriteria(final String searchText, final WhereClause whereClause, final Table folderTable) {
        final Table templateTable = Table.get(OMEMTEMPLATE.TABLE);
        final Column folderPkColumn = folderTable.getPKColumn();
        final SelectQuery templateSubQuery = SelectQuery.get(templateTable);

        templateSubQuery.addSelectClause(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID));
        templateSubQuery.setWhereClause(new WhereClause(Criteria.contains(templateTable.getColumn(OMEMTEMPLATE.NAME), searchText)));

        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Table tagTable = Table.get(OMEMTAG.TABLE);
        final SelectQuery tagSubQuery = SelectQuery.get(templateTable);

        tagSubQuery.addSelectClause(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID));
        tagSubQuery.addJoinClause(new JoinClause(templateTable.getPKColumn(), templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID)));
        tagSubQuery.addJoinClause(new JoinClause(templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID), tagTable.getPKColumn()));
        tagSubQuery.setWhereClause(new WhereClause(Criteria.contains(tagTable.getColumn(OMEMTAG.NAME), searchText)));

        final WhereClause searchWhereClause = new WhereClause(Criteria.contains(folderTable.getColumn(OMEMTEMPLATEFOLDER.NAME), searchText))
                .or(Criteria.in(folderPkColumn, templateSubQuery))
                .or(Criteria.in(folderPkColumn, tagSubQuery));

        return whereClause.and(searchWhereClause);
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} to get template IDs related to user folder
     * </p>
     *
     * @param userId         Represents the user ID
     * @param folderTable    Represents the template folder table
     * @param folderPkColumn Represents the PK column of the folder table
     * @return {@link SelectQuery}
     */
    private SelectQuery getUserFolderQuery(final long userId, final Table folderTable, final Column folderPkColumn) {
        final Table userFolderTable = getTemplateFolderUser();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.addSelectClause(folderPkColumn);
        selectQuery.addJoinClause(new JoinClause(folderPkColumn, userFolderTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userFolderTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID), userId)));

        return selectQuery;
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} to get template IDs related to team folder
     * </p>
     *
     * @param userId         Represents the user ID
     * @param folderTable    Represents the template folder table
     * @param folderPkColumn Represents the PK column of the folder table
     * @return {@link SelectQuery}
     */
    private SelectQuery getTeamFolderQuery(final long userId, final Table folderTable, final Column folderPkColumn) {
        final Table teamFolderTable = Table.get(OMEMTEMPLATEFOLDERTEAM.TABLE);
        final Table teamUserTable = teamManager.getUserTeamTable();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.addSelectClause(folderPkColumn);
        selectQuery.addJoinClause(List.of(
                new JoinClause(folderPkColumn, teamFolderTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID)),
                new JoinClause(teamFolderTable.getColumn(OMEMTEMPLATEFOLDERTEAM.TEAM_ID), teamUserTable.getColumn(MCRMUSERTEAM.TEAM_ID)))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamUserTable.getColumn(MCRMUSERTEAM.USER_ID), userId)));

        return selectQuery;
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} to get template IDs related to territory folder
     * </p>
     *
     * @param userId         Represents the user ID
     * @param folderTable    Represents the template folder table
     * @param folderPkColumn Represents the PK column of the folder table
     * @return {@link SelectQuery}
     */
    private SelectQuery getTerritoryFolderQuery(final long userId, final Table folderTable, final Column folderPkColumn) {
        final Table territoryFolderTable = Table.get(OMEMTEMPLATEFOLDERTERRITORY.TABLE);
        final Table userTerritoryTable = territoryManager.getUserTerritoryTable();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.addSelectClause(folderPkColumn);
        selectQuery.addJoinClause(List.of(
                new JoinClause(folderPkColumn, territoryFolderTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID)),
                new JoinClause(territoryFolderTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID), userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId)));

        return selectQuery;
    }
}
