package jmail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public final class EmailValidator {
    private final Set<EmailValidationRule> validationRules;

    EmailValidator(Set<EmailValidationRule> validationRules) {
        this.validationRules = Collections.unmodifiableSet(validationRules);
    }

    EmailValidator() {
        this(new HashSet<>());
    }

    public EmailValidator withRules(Collection<EmailValidationRule> rules) {
        Set<EmailValidationRule> ruleSet = new HashSet<>(validationRules);
        ruleSet.addAll(rules);
        return new EmailValidator(ruleSet);
    }

    public EmailValidator withRule(EmailValidationRule rule) {
        return withRules(Collections.singletonList(rule));
    }

    public boolean isValid(String email) {
        return JMail.tryParse(email)
            .filter(this::passesRules)
            .isPresent();
    }

    public boolean isInvalid(String email) {
        return !isValid(email);
    }

    public void enforceValid(String email) throws InvalidEmailException {
        if (!isValid(email)) {
            throw new InvalidEmailException();
        }
    }

    public EmailValidationResult validate(String email) {
        EmailValidationResult result = JMail.validate(email);
        if (!result.getEmail().isPresent()) return result;
        if (!passesRules(result.getEmail().get())) {
            return EmailValidationResult.failure(FailureReason.FAILED_CUSTOM_VALIDATION);
        }
        return result;
    }

    public Optional<Email> tryParse(String email) {
        return JMail.tryParse(email).filter(this::passesRules);
    }

    private boolean passesRules(Email email) {
        return validationRules.stream()
            .allMatch(rule -> rule.validate(email));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EmailValidator.class.getSimpleName() + "[", "]")
            .add("validationRuleCount=" + validationRules.size())
            .toString();
    }
}
