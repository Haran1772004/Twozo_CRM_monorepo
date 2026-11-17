package com.tlc.crm.core.internal.index.consumer;

import com.tlc.crm.common.config.manager.search.IndexManager;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.action.parser.impl.NativeParser;

import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.api.product.manager.ProductIndexManager;
import com.tlc.crm.core.features.api.activity.manager.ActivityIndexManager;
import com.tlc.crm.core.features.api.file.manager.FileIndexManager;
import com.tlc.crm.core.features.api.note.manager.NoteIndexManager;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Facilitates to process {@link IndexManager} actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexActionListener.class);

    private final DataParser dataParser;

    public IndexActionListener() {
        dataParser = NativeParser.getInstance();
    }

    /**
     * <p>
     * Enables to process the {@link StreamMessage} data.
     * </p>
     *
     * @param streamMessage Represents {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final Map<?, ?> data = new HashMap<>((Map<?, ?>) dataParser.deserialize(streamMessage.data()));
        final Collection<TlcModel> models = (Collection<TlcModel>) data.get(ActionFields.MODELS);
        final SearchIndex searchIndex = (SearchIndex) data.get(ActionFields.INDEX);
        final Action action = (Action) data.get(ActionFields.ACTION);

        try {
            final IndexManager<TlcModel> indexManager = get(searchIndex);

            switch (action) {
                case CREATE -> indexManager.create(models);
                case UPDATE -> indexManager.update(models);
                case PARTIAL_UPDATE -> indexManager.partialUpdate(models);
                case DELETE -> indexManager.delete(models);
                case MERGE -> indexManager.merge(models.iterator().next(), (Collection<Long>) data.get(ActionFields.DUPLICATE_IDS));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Indexing Record, Processing : {} {} action failed. IDs : {}",
                    searchIndex.getName(), action.name().toLowerCase(), models.stream().map(TlcModel::id).toList());
            throwable.printStackTrace();
        }
    }

    /**
     * <p>
     * Gets {@link IndexManager}.
     * </p>
     *
     * @param searchIndex Represents {@link SearchIndex}.
     * @return {@link IndexManager}.
     */
    private IndexManager get(final SearchIndex searchIndex) {

        return switch (searchIndex) {
            case DEAL -> DealIndexManager.getInstance();
            case CONTACT -> ContactIndexManager.getInstance();
            case COMPANY -> CompanyIndexManager.getInstance();
            case ACTIVITY -> ActivityIndexManager.getInstance();
            case PRODUCT -> ProductIndexManager.getInstance();
            case FILE -> FileIndexManager.getInstance();
            case NOTE -> NoteIndexManager.getInstance();
        };
    }
}