package com.tlc.crm.webform.action;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.file.client.service.AttachmentService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.webform.api.model.FormElement;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.builder.TemplateBuilder;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.action.res.CrmHtmlResponse;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.api.manager.CurrencyUnitManager;
import com.tlc.crm.entity.currency.api.model.CurrencyData;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.WebFormTemplateRequest;
import com.tlc.crm.webform.field.WebFormFields;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the webform render related action
 *
 * @author Kishore J
 * @version 1.0
 */
@WebAction(path = "/webform/render/{id}", method = HttpMethod.GET)
public class WebFormRenderAction extends CrmPublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFormRenderAction.class);

    private static final String CONTENT_DISPOSITION_FORMAT = "inline; filename=%s";
    private static final String RESPONSE_CONTENT_TYPE = "response-content-type";
    private static final String RESPONSE_CONTENT_DISPOSITION = "response-content-disposition";
    private static final String WEB_FORM = "webForm";
    private final WebFormManager webFormManager;
    private final String webFormSubmitUrl;
    private final HtmlParser htmlParser;
    private final FieldService fieldService;
    private final BaseCurrencyService baseCurrencyService;
    private final CurrencyUnitManager currencyUnitManager;
    private final SecureTaskHandler secureTaskHandler;
    private final OptionService optionService;
    private final AttachmentService attachmentService;

    public WebFormRenderAction() {
        webFormManager = WebFormManager.getInstance();
        htmlParser = HtmlParser.getInstance();
        fieldService = FieldService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        currencyUnitManager = CurrencyUnitManager.getInstance();
        secureTaskHandler = SecureTaskHandler.getInstance();
        optionService = OptionService.getInstance();
        webFormSubmitUrl = ConfHolder.getInstance().getProperties("webform").getConfig("redirectUrl.submit");
        attachmentService = AttachmentService.getInstance();
    }

    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = Json.object(Base64.getDecoder().decode(request.getParameter(WebFormFields.ID.getName())));
        final long orgId = requestJson.getLong(WebFormFields.ORG_ID.getName());
        final long id = requestJson.getLong(WebFormFields.ID.getName());
        final long ownerId = requestJson.getLong(WebFormFields.OWNER_ID.getName());
        final CrmHtmlResponse crmHtmlResponse = crmResponse.convertTo(CrmHtmlResponse.class);

        LOGGER.info("Webform: Before secureProcess. Thread: {}, OrgId: {}, Id: {}", Thread.currentThread().getName(), orgId, id);

        secureTaskHandler.secureProcess(orgId, ownerId, () -> {
            LOGGER.info("Webform: Lambda task started. Thread: {}, OrgId: {}, Id: {}", Thread.currentThread().getName(), orgId, id);
            try {
                final Optional<WebForm> webForm = webFormManager.get(orgId, List.of(id)).stream().findAny();

                if (webForm.isPresent()) {
                    final WebForm form = webForm.get();
                    final WebFormTemplateRequest webFormTemplateRequest = new WebFormTemplateRequest();
                    final Map<String, TemplateModel> emailNotifiableModels = new HashMap<>();
                    final Map<Long, Collection<Option>> optionsMap = new HashMap<>();
                    final boolean isIframe = requestJson.getBoolean(WebFormFields.IS_IFRAME.getName());

                    webFormTemplateRequest.setIframe(isIframe);
                    webFormTemplateRequest.setFormSubmitUrl(HttpUtil.buildAbsoluteUrl(webFormSubmitUrl));
                    webFormTemplateRequest.setWebForm(form);
                    final Collection<FormFieldProperty> formFieldProperties = form.getFormFieldProperties().stream().sorted(Comparator.comparing(FormFieldProperty::order)).toList();
                    final Map<Long, FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.CONTACT.getName()).stream().collect(Collectors.toMap(FieldInfo::id, Function.identity()));
                    boolean isCurrencyDataLoaded = false;
                    final Collection<FormFieldProperty> activeFieldProperties = new LinkedHashSet<>();

                    for (final FormFieldProperty formFieldProperty : formFieldProperties) {

                        if (formFieldProperty instanceof final FormField formField) {
                            final Long fieldId = formField.getFieldId();
                            final FieldInfo fieldInfo = fields.get(fieldId);

                            if (Objects.nonNull(fieldInfo)) {
                                final StoreType storeType = fieldInfo.storeType();
                                activeFieldProperties.add(formFieldProperty);

                                formField.setStoreType(storeType);

                                switch (storeType) {
                                    case DROPDOWN, MULTI_SELECT -> {
                                        optionsMap.put(fieldId, optionService.get(orgId, fieldInfo));
                                        webFormTemplateRequest.setOptionMap(optionsMap);
                                    }
                                    case MONETARY -> {
                                        if (!isCurrencyDataLoaded) {
                                            webFormTemplateRequest.setCurrencyDataList(getCurrencies(orgId));
                                            isCurrencyDataLoaded = true;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (formFieldProperty instanceof final FormElement formElement && FormElement.Category.IMAGE == formElement.getCategory()) {
                                final JsonObject configJson = formElement.getConfigAsJson();
                                final Object imageAttachmentId = configJson.opt(WebFormFields.ATTACHMENT_ID.getName());

                                if (Objects.nonNull(imageAttachmentId)) {
                                    final long attachmentId = ((Number) imageAttachmentId).longValue();
                                    final String imageUrl = getImagePresignedUrl(attachmentId, formElement.getLabel());
                                    final JsonObject enrichedConfig = Json.object(configJson.toString());

                                    enrichedConfig.put(WebFormFields.URL.getName(), imageUrl);
                                    formElement.setConfig(enrichedConfig.toString());
                                }
                            }
                            activeFieldProperties.add(formFieldProperty);
                        }
                    }
                    form.setFormFieldProperties(activeFieldProperties);
                    emailNotifiableModels.put(WEB_FORM, webFormTemplateRequest);
                    final DefaultTemplateBuildRequest<TemplateModel> templateDataHolder = new DefaultTemplateBuildRequest<>(emailNotifiableModels);
                    final Optional<String> template = TemplateBuilder.getInstance().build(WEB_FORM, templateDataHolder);

                    if (template.isPresent()) {
                        final HtmlDocument htmlDocument = htmlParser.parse(template.get());

                        if (isIframe) {
                            crmHtmlResponse.enableIFrameSupport();
                        }
                        crmHtmlResponse.put(htmlDocument);
                        crmHtmlResponse.applyCSRFProtection(requestJson.toString());
                        webFormManager.updateVisitDetails(id, orgId, form);
                    }
                }
            } catch (final Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Webform: Error while processing webform render for Org Id: {}, Id: {}", orgId, id);
                exception.printStackTrace();
            } finally {
                LOGGER.info("Webform: About to send html response. Thread: {}, OrgId: {}, Id: {}", Thread.currentThread().getName(), orgId, id);
                crmHtmlResponse.send();
            }
        });

        LOGGER.info("Webform: pProcess method about to return. Thread: {}, OrgId: {}, Id: {}", Thread.currentThread().getName(), orgId, id);
    }

    /**
     * <p>
     * Fetches the collection of {@link CurrencyData}
     * </p>
     *
     * @param orgId Represents an org id
     * @return The Collection of {@link CurrencyData}
     */
    public Collection<CurrencyData> getCurrencies(final long orgId) {
        final com.tlc.l10n.model.currency.Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final Collection<CurrencyData> currencyList = new HashSet<>(currencyUnitManager.getCurrencies(orgId));

        currencyList.add(new CurrencyData(true, true, defaultCurrency));

        return currencyList.stream().sorted(Comparator.comparing(CurrencyData::isActive).reversed()
                .thenComparing(data -> data.currency().getCode())).toList();
    }

    /**
     * <p>
     * To ensure secure processing of user tasks. It manages the execution of tasks
     * in a secure context.
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     * @see SecureUserTaskProcessor
     */
    private static class SecureTaskHandler implements SecureUserTaskProcessor {

        private SecureTaskHandler() {
        }

        private static class Instance {
            private static final SecureTaskHandler INSTANCE = new SecureTaskHandler();
        }

        /**
         * <p>
         * Gets the {@link SecureTaskHandler} Instance
         * </p>
         *
         * @return {@link SecureTaskHandler}
         */
        public static SecureTaskHandler getInstance() {
            return Instance.INSTANCE;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @param orgId  Represents the organization ID
         * @param userId Represents the userID
         * @param task   the task to be executed, with the security context value loaded before execution
         */
        @Override
        public void secureProcess(final long orgId, final long userId, final RunnableTask task) {
            SecureUserTaskProcessor.super.secureProcess(orgId, userId, task);
        }
    }

    /**
     * Builds a presigned blob storage URL for the image attachment,
     * so it can be embedded directly in the form's HTML.
     *
     * @param attachmentId the image attachment id
     * @param fileName     the image file name
     * @return presigned URL pointing to blob storage
     */
    private String getImagePresignedUrl(final long attachmentId, final String fileName) {
        final String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        final int lastDot = fileName.lastIndexOf('.');
        final String extension = lastDot >= NumericalConstant.ZERO ? fileName.substring(lastDot + NumericalConstant.ONE) : "png";
        final String contentType = String.join("/", "image", extension);
        final String contentDisposition = String.format(CONTENT_DISPOSITION_FORMAT, encodedFileName);

        return attachmentService.getPresignedUrl(attachmentId, NumericalConstant.ONE,
                Map.of(RESPONSE_CONTENT_TYPE, contentType, RESPONSE_CONTENT_DISPOSITION, contentDisposition));
    }
}