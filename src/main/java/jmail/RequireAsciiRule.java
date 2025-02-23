
package jmail;

public class RequireAsciiRule implements EmailValidationRule {
    @Override
    public boolean validate(Email email) {
        return email.isAscii();
    }
}
