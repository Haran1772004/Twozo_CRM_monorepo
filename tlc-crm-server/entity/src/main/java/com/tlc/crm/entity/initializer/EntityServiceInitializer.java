package com.tlc.crm.entity.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.list.DefaultStructureHandler;
import com.tlc.crm.common.models.I18nPlaceholderResolverRegistry;
import com.tlc.crm.common.models.I18nPlaceholderResolverType;
import com.tlc.crm.entity.listeners.MenuUserListener;
import com.tlc.crm.entity.listeners.permission.FieldPermissionRowListener;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.resolver.source.SourceNameResolver;
import com.tlc.crm.entity.role.permission.provider.HiddenFieldIdsProvider;
import com.tlc.crm.entity.role.permission.provider.HiddenFieldNamesProvider;
import com.tlc.crm.entity.role.permission.resolver.FieldConfigColumnDataResolver;
import com.tlc.crm.entity.store.AsyncBulkActionTopicStore;
import com.tlc.crm.entity.role.permission.provider.DataProviderSupplier;
import com.tlc.crm.security.internal.permission.registry.ColumnValueResolverRegistry;
import com.tlc.crm.security.permission.SecureDefaultValueProviderRegistry;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OIAMPFIELDPERMISSIONS;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.crm.entity.ActivityStatusController;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.cache.internal.CacheLoader;
import com.tlc.crm.entity.currency.listener.BaseCurrencyListener;
import com.tlc.crm.entity.currency.listener.CurrencyUnitListener;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.listeners.CrmFieldConfigListener;
import com.tlc.crm.entity.listeners.CrmFieldListener;
import com.tlc.crm.entity.listeners.OptionListener;
import com.tlc.crm.entity.listeners.SourceSettingsListener;
import com.tlc.crm.entity.listeners.cleanup.OrphanReferenceCleanupListener;
import com.tlc.crm.entity.listeners.filter.UserRowListener;
import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.entity.models.EntityConverterHolder;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.type.converter.AddressConverter;
import com.tlc.crm.entity.models.type.converter.DateRangeConverter;
import com.tlc.crm.entity.models.type.converter.DateTimeRangeConverter;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.entity.models.type.converter.TimeRangeConverter;
import com.tlc.crm.entity.sql.resource.OMETBASECURRENCY;
import com.tlc.crm.entity.sql.resource.OMETCURRENCY;
import com.tlc.crm.entity.sql.resource.OMETFIELDCONFIG;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.entity.sql.resource.OMETOPTION;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.sql.resource.OMETSOURCESETTINGS;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

