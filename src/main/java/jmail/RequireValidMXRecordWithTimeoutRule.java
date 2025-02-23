
package jmail;

import jmail.dns.DNSLookupUtil;

public class RequireValidMXRecordWithTimeoutRule implements EmailValidationRule {
    private final int initialTimeout;
    private final int numRetries;

    public RequireValidMXRecordWithTimeoutRule(int initialTimeout, int numRetries) {
        this.initialTimeout = initialTimeout;
        this.numRetries = numRetries;
    }

    @Override
    public boolean validate(Email email) {
        return DNSLookupUtil.hasMXRecord(email.domainWithoutComments(), initialTimeout, numRetries);
    }
}
