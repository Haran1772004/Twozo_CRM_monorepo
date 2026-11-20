package com.tlc.crm.core.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;

import com.tlc.crm.core.action.analytics.internal.DashboardTrashController;
import com.tlc.crm.core.action.analytics.reports.data.export.AnalyticsExportConsumerController;
import com.tlc.crm.core.api.analytics.initializer.AnalyticsSinkConnectorInitializer;
import com.tlc.crm.core.api.insights.listener.EntityInsightsListenerRegistrar;
import com.tlc.crm.core.api.insights.scheduler.EntityInsightsRecomputeScheduler;
import com.tlc.crm.core.features.internal.quota.controller.QuotaDealEventController;
import com.tlc.crm.core.features.internal.quota.listener.QuotaDealPostEventListener;
import com.tlc.crm.core.features.internal.quota.listener.QuotaDealPreEventListener;
import com.tlc.crm.core.features.internal.quota.controller.QuotaEventController;
import com.tlc.crm.core.features.internal.quota.listener.QuotaPostEventListener;
import com.tlc.crm.core.internal.analytics.initializer.CRMAnalyticsCacheInitializer;
import com.tlc.crm.core.iam.role.permission.handler.impl.SourceModelPermissionHandler;
import com.tlc.crm.core.iam.role.permission.resolver.FieldValueColumnDataResolver;
import com.tlc.crm.core.iam.role.permission.validator.UserCompanyRelationshipValidator;
import com.tlc.crm.core.iam.role.permission.validator.UserContactRelationshipValidator;
import com.tlc.crm.core.iam.role.permission.validator.UserDealRelationshipValidator;
import com.tlc.crm.core.internal.async.bulk.service.BulkActionService;
import com.tlc.crm.core.internal.event.CoreUserUpdatePostEventListener;
import com.tlc.crm.core.internal.event.activity.ActivityPostEventListener;
import com.tlc.crm.core.internal.event.email.EmailAlertEventService;
import com.tlc.crm.core.internal.event.index.IndexEventListener;
import com.tlc.crm.core.internal.event.onboard.service.OrgOnboardingService;
import com.tlc.crm.core.internal.index.consumer.service.IndexActionService;
import com.tlc.crm.core.internal.listener.FollowersRolePermissionListener;
import com.tlc.crm.core.internal.listener.RoleRowListener;
import com.tlc.crm.core.internal.marketinglist.MarketingListTransferService;
import com.tlc.crm.core.internal.store.RolePermissionCategoryListenerStore;
import com.tlc.crm.core.list.dynamic.CompanyDynamicFieldsFetcher;
import com.tlc.crm.core.list.dynamic.ContactDynamicFieldsFetcher;
import com.tlc.crm.core.list.dynamic.DealDynamicFieldsFetcher;
import com.tlc.crm.core.list.dynamic.ProductDynamicFieldsFetcher;
import com.tlc.crm.core.attachment.async.UploadListenerController;
import com.tlc.crm.core.list.ContactListPreviewRetriever;
import com.tlc.crm.core.list.DealListPreviewRetriever;
import com.tlc.crm.core.list.InvoiceListPreviewRetriever;
import com.tlc.crm.core.list.field.selector.ActivityListQueryComposer;
import com.tlc.crm.core.list.field.selector.CompanyListQueryComposer;
import com.tlc.crm.core.list.field.selector.ProductListQueryComposer;
import com.tlc.crm.core.list.field.selector.DealListQueryComposer;
import com.tlc.crm.core.notification.email.alert.EmailAlertPostEventListener;
import com.tlc.crm.core.notification.email.alert.container.EmailAlertFieldValueExtractorRegistry;
import com.tlc.crm.core.notification.email.alert.extractor.DateTimeFieldValueExtractor;
import com.tlc.crm.core.notification.email.alert.extractor.DefaultFieldValueExtractor;
import com.tlc.crm.core.notification.email.alert.extractor.TlcModelFieldValueExtractor;
import com.tlc.crm.core.segment.listener.SegmentPostEventListener;
import com.tlc.crm.core.segment.scheduler.SegmentRecomputeScheduler;
import com.tlc.crm.core.sql.resource.MPOPRICETAG;
import com.tlc.crm.core.list.summary.ActivityCountSummaryFetcher;
import com.tlc.crm.core.list.summary.CompanyCountSummaryFetcher;
import com.tlc.crm.core.list.summary.ContactCountSummaryFetcher;
import com.tlc.crm.core.list.summary.DealCountSummaryFetcher;
import com.tlc.crm.core.list.summary.ProductCountSummaryFetcher;
import com.tlc.crm.entity.fields.OrgOnboardFields;
import com.tlc.crm.entity.list.dynamic.container.ListDynamicFieldsFetcherRegistry;
import com.tlc.crm.entity.list.registry.ListPreviewRetrieverRegistry;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.field.selector.ListQueryComposerContainer;
import com.tlc.crm.core.list.field.selector.ContactListQueryComposer;
import com.tlc.crm.core.internal.event.contact.lifecyclestage.LifeCycleDeleteEventListener;
import com.tlc.crm.core.internal.event.contact.lifecyclestage.DealUpdateEventListener;
import com.tlc.crm.entity.list.summary.container.ListSummaryFetcherRegistry;
import com.tlc.crm.security.internal.permission.registry.ColumnValueResolverRegistry;
import com.tlc.crm.security.model.PermissionHandlerRegistry;
import com.tlc.crm.security.model.PermissionHandlerType;
import com.tlc.crm.security.permission.ColumnDataPermissionValidatorRegistry;
import com.tlc.crm.security.permission.handler.ModelPermissionHandler;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.crm.core.action.contact.scheduler.ContactPullController;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.converter.SourceNoteConverter;
import com.tlc.crm.core.features.internal.dataimport.async.consumer.ImportConsumerController;
import com.tlc.crm.core.features.internal.dataimport.async.registry.ReadTaskRegistry;
import com.tlc.crm.core.features.internal.dataimport.async.registry.impl.ReadTaskRegistryImpl;
import com.tlc.crm.core.features.internal.dataimport.async.service.ImportServiceHolder;
import com.tlc.crm.core.internal.event.contact.ContactCreateEventListener;
import com.tlc.crm.core.internal.event.contact.ContactDeleteEventListener;
import com.tlc.crm.core.internal.event.contact.ContactPushEventListener;
import com.tlc.crm.core.segment.consumer.SegmentRecomputeConsumerService;
import com.tlc.crm.core.internal.event.contact.ContactSyncDisablingEventListener;
import com.tlc.crm.core.internal.event.contact.ContactUpdateEventListener;
import com.tlc.crm.core.internal.event.contact.publisher.ContactDataPublisher;
import com.tlc.crm.core.internal.event.contact.publisher.ContactDataRemover;
import com.tlc.crm.core.internal.features.listener.SourceNoteListener;
import com.tlc.crm.core.internal.webform.WebFormSubmitController;
import com.tlc.crm.core.notification.email.internal.controller.EmailTrashController;
import com.tlc.crm.core.notification.email.internal.controller.ScheduledEmailServiceController;
import com.tlc.crm.core.notification.email.settings.listener.BlockedEmailAddressListener;
import com.tlc.crm.core.notification.email.settings.listener.EmailSettingsListener;
import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;
import com.tlc.crm.email.notification.sql.resource.OMEMBLOCKEDADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMUSEREMAILSETTINGS;
import com.tlc.crm.entity.models.EntityConverterHolder;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.webform.cache.TemplateCache;
import com.tlc.crm.webform.cache.internal.TemplateCacheImpl;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

