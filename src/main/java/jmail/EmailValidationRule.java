package jmail;

public interface EmailValidationRule {
	boolean validate(Email email);
}
