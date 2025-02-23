
package jmail;

public class DisallowIpDomainRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return !email.isIpAddress();
    }
}