import com.tlc.validator.TlcModel;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EntityServiceInitializer implements ServiceInitializer {

    private final CacheLoader cacheLoader;
    private final ActivityStatusController activityStatusController;
    private final WritableDataStore writableDataStore;
    private final EntityManagerHolder entityManagerHolder;
    private final EntityConverterHolder entityConverterHolder;
    private final PhoneManager phoneManager;
    private final EmailManager emailManager;
    private final OptionConverter optionConverter;
    private final PhoneConverter phoneConverter;
    private final EmailConverter emailConverter;
    private final MonetaryManager monetaryManager;
    private final MonetaryConverter monetaryConverter;
    private final TimeRangeConverter timeRangeConverter;
    private final DateTimeRangeConverter dateTimeRangeConverter;
    private final AddressConverter addressConverter;
    private final DateRangeConverter dateRangeConverter;
    private final AddressManager addressManager;
    private final DateTimeRangeManager dateTimeRangeManager;
    private final TimeRangeManager timeRangeManager;
    private final DateRangeManager dateRangeManager;
    private final DefaultStructureHandler defaultStructureHandler;
    private final CrmFieldListener crmFieldListener;
    private final CrmFieldConfigListener crmFieldConfigListener;
    private final SourceSettingsListener sourceSettingsListener;
    private final OptionListener optionListener;
    private final CurrencyUnitListener currencyUnitListener;
    private final BaseCurrencyListener baseCurrencyListener;
    private final TagManager tagManager;
    private final TagConverter tagConverter;
    private final UserRowListener userRowListener;
    private final SecureDefaultValueProviderRegistry valueProviderRegistry;
    private final DataProviderSupplier dataProviderSupplier;
    private final MenuUserListener menuUserListener;
    private final AsyncBulkActionTopicStore asyncBulkActionTopicStore;
    private final I18nPlaceholderResolverRegistry i18NPlaceholderResolverRegistry;
    private final FieldPermissionRowListener fieldPermissionRowListener;
    private final HiddenFieldIdsProvider hiddenFieldIdsProvider;
    private final HiddenFieldNamesProvider hiddenFieldNamesProvider;
    private final ColumnValueResolverRegistry columnValueResolverRegistry;
    private final FieldConfigColumnDataResolver fieldConfigColumnDataResolver;
    private final MarketingListManager marketingListManager;
    private final EventListenerRegistry eventListenerRegistry;
    private final OrphanReferenceCleanupListener orphanReferenceCleanupListener;

    private EntityServiceInitializer() {
        optionConverter = OptionConverter.getInstance();
        phoneManager = PhoneManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneConverter = PhoneConverter.getInstance();
        emailConverter = EmailConverter.getInstance();
        timeRangeConverter = TimeRangeConverter.getInstance();
        dateTimeRangeConverter = DateTimeRangeConverter.getInstance();
        addressConverter = AddressConverter.getInstance();
        dateRangeConverter = DateRangeConverter.getInstance();
        addressManager = AddressManager.getInstance();
        dateTimeRangeManager = DateTimeRangeManager.getInstance();
        timeRangeManager = TimeRangeManager.getInstance();
        writableDataStore = DataStoreProvider.getWritableDataStore();
        dateRangeManager = DateRangeManager.getInstance();
        entityManagerHolder = EntityManagerHolder.getInstance();
        entityConverterHolder = EntityConverterHolder.getInstance();
        defaultStructureHandler = DefaultStructureHandler.get();
        crmFieldListener = CrmFieldListener.getInstance();
        crmFieldConfigListener = CrmFieldConfigListener.getInstance();
        sourceSettingsListener = SourceSettingsListener.getInstance();
        optionListener = OptionListener.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        currencyUnitListener = CurrencyUnitListener.getInstance();
        baseCurrencyListener = BaseCurrencyListener.getInstance();
        activityStatusController = ActivityStatusController.getInstance();
        cacheLoader = CacheLoader.getInstance();
        tagManager = TagManager.getInstance();
        tagConverter = TagConverter.getInstance();
        userRowListener = UserRowListener.getInstance();
        valueProviderRegistry = SecureDefaultValueProviderRegistry.get();
        dataProviderSupplier = DataProviderSupplier.getInstance();
        menuUserListener = MenuUserListener.getInstance();
        asyncBulkActionTopicStore = AsyncBulkActionTopicStore.getInstance();
        i18NPlaceholderResolverRegistry = I18nPlaceholderResolverRegistry.getInstance();
        fieldPermissionRowListener = FieldPermissionRowListener.getInstance();
        hiddenFieldIdsProvider = HiddenFieldIdsProvider.getInstance();
        hiddenFieldNamesProvider = HiddenFieldNamesProvider.getInstance();
        columnValueResolverRegistry = ColumnValueResolverRegistry.getInstance();
        fieldConfigColumnDataResolver = FieldConfigColumnDataResolver.getInstance();
        marketingListManager = MarketingListManager.getInstance();
        eventListenerRegistry = EventListenerRegistry.getInstance();
        orphanReferenceCleanupListener = OrphanReferenceCleanupListener.getInstance();
    }

    private static final class Instance {
        private static final EntityServiceInitializer ENTITY_SERVICE_INITIALIZER = new EntityServiceInitializer();
    }

    public static EntityServiceInitializer getInstance() {
        return Instance.ENTITY_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        columnValueResolverRegistry.addResolver(fieldConfigColumnDataResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        final Table userTable = Table.get(OMCRMUSER.TABLE);

        writableDataStore.registerRowListener(userTable, userRowListener);
        writableDataStore.registerRowListener(Table.get(OMETFIELDS.TABLE), crmFieldListener);
        writableDataStore.registerRowListener(Table.get(OMETFIELDCONFIG.TABLE), crmFieldConfigListener);
        writableDataStore.registerRowListener(Table.get(OMETSOURCESETTINGS.TABLE), sourceSettingsListener);
        writableDataStore.registerRowListener(Table.get(OMETOPTION.TABLE), optionListener);
        writableDataStore.registerRowListener(Table.get(OMETCURRENCY.TABLE), currencyUnitListener);
        writableDataStore.registerUpdatedEntryListener(Table.get(OMETBASECURRENCY.TABLE), baseCurrencyListener);
        writableDataStore.registerUpdatedEntryListener(Table.get(OIAMPFIELDPERMISSIONS.TABLE), fieldPermissionRowListener);
        valueProviderRegistry.register(dataProviderSupplier);
        valueProviderRegistry.register(hiddenFieldIdsProvider);
        valueProviderRegistry.register(hiddenFieldNamesProvider);
        activityStatusController.start(null);
        writableDataStore.registerNewEntryListener(userTable, menuUserListener);
        loadConverters();
        loadManagers();
        cacheLoader.load();
        asyncBulkActionTopicStore.add(ConfHolder.getInstance().getProperties("async-bulk-action"));
        i18NPlaceholderResolverRegistry.add(I18nPlaceholderResolverType.SOURCE, SourceNameResolver.getInstance());
        eventListenerRegistry.registerPostEventListener(orphanReferenceCleanupListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        cacheLoader.clear();
        defaultStructureHandler.clearCache();
        activityStatusController.stop();
        asyncBulkActionTopicStore.clear();
    }

    /**
     * Loads the config managers of respective {@link StoreType} into {@link com.tlc.crm.entity.models.EntityConverterHolder}
     */
    private void loadManagers() {
        final Map<StoreType, ConfigManager<? extends TlcModel>> managers = new EnumMap<>(StoreType.class);

        managers.put(StoreType.DATE_RANGE, dateRangeManager);
        managers.put(StoreType.TIME_RANGE, timeRangeManager);
        managers.put(StoreType.DATE_TIME_RANGE, dateTimeRangeManager);
        managers.put(StoreType.MONETARY, monetaryManager);
        managers.put(StoreType.DEAL_VALUE, monetaryManager);
        managers.put(StoreType.PHONE, phoneManager);
        managers.put(StoreType.EMAIL, emailManager);
        managers.put(StoreType.PHONES, phoneManager);
        managers.put(StoreType.EMAILS, emailManager);
        managers.put(StoreType.ADDRESS, addressManager);
        managers.put(StoreType.TAGS, tagManager);
        managers.put(StoreType.MARKETING_LIST, marketingListManager);

        entityManagerHolder.loadManagersByStoreType(managers);
    }

    /**
     * Loads the converters of respective {@link StoreType} into {@link com.tlc.crm.entity.models.EntityConverterHolder}
     */
    private void loadConverters() {
        final Map<StoreType, EntityConverter<? extends TlcModel>> converters = new HashMap<>(Map.of(
                StoreType.DATE_RANGE, dateRangeConverter,
                StoreType.DROPDOWN, optionConverter,
                StoreType.TIME_RANGE, timeRangeConverter,
                StoreType.DATE_TIME_RANGE, dateTimeRangeConverter,
                StoreType.MONETARY, monetaryConverter,
                StoreType.PHONE, phoneConverter,
                StoreType.EMAIL, emailConverter,
                StoreType.PHONES, phoneConverter,
                StoreType.ADDRESS, addressConverter,
                StoreType.MULTI_SELECT, optionConverter));

        converters.put(StoreType.EMAILS, emailConverter);
        converters.put(StoreType.DEAL_VALUE, monetaryConverter);
        converters.put(StoreType.TAGS, tagConverter);
        entityConverterHolder.loadConvertersByStoreType(converters);
    }
}