import java.util.Map;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CoreServiceInitializer implements ServiceInitializer {

    private final ImportConsumerController importConsumerController;
    private final ImportServiceHolder importServiceHolder;
    private final ContactCreateEventListener contactCreateEventListener;
    private final ContactUpdateEventListener contactUpdateEventListener;
    private final ContactDeleteEventListener contactDeleteEventListener;
    private final WebFormSubmitController webFormSubmitController;
    private final IndexActionService indexActionService;
    private final IndexEventListener indexEventListener;
    private final CompanyManager companyManager;
    private final ContactManager contactManager;
    private final CompanyConverter companyConverter;
    private final ContactConverter contactConverter;
    private final EventListenerRegistry eventListenerRegistry;
    private final ContactDataPublisher contactDataPublisher;
    private final WritableDataStore writableDataStore;
    private final SourceNoteListener sourceNoteListener;
    private final EntityManagerHolder entityManagerHolder;
    private final EntityConverterHolder entityConverterHolder;
    private final ContactPullController contactPullController;
    private final ContactPushEventListener contactPushEventListener;
    private final ReadTaskRegistry readTaskRegistry;
    private final TemplateCache templateCache;
    private final DealManager dealManager;
    private final DealConverter dealConverter;
    private final ProductConverter productConverter;
    private final ProductManager productManager;
    private final SourceNoteManager sourceNoteManager;
    private final SourceNoteConverter sourceNoteConverter;
    private final ContactSyncDisablingEventListener contactSyncDisablingEventListener;
    private final ContactDataRemover contactDataRemover;
    private final ScheduledEmailServiceController scheduledEmailServiceController;
    private final EmailTrashController emailTrashController;
    private final BlockedEmailAddressListener blockedEmailAddressListener;
    private final EmailSettingsListener emailSettingsListener;
    private final EmailAlertPostEventListener emailAlertPostEventListener;
    private final ListQueryComposerContainer listQueryComposerContainer;
    private final ActivityListQueryComposer activityListQueryComposer;
    private final DealListQueryComposer dealListQueryComposer;
    private final ContactListQueryComposer contactListQueryComposer;
    private final ListSummaryFetcherRegistry listSummaryFetcherRegistry;
    private final ContactCountSummaryFetcher contactCountSummaryFetcher;
    private final CompanyCountSummaryFetcher companyCountSummaryFetcher;
    private final DealCountSummaryFetcher dealCountSummaryFetcher;
    private final ProductCountSummaryFetcher productCountSummaryFetcher;
    private final ActivityCountSummaryFetcher activityCountSummaryFetcher;
    private final DealUpdateEventListener dealUpdateEventListener;
    private final LifeCycleDeleteEventListener lifeCycleDeleteEventListener;
    private final UploadListenerController uploadListenerController;
    private final ListDynamicFieldsFetcherRegistry listDynamicFieldsFetcherRegistry;
    private final ContactDynamicFieldsFetcher contactDynamicFieldsFetcher;
    private final CompanyDynamicFieldsFetcher companyDynamicFieldsFetcher;
    private final DealDynamicFieldsFetcher dealDynamicFieldsFetcher;
    private final ProductDynamicFieldsFetcher productDynamicFieldsFetcher;
    private final EmailAlertFieldValueExtractorRegistry emailAlertFieldValueExtractorRegistry;
    private final DateTimeFieldValueExtractor dateTimeFieldValueExtractor;
    private final DefaultFieldValueExtractor defaultFieldValueExtractor;
    private final TlcModelFieldValueExtractor tlcModelFieldValueExtractor;
    private final ListPreviewRetrieverRegistry listPreviewRetrieverRegistry;
    private final ContactListPreviewRetriever contactListPreviewRetriever;
    private final DealListPreviewRetriever dealListPreviewRetriever;
    private final InvoiceListPreviewRetriever invoiceListPreviewRetriever;
    private final ColumnDataPermissionValidatorRegistry columnDataPermissionValidatorRegistry;
    private final UserContactRelationshipValidator userContactRelationshipValidator;
    private final UserCompanyRelationshipValidator userCompanyRelationshipValidator;
    private final UserDealRelationshipValidator userDealRelationshipValidator;
    private final DashboardTrashController dashboardTrashController;
    private final ColumnValueResolverRegistry columnValueResolverRegistry;
    private final FieldValueColumnDataResolver fieldValueColumnDataResolver;
    private final BulkActionService bulkActionService;
    private final ModelPermissionHandler modelPermissionHandler;
    private final PermissionHandlerRegistry permissionHandlerRegistry;
    private final AnalyticsExportConsumerController analyticsExportConsumerController;
    private final RoleRowListener roleRowListener;
    private final RolePermissionCategoryListenerStore rolePermissionCategoryListenerStore;
    private final ActivityPostEventListener activityPostEventListener;
    private final QuotaDealPostEventListener quotaDealPostEventListener;
    private final QuotaDealPreEventListener quotaDealPreEventListener;
    private final QuotaPostEventListener quotaPostEventListener;
    private final QuotaDealEventController quotaDealEventController;
    private final QuotaEventController quotaEventController;
    private final OrgOnboardingService orgOnboardingService;
    private final CRMAnalyticsCacheInitializer crmAnalyticsCacheInitializer;
    private final AnalyticsSinkConnectorInitializer analyticsSinkConnectorInitializer;
    private final MarketingListTransferService marketingListTransferService;
    private final EntityInsightsRecomputeScheduler entityInsightsRecomputeScheduler;
    private final EmailAlertEventService emailAlertEventService;
    private final SegmentRecomputeScheduler segmentRecomputeScheduler;
    private final SegmentPostEventListener segmentPostEventListener;
    private final SegmentRecomputeConsumerService segmentRecomputeConsumerService;

    private CoreServiceInitializer() {
        importServiceHolder = new ImportServiceHolder();
        webFormSubmitController = new WebFormSubmitController();
        importConsumerController = new ImportConsumerController();
        contactCreateEventListener = new ContactCreateEventListener();
        contactUpdateEventListener = new ContactUpdateEventListener();
        contactDeleteEventListener = new ContactDeleteEventListener();
        indexActionService = new IndexActionService();
        indexEventListener = IndexEventListener.getInstance();
        this.emailAlertEventService = new EmailAlertEventService();
        companyManager = CompanyManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
        contactConverter = ContactConverter.getInstance();
        eventListenerRegistry = EventListenerRegistry.getInstance();
        writableDataStore = DataStoreProvider.getWritableDataStore();
        sourceNoteListener = SourceNoteListener.getInstance();
        entityManagerHolder = EntityManagerHolder.getInstance();
        entityConverterHolder = EntityConverterHolder.getInstance();
        contactPullController = ContactPullController.getInstance();
        contactPushEventListener = new ContactPushEventListener();
        readTaskRegistry = ReadTaskRegistry.get();
        templateCache = TemplateCacheImpl.getInstance();
        dealManager = DealManager.getInstance();
        dealConverter = DealConverter.getInstance();
        productManager = ProductManager.getInstance();
        productConverter = ProductConverter.getInstance();
        sourceNoteManager = SourceNoteManager.getInstance();
        sourceNoteConverter = SourceNoteConverter.getInstance();
        contactSyncDisablingEventListener = new ContactSyncDisablingEventListener();
        contactDataPublisher = ContactDataPublisher.getInstance();
        contactDataRemover = ContactDataRemover.getInstance();
        scheduledEmailServiceController = ScheduledEmailServiceController.getInstance();
        emailTrashController = EmailTrashController.getInstance();
        blockedEmailAddressListener = BlockedEmailAddressListener.getInstance();
        emailSettingsListener = EmailSettingsListener.getInstance();
        emailAlertPostEventListener = EmailAlertPostEventListener.getInstance();
        listQueryComposerContainer = ListQueryComposerContainer.getInstance();
        activityListQueryComposer = ActivityListQueryComposer.getInstance();
        dealListQueryComposer = DealListQueryComposer.getInstance();
        contactListQueryComposer = ContactListQueryComposer.getInstance();
        listSummaryFetcherRegistry = ListSummaryFetcherRegistry.getInstance();
        contactCountSummaryFetcher = ContactCountSummaryFetcher.getInstance();
        companyCountSummaryFetcher = CompanyCountSummaryFetcher.getInstance();
        dealCountSummaryFetcher = DealCountSummaryFetcher.getInstance();
        productCountSummaryFetcher = ProductCountSummaryFetcher.getInstance();
        activityCountSummaryFetcher = ActivityCountSummaryFetcher.getInstance();
        dealUpdateEventListener = DealUpdateEventListener.getInstance();
        lifeCycleDeleteEventListener = LifeCycleDeleteEventListener.getInstance();
        listDynamicFieldsFetcherRegistry = ListDynamicFieldsFetcherRegistry.getInstance();
        contactDynamicFieldsFetcher = ContactDynamicFieldsFetcher.getInstance();
        companyDynamicFieldsFetcher = CompanyDynamicFieldsFetcher.getInstance();
        dealDynamicFieldsFetcher = DealDynamicFieldsFetcher.getInstance();
        productDynamicFieldsFetcher = ProductDynamicFieldsFetcher.getInstance();
        emailAlertFieldValueExtractorRegistry = EmailAlertFieldValueExtractorRegistry.getInstance();
        dateTimeFieldValueExtractor = DateTimeFieldValueExtractor.getInstance();
        defaultFieldValueExtractor = DefaultFieldValueExtractor.getInstance();
        tlcModelFieldValueExtractor = TlcModelFieldValueExtractor.getInstance();
        this.uploadListenerController = new UploadListenerController();
        this.listPreviewRetrieverRegistry = ListPreviewRetrieverRegistry.getInstance();
        this.contactListPreviewRetriever = ContactListPreviewRetriever.getInstance();
        this.dealListPreviewRetriever = DealListPreviewRetriever.getInstance();
        this.invoiceListPreviewRetriever = InvoiceListPreviewRetriever.getInstance();
        columnDataPermissionValidatorRegistry = ColumnDataPermissionValidatorRegistry.getInstance();
        userContactRelationshipValidator = UserContactRelationshipValidator.getInstance();
        userCompanyRelationshipValidator = UserCompanyRelationshipValidator.getInstance();
        userDealRelationshipValidator = UserDealRelationshipValidator.getInstance();
        dashboardTrashController = DashboardTrashController.getInstance();
        columnValueResolverRegistry = ColumnValueResolverRegistry.getInstance();
        fieldValueColumnDataResolver = FieldValueColumnDataResolver.getInstance();
        bulkActionService = new BulkActionService();
        modelPermissionHandler = SourceModelPermissionHandler.getInstance();
        permissionHandlerRegistry = PermissionHandlerRegistry.getInstance();
        analyticsExportConsumerController = new AnalyticsExportConsumerController();
        roleRowListener = RoleRowListener.getInstance();
        rolePermissionCategoryListenerStore = RolePermissionCategoryListenerStore.getInstance();
        activityPostEventListener = ActivityPostEventListener.getInstance();
        quotaDealPostEventListener = QuotaDealPostEventListener.getInstance();
        quotaDealPreEventListener = QuotaDealPreEventListener.getInstance();
        quotaDealEventController = new QuotaDealEventController();
        quotaPostEventListener = QuotaPostEventListener.getInstance();
        quotaEventController = new QuotaEventController();
        orgOnboardingService = new OrgOnboardingService();
        crmAnalyticsCacheInitializer = CRMAnalyticsCacheInitializer.getInstance();
        analyticsSinkConnectorInitializer = AnalyticsSinkConnectorInitializer.getInstance();
        marketingListTransferService = MarketingListTransferService.getInstance();
        entityInsightsRecomputeScheduler = EntityInsightsRecomputeScheduler.getInstance();
        segmentRecomputeScheduler = SegmentRecomputeScheduler.getInstance();
        segmentPostEventListener = SegmentPostEventListener.getInstance();
        segmentRecomputeConsumerService = SegmentRecomputeConsumerService.getInstance();
    }

    private static final class Instance {
        private static final CoreServiceInitializer CORE_SERVICE_INITIALIZER = new CoreServiceInitializer();
    }

    public static CoreServiceInitializer getInstance() {
        return Instance.CORE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        importServiceHolder.start(configurations);

        columnValueResolverRegistry.addResolver(fieldValueColumnDataResolver);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        writableDataStore.registerNewEntryListener(Table.get(OMNTCOMPANYNOTE.TABLE), sourceNoteListener);
        writableDataStore.registerNewEntryListener(Table.get(OMNTCONTACTNOTE.TABLE), sourceNoteListener);
        writableDataStore.registerNewEntryListener(Table.get(OMNTDEALNOTE.TABLE), sourceNoteListener);
        writableDataStore.registerUpdatedEntryListener(Table.get(OMNTNOTE.TABLE), sourceNoteListener);

        entityManagerHolder.loadManagersByStoreType(Map.of(
                StoreType.COMPANY, companyManager,
                StoreType.COMPANY_ONBOARD, companyManager,
                StoreType.CONTACT, contactManager,
                StoreType.CONTACT_ONBOARD, contactManager,
                StoreType.CONTACTS_ONBOARD, contactManager,
                StoreType.DEAL, dealManager,
                StoreType.PRODUCT, productManager,
                StoreType.NOTE, sourceNoteManager));
        entityManagerHolder.loadManagersByStoreSubtype(Map.of(StoreSubType.PRODUCT, productManager));
        entityManagerHolder.loadTableByStoreType(Map.of(StoreType.PRICE_TAGS, Table.get(MPOPRICETAG.TABLE_NAME)));

        listQueryComposerContainer.put(Provider.ACTIVITY_LIST, activityListQueryComposer);
        listQueryComposerContainer.put(Provider.DEAL_LIST, dealListQueryComposer);
        listQueryComposerContainer.put(Provider.CONTACT_LIST, contactListQueryComposer);
        listQueryComposerContainer.put(Provider.COMPANY_LIST, CompanyListQueryComposer.getInstance());
        listQueryComposerContainer.put(Provider.PRODUCT_LIST, ProductListQueryComposer.getInstance());
        entityConverterHolder.loadConvertersByStoreType(Map.of(
                StoreType.COMPANY, companyConverter,
                StoreType.COMPANY_ONBOARD, companyConverter,
                StoreType.CONTACT, contactConverter,
                StoreType.CONTACT_ONBOARD, contactConverter,
                StoreType.CONTACTS_ONBOARD, contactConverter,
                StoreType.DEAL, dealConverter,
                StoreType.PRODUCT, productConverter,
                StoreType.NOTE, sourceNoteConverter));

        webFormSubmitController.start(configurations);
        importConsumerController.start(configurations);
        templateCache.load();

        /* CRM Analytics Caches & Listeners */
        crmAnalyticsCacheInitializer.postInitialize(configurations);
        analyticsSinkConnectorInitializer.initialize();

        contactPullController.start(null);
        contactPushEventListener.start(null);
        contactSyncDisablingEventListener.start(null);
        uploadListenerController.start(null);
        contactCreateEventListener.start(configurations);
        contactUpdateEventListener.start(configurations);
        contactDeleteEventListener.start(configurations);
        indexActionService.start(configurations.getNestedConfigs("index-action"));
        this.emailAlertEventService.start(configurations.getNestedConfigs("email-alert-action"));
        eventListenerRegistry.registerPostEventListener(contactDataPublisher);
        eventListenerRegistry.registerPostEventListener(contactDataRemover);
        eventListenerRegistry.registerPostEventListener(emailAlertPostEventListener);
        eventListenerRegistry.registerPostEventListener(activityPostEventListener);
        eventListenerRegistry.registerPreEventListener(quotaDealPreEventListener);
        eventListenerRegistry.registerPostEventListener(quotaDealPostEventListener);
        eventListenerRegistry.registerPostEventListener(quotaPostEventListener);
        eventListenerRegistry.registerPostEventListener(indexEventListener);

        listSummaryFetcherRegistry.addFetcher(Provider.CONTACT_LIST, contactCountSummaryFetcher);
        listSummaryFetcherRegistry.addFetcher(Provider.COMPANY_LIST, companyCountSummaryFetcher);
        listSummaryFetcherRegistry.addFetcher(Provider.DEAL_LIST, dealCountSummaryFetcher);
        listSummaryFetcherRegistry.addFetcher(Provider.PRODUCT_LIST, productCountSummaryFetcher);
        listSummaryFetcherRegistry.addFetcher(Provider.ACTIVITY_LIST, activityCountSummaryFetcher);
        listDynamicFieldsFetcherRegistry.add(Provider.CONTACT_LIST, contactDynamicFieldsFetcher);
        listDynamicFieldsFetcherRegistry.add(Provider.COMPANY_LIST, companyDynamicFieldsFetcher);
        listDynamicFieldsFetcherRegistry.add(Provider.DEAL_LIST, dealDynamicFieldsFetcher);
        listDynamicFieldsFetcherRegistry.add(Provider.PRODUCT_LIST, productDynamicFieldsFetcher);
        listPreviewRetrieverRegistry.add(Provider.CONTACT_LIST, contactListPreviewRetriever);
        listPreviewRetrieverRegistry.add(Provider.DEAL_LIST, dealListPreviewRetriever);
        listPreviewRetrieverRegistry.add(Provider.INVOICE_LIST, invoiceListPreviewRetriever);

        emailAlertFieldValueExtractorRegistry.add(EmailAlertFieldType.DEFAULT_FIELD_TYPE, defaultFieldValueExtractor);
        emailAlertFieldValueExtractorRegistry.add(EmailAlertFieldType.DATE_TIME, dateTimeFieldValueExtractor);
        emailAlertFieldValueExtractorRegistry.add(EmailAlertFieldType.TLC_MODEL, tlcModelFieldValueExtractor);

        /* Notification */

        writableDataStore.registerRowListener(Table.get(OMEMUSEREMAILSETTINGS.TABLE), emailSettingsListener);
        writableDataStore.registerRowListener(Table.get(OMEMBLOCKEDADDRESS.TABLE), blockedEmailAddressListener);
        scheduledEmailServiceController.start(null);
        emailTrashController.start(null);
        eventListenerRegistry.registerPostEventListener(dealUpdateEventListener);
        eventListenerRegistry.registerCompositeEventListener(lifeCycleDeleteEventListener);
        eventListenerRegistry.registerPostEventListener(segmentPostEventListener);
        analyticsExportConsumerController.start(configurations);
        eventListenerRegistry.registerPostEventListener(CoreUserUpdatePostEventListener.getInstance());

        /* Permission Set */
        columnDataPermissionValidatorRegistry.register(userContactRelationshipValidator);
        columnDataPermissionValidatorRegistry.register(userCompanyRelationshipValidator);
        columnDataPermissionValidatorRegistry.register(userDealRelationshipValidator);
        permissionHandlerRegistry.loadPermissionHandlers(PermissionHandlerType.SOURCE_MODEL_PERMISSION, modelPermissionHandler);

        /* Dashboard */
        dashboardTrashController.start(null);

        bulkActionService.start(ConfHolder.getInstance().getProperties("async-bulk-action"));

        writableDataStore.registerUpdatedEntryListener(Table.get(OMCRMROLE.TABLE), roleRowListener);
        rolePermissionCategoryListenerStore.add(Category.Type.FOLLOWERS, FollowersRolePermissionListener.getInstance());
        quotaDealEventController.start(configurations);
        quotaEventController.start(configurations);

        final Configurations orgOnboardingConfigs = configurations.getNestedConfigs("org-onboarding");

        if (Boolean.parseBoolean(orgOnboardingConfigs.optConfig(OrgOnboardFields.ENABLED, "false"))) {
            orgOnboardingService.start(orgOnboardingConfigs);
        }
        marketingListTransferService.start(configurations);

        /* Entity Insights (materialized per-entity rollup) */
        EntityInsightsListenerRegistrar.registerAll(writableDataStore);
        entityInsightsRecomputeScheduler.start(configurations);
        segmentRecomputeScheduler.start(configurations);
        segmentRecomputeConsumerService.start(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        ((ReadTaskRegistryImpl) readTaskRegistry).clear();

        importConsumerController.stop();
        importServiceHolder.stop();
        contactCreateEventListener.stop();
        contactUpdateEventListener.stop();
        contactDeleteEventListener.stop();
        uploadListenerController.stop();
        webFormSubmitController.stop();
        contactPullController.stop();
        contactPushEventListener.stop();
        contactSyncDisablingEventListener.stop();
        listSummaryFetcherRegistry.clear();
        listPreviewRetrieverRegistry.clear();

        /* Notification */

        scheduledEmailServiceController.stop();
        emailTrashController.stop();
        crmAnalyticsCacheInitializer.stop();

        bulkActionService.stop();
        analyticsExportConsumerController.stop();
        segmentRecomputeScheduler.stop();
        segmentRecomputeConsumerService.stop();

        rolePermissionCategoryListenerStore.clear();
        quotaDealEventController.stop();
        quotaEventController.stop();
        orgOnboardingService.stop();
        entityInsightsRecomputeScheduler.stop();
        this.emailAlertEventService.stop();
    }
}