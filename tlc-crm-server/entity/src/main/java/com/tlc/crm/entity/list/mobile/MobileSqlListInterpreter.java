package com.tlc.crm.entity.list.mobile;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles SQL-based data processing for mobile app data.
 * Acts as bridge for SQL data manipulation with mobile-specific data handling, ensuring integration and efficiency for mobile list operations.
 *
 * <p>
 * Customizes selection criteria to focus on default fields enabled for mobile viewing.
 * </p>
 *
 * @author Kavinila
 * @version 1.0
 * @see EntitySQLListInterpreter
 */
public class MobileSqlListInterpreter extends EntitySQLListInterpreter {

    public MobileSqlListInterpreter(final long userId, final long orgId, final Provider provider, final JoinLoader joinLoader,
            final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultSelect() {
        listStructure.getFields().stream().filter(fieldConfig -> {
            final FieldConfig.Config config = fieldConfig.getConfig();

            return Objects.isNull(config) ? false : config.isEnabledForMobile();
        }).forEach(fieldConfig -> selectQuery.addSelectClause(getSelectColumn(fieldConfig)));
    }

    /**
     * <p>
     * Checks whether the field is selected before
     * </p>
     *
     * @param configuredField Represents the {@link ConfiguredField}
     * @return True if the field is selected before, false otherwise
     */
    public boolean isSelectedBefore(final ConfiguredField configuredField) {
        final String fieldName = configuredField.fieldName();
        final Collection<String> fieldConfigs = listStructure.getFields().stream().filter(fieldConfig -> {
            final FieldConfig.Config config = fieldConfig.getConfig();

            return Objects.isNull(config) ? false : config.isEnabledForMobile();
        }).map(FieldConfig::getName).collect(Collectors.toUnmodifiableSet());

        return fieldConfigs.contains(fieldName);
    }
}
