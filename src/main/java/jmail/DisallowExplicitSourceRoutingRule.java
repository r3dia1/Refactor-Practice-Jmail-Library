
package jmail;

public class DisallowExplicitSourceRoutingRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return email.explicitSourceRoutes().size() <= 0;
    }
}
