package com.tlc.email.common.resolver;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.service.EmailAddOnService;
import com.tlc.email.common.service.SmartBccService;
import com.tlc.email.common.service.TeamInboxService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * The {@code EmailServiceResolver} is a utility class responsible for mapping email
 * recipients to their corresponding registered add-on services.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailServiceResolver {

    private static final Collection<EmailAddOnService> REGISTERED_SERVICES = Set.of(
            SmartBccService.getInstance(), TeamInboxService.getInstance()
    );
    private static final String AT_SYMBOL = "@";

    private EmailServiceResolver() {
    }

    /**
     * <p>
     * Resolves the {@link ServiceType} for a single email address.
     * </p>
     *
     * @param emailAddress The email address to resolve
     * @return An {@link Optional} containing the matching {@link ServiceType} if found, otherwise empty
     */
    public static Optional<ServiceType> resolveServiceType(final String emailAddress) {
        return resolveServiceType(List.of(emailAddress))
                .stream()
                .findFirst();
    }

    /**
     * <p>
     * Resolves the services associated with the given recipient email addresses by matching each recipient's domain.
     * </p>
     *
     * <p>
     * Note: This method allows resolving multiple services at once for a given collection of recipients.
     * It is particularly useful for determining which service-specific email services should process an
     * incoming.
     * </p>
     *
     * @param recipients A collection of recipient email addresses to be evaluated.
     * @return A collection of resolved {@link ServiceType} instances.
     */
    private static Collection<ServiceType> resolveServiceType(final Collection<String> recipients) {
        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            return Collections.emptyList();
        }
        final Collection<ServiceType> serviceTypes = new HashSet<>();

        for (final String recipient : recipients) {

            if (Objects.isNull(recipient) || recipient.isEmpty()) {
                continue;
            }
            final int atIndex = recipient.indexOf(AT_SYMBOL);

            if (atIndex <= NumericalConstant.ZERO || atIndex == recipient.length() - NumericalConstant.ONE) {
                continue;
            }
            final String localPart = recipient.substring(NumericalConstant.ZERO, atIndex).toLowerCase();
            final String domainPart = recipient.substring(atIndex).toLowerCase();

            for (final EmailAddOnService service : REGISTERED_SERVICES) {
                final String serviceDomain = service.getDomain().toLowerCase();
                final ServiceType serviceType = service.getServiceType();

                if (domainPart.equals(serviceDomain) && localPart.startsWith(serviceType.getName().toLowerCase())) {
                    serviceTypes.add(serviceType);
                }
            }
        }

        return serviceTypes;
    }
}