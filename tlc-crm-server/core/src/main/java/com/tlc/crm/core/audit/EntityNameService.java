package com.tlc.crm.core.audit;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;

/**
 * <p>
 * Provides the entity name given {@link NoteHolder.Source}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class EntityNameService {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;

    private EntityNameService() {
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
    }

    private static class Instance {
        private static final EntityNameService ENTITY_NAME_PROVIDER = new EntityNameService();
    }

    public static EntityNameService getInstance() {
        return Instance.ENTITY_NAME_PROVIDER;
    }

    /**
     * <p>
     * Fetches the name of the entity for the given entity id.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param entityId Represents the entity(Contact, Company, Deal) id.
     * @param source   Represents the {@link NoteHolder.Source}(such as Contact, Company, Deal).
     * @return The entity name.
     */
    public String fetchEntityName(final Long orgId, final Long entityId, final NoteHolder.Source source) {
        return switch (source) {
            case CONTACT -> contactManager.getContactFullName(orgId, entityId);
            case COMPANY -> companyManager.partialGet(orgId, entityId).getName();
            case DEAL -> DealManager.getInstance().partialGet(orgId, entityId).getTitle();
        };
    }
}
