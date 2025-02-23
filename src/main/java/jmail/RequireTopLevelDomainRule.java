
package jmail;

public class RequireTopLevelDomainRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return !email.topLevelDomain().equals(TopLevelDomain.NONE);
    }
}
