
package jmail;

import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

public class DisallowReservedDomainsRule implements EmailValidationRule {
    private static final Set<String> reservedTopLevelDomains = new HashSet<>(
        Arrays.asList("test", "invalid", "example", "localhost"));
    private static final Set<TopLevelDomain> reservedExampleTlds = new HashSet<>(
        Arrays.asList(TopLevelDomain.DOT_COM, TopLevelDomain.DOT_NET, TopLevelDomain.DOT_ORG));

    @Override
    public boolean validate(Email email) {
        List<String> domainParts = email.domainParts();
        if (reservedTopLevelDomains.contains(domainParts.get(domainParts.size() - 1))) {
            return false;
        }
        if (domainParts.size() > 1
            && "example".equals(domainParts.get(domainParts.size() - 2))
            && reservedExampleTlds.contains(email.topLevelDomain())) {
            return false;
        }
        return true;
    }
}
