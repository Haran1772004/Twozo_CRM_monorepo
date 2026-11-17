package com.tlc.crm.core.notification.email.template.api.extractor;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.notification.email.template.status.TemplateErrorCodes;
import com.tlc.crm.core.template.PlaceHolderValueInterpreter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.iam.common.models.User;
import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.notification.service.template.PlaceHolderExtractor;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides implementations for {@link PlaceHolder} handling functionalities related to {@link EntityEmail}.
 *
 * @author AswiniN
 * @version 1.0
 */
public class MailPlaceHolderExtractor implements PlaceHolderExtractor<EntityMailTemplate> {

    private static final int CONTACT_FULL_NAME_FIELD_ID = NumericalConstant.NEGATIVE_ONE;

    private final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\$\\{.*?_(-?\\d+)\\}");
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final PlaceHolderValueInterpreter placeHolderValueInterpreter;
    private final FieldService fieldService;

    private MailPlaceHolderExtractor() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        placeHolderValueInterpreter = PlaceHolderValueInterpreter.getInstance();
        fieldService = FieldService.getInstance();
    }

    private static class Instance {
        private static final MailPlaceHolderExtractor INSTANCE = new MailPlaceHolderExtractor();
    }

    /**
     * Gets instance of {@link MailPlaceHolderExtractor}.
     *
     * @return {@link MailPlaceHolderExtractor} instance
     */
    public static MailPlaceHolderExtractor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the mail placeholder pattern
     * </p>
     *
     * @return the placeholder pattern
     */
    public Pattern getPattern() {
        return PLACE_HOLDER_PATTERN;
    }

    /**
     * {@inheritDoc}
     *
     * @param template Refers to the template
     * @return {@link Collection} of {@link PlaceHolder}
     */
    @Override
    public Collection<MailPlaceHolder> getPlaceHolders(final EntityMailTemplate template) {

        if (Objects.nonNull(template)) {
            final Source source = template.getSource();
            final long orgId = template.orgId();
            final Long entityId = template.getEntityId();

            if (Objects.nonNull(source)) {
                if (source == Source.CONTACT) {
                    if (contactManager.exists(orgId, entityId)) {
                        final Contact contact = contactManager.get(orgId, entityId);

                        if (Objects.nonNull(contact)) {
                            final Company contactCompany = contact.getCompany();

                            if (Objects.nonNull(contactCompany)) {
                                contact.setCompany(companyManager.get(orgId, contactCompany.id()));
                            }
                            final Long dealId = contactManager.getRecentlyLinkedDealId(orgId, entityId);

                            if (Objects.nonNull(dealId) && dealManager.exists(orgId, dealId)) {
                                final Deal deal = dealManager.get(orgId, dealId);

                                if (Objects.nonNull(deal)) {
                                    final Contact dealContact = deal.getPrimaryContact();
                                    final Company dealCompany = deal.getCompany();

                                    if (Objects.nonNull(dealContact)) {
                                        deal.setPrimaryContact(contactManager.get(orgId, dealContact.id()));
                                    }

                                    if (Objects.nonNull(dealCompany)) {
                                        deal.setCompany(companyManager.get(orgId, dealCompany.id()));
                                    }
                                    return getPlaceHolders(template, deal);
                                }
                            }
                            return getPlaceHolders(template, contact);
                        }
                    } else {
                        throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                    }
                } else if (source == Source.DEAL) {

                    if (dealManager.exists(orgId, entityId)) {
                        final Deal deal = dealManager.get(orgId, entityId);

                        if (Objects.nonNull(deal)) {
                            final Contact dealContact = deal.getPrimaryContact();
                            final Company dealCompany = deal.getCompany();

                            if (Objects.nonNull(dealContact)) {
                                deal.setPrimaryContact(contactManager.get(orgId, dealContact.id()));
                            }

                            if (Objects.nonNull(dealCompany)) {
                                deal.setCompany(companyManager.get(orgId, dealCompany.id()));
                            }
                            return getPlaceHolders(template, deal);
                        }
                    } else {
                        throw ErrorCode.get(ContactErrorCodes.ENTITY_NOT_FOUND, "i18n.deal.not.found");
                    }
                } else if (source == Source.COMPANY) {

                    if (companyManager.exists(orgId, entityId)) {
                        final Company company = companyManager.get(orgId, entityId);

                        if (Objects.nonNull(company)) {
                            return getPlaceHolders(template, company);
                        }
                    } else {
                        throw ErrorCode.get(ContactErrorCodes.ENTITY_NOT_FOUND, "i18n.company.not.found");
                    }
                } else {
                    throw ErrorCode.get(TemplateErrorCodes.SOURCE_NOT_FOUND, "i18n.email.template.source.not.found");
                }
            } else {
                return getPlaceHolders(template, null);
            }

            return null;
        }
        return null;
    }

    /**
     * <p>
     * Collects the {@link PlaceHolder} present in subject field.
     * </p>
     *
     * @param placeHolders Refers to the {@link Collection} of {@link PlaceHolder}
     * @return collection of placeholders.
     */
    public Collection<PlaceHolder> getSubjectPlaceHolders(final Collection<PlaceHolder> placeHolders) {
        final Collection<PlaceHolder> subjectPlaceHolders = new HashSet<>();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (final PlaceHolder placeHolder : placeHolders) {

                if (((MailPlaceHolder) placeHolder).isSubjectPlaceHolder()) {
                    subjectPlaceHolders.add(placeHolder);
                }
            }
        }

        return subjectPlaceHolders;
    }

    /**
     * <p>
     * Collects the {@link PlaceHolder} present in content field.
     * </p>
     *
     * @param placeHolders Refers to the {@link Collection} of {@link PlaceHolder}
     * @return collection of placeholders.
     */
    public Collection<PlaceHolder> getContentPlaceHolders(final Collection<PlaceHolder> placeHolders) {
        final Collection<PlaceHolder> contentPlaceHolders = new HashSet<>();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (final PlaceHolder placeHolder : placeHolders) {

                if (!((MailPlaceHolder) placeHolder).isSubjectPlaceHolder()) {
                    contentPlaceHolders.add(placeHolder);
                }
            }
        }

        return contentPlaceHolders;
    }

    /**
     * <p>
     * Resolves the placeHolder values with appropriate value of user name from provided user map
     * </p>
     *
     * @param placeHolders Represents the {@link Collection} of {@link MailPlaceHolder}
     * @param crmUsers     Represents the {@link Collection} of {@link CrmUser}
     * @return {@link Collection} of {@link PlaceHolder}
     */
    public Collection<PlaceHolder> resolvePlaceHoldersWithUser(final Collection<MailPlaceHolder> placeHolders,
                                                               final Map<Long, User> crmUsers) {
        final Collection<PlaceHolder> placeHolderList = new HashSet<>();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (MailPlaceHolder placeHolder : placeHolders) {

                if (placeHolder.isOwnerField()) {
                    final User user = crmUsers.get(placeHolder.getLong());

                    if (Objects.nonNull(user)) {
                        placeHolder.setValue(user.getFullName());
                    }
                }
                placeHolderList.add(placeHolder);
            }
        }

        return placeHolderList;
    }

    /**
     * <p>
     * Checks whether the content provided has placeholder elements in it
     * </p>
     *
     * @param content   Refers to the content provided
     * @return {@link Boolean} true if placeholder present
     */
    public boolean hasPlaceHolders(final String content) {
        return Objects.nonNull(content) && PLACE_HOLDER_PATTERN.matcher(content).find();
    }

    /**
     * <p>
     * Fetches the placeholders provided by the user from the content given.
     * </p>
     *
     * @param content   Represents the content from which placeholder fields are extracted
     * @return {@link Collection} of {@link String} placeholders
     */
    private Collection<Long> getPlaceHoldersFieldIds(final String content) {
        final Collection<Long> fieldIds = new HashSet<>();
        final Matcher matcher = PLACE_HOLDER_PATTERN.matcher(content);

        while (matcher.find()) {
            fieldIds.add(Long.parseLong(matcher.group(NumericalConstant.ONE)));
        }
        return fieldIds;
    }

    /**
     * <p>
     * Fetches the {@link Collection} of {@link MailPlaceHolder} filled with appropriate values
     * </p>
     *
     * @param template Refers to {@link EntityMailTemplate}
     * @param entity   Refers to {@code Entity}.
     * @return {@link Collection} of {@link MailPlaceHolder}
     */
    private Collection<MailPlaceHolder> getPlaceHolders(final EntityMailTemplate template, final Entity entity) {
        final Collection<MailPlaceHolder> placeHolders = new ArrayList<>();
        final Set<Long> fieldIds = new HashSet<>();
        final String subject = template.getSubject();
        final String content = template.getContent();
        final Collection<Long> subjectPlaceHolderFieldIds = Objects.isNull(subject) ? Collections.emptyList() : getPlaceHoldersFieldIds(subject);
        final Collection<Long> contentPlaceHolderFieldIds = Objects.isNull(content) ? Collections.emptyList() : getPlaceHoldersFieldIds(content);

        if (!subjectPlaceHolderFieldIds.isEmpty()) {
            fieldIds.addAll(subjectPlaceHolderFieldIds);
        }

        if (!contentPlaceHolderFieldIds.isEmpty()) {
            fieldIds.addAll(contentPlaceHolderFieldIds);
        }

        if (!fieldIds.isEmpty()) {
            final Map<Long, FieldInfo> fieldInfoMap = fieldService.getFieldsById(template.orgId(), fieldIds);
            final CrmUser user = LoggedInUserInfo.getUser();
            final UserRegionalPreference regionalPreference = user.getPreferenceHolder().get(UserRegionalPreference.TYPE);
            final TimeZone timeZone = regionalPreference.getTimezoneInfo().getTimeZone();
            final BiConsumer<Collection<Long>, Boolean> placeHolderConsumer = (placeHolderFieldIds, isSubject) -> {

                for (final Long placeHolderFieldId : placeHolderFieldIds) {
                    fillPlaceHolders(placeHolderFieldId, Objects.isNull(entity) ? null : entity,
                            placeHolders, isSubject, fieldInfoMap, timeZone);
                }
            };

            if (template.isSubjectHasPlaceHolders()) {
                placeHolderConsumer.accept(subjectPlaceHolderFieldIds, true);
            }

            if (template.isContentHasPlaceHolders()) {
                placeHolderConsumer.accept(contentPlaceHolderFieldIds, false);
            }
        }

        return placeHolders;
    }

    /**
     * <p>
     * Fills the placeholders with the entity values provided based on the field id.
     * </p>
     *
     * @param fieldId      Represents the field ID
     * @param entity       Represents the {@link Entity}
     * @param placeHolders Represents the {@link Collection} of {@link MailPlaceHolder}
     * @param isSubject    A flag to indicate whether subject or content field
     * @param fieldInfoMap Represents the {@link Map} of {@link FieldInfo}
     * @param timeZone     Represents the user timezone
     */
    private void fillPlaceHolders(final Long fieldId, final Entity entity,
                                  final Collection<MailPlaceHolder> placeHolders, final boolean isSubject,
                                  final Map<Long, FieldInfo> fieldInfoMap, final TimeZone timeZone) {

        if (Objects.nonNull(fieldId)) {
            final FieldInfo fieldInfo = fieldInfoMap.get(fieldId);
            final MailPlaceHolder placeHolder = new MailPlaceHolder();
            final Consumer<Long> placeHolderConsumer = (crmFieldId) -> {
                placeHolder.setKey(crmFieldId.toString());
            };

            if (Objects.isNull(entity)) {
                placeHolder.setValue(null);
                placeHolderConsumer.accept(fieldId);
            } else {
                final String entityType = entity.getEntityType();

                if (CONTACT_FULL_NAME_FIELD_ID == fieldId) {
                    if (Objects.equals(entityType, Deal.ENTITY_TYPE)) {
                        placeHolder.setValue(((Deal) entity).getPrimaryContact().getFullName());
                    } else if (Objects.equals(entityType, Contact.ENTITY_TYPE)) {
                        placeHolder.setValue(((Contact) entity).getFullName());
                    } else {
                        placeHolder.setValue(null);
                    }
                    placeHolderConsumer.accept(fieldId);
                } else if (Objects.isNull(fieldInfo)) {
                    placeHolder.setValue(null);
                    placeHolderConsumer.accept(fieldId);
                } else {
                    placeHolderConsumer.accept(fieldId);
                    placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, entity, timeZone);
                }
            }

            if (Objects.nonNull(fieldInfo)) {
                placeHolder.setOwnerField(fieldInfo.storeType().isOwnerType());
            }
            placeHolder.setSubjectPlaceHolder(isSubject);
            placeHolders.add(placeHolder);
        }
    }
}