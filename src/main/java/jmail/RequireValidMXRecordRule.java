
package jmail;

import jmail.dns.DNSLookupUtil;

public class RequireValidMXRecordRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return DNSLookupUtil.hasMXRecord(email.domainWithoutComments());
    }
}
