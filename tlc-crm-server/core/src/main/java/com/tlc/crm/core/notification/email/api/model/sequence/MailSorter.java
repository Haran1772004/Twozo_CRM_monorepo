package com.tlc.crm.core.notification.email.api.model.sequence;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.sql.common.api.dml.OrderByClause;

import java.util.Collection;
import java.util.Comparator;

/**
 * <p>
 * Implements the sorting action for email
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class MailSorter {

    /**
     * <p>
     * Gets the sorted email list based on the order provided.
     * </p>
     *
     * @param mails Collection of {@link EntityEmail}
     * @param order Represents the sorting order of mails either ascending or descending based on date
     * @return Collection for sorted mails
     */
    public static Collection<EntityEmail> sortByDate(final Collection<EntityEmail> mails, final OrderByClause.OrderType order) {
        final Comparator<EntityEmail> comparator = Comparator.comparing(mail -> mail.getMetaData().getDate());

        return switch (order) {
            case ASCENDING -> mails.stream().sorted(comparator).toList();
            case DESCENDING -> mails.stream().sorted(comparator.reversed()).toList();
        };
    }
}
