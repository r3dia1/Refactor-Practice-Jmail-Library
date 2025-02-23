
package jmail;

public class DisallowQuotedIdentifiersRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return !email.hasIdentifier();
    }
}
