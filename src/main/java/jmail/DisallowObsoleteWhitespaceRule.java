
package jmail;

public class DisallowObsoleteWhitespaceRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return !email.containsWhitespace();
    }
}
