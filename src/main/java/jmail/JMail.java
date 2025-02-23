package jmail;

import jmail.net.InternetProtocolAddress;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides static methods to validate an email address
 * using standard RFC validation, or to create a new {@link EmailValidator}.
 */
public final class JMail {
	private static final int MAX_EMAIL_LENGTH = 320;
	private static final int MAX_LOCAL_PART_LENGTH = 64;
	private static final int MAX_DOMAIN_LENGTH = 255;
	private static final int MAX_DOMAIN_PART_LENGTH = 63;

  /**
   * Private constructor to prevent instantiation.
   */
  private JMail() {
  }

  /**
   * Returns a new instance of {@link EmailValidator}. In general,
   * you should favor using {@link #strictValidator()} instead of this method,
   * as this method will allow IP address domain literals, dotless domains, and explicit
   * source routing unless configured separately.
   *
   * @return the new {@link EmailValidator} instance
   */
  public static EmailValidator validator() {
    return new EmailValidator();
  }

  /**
   * Returns a new instance of {@link EmailValidator} with stricter rules applied
   * to the validator. The following rules are applied and do not need to be
   * added:
   *
   * <ul>
   *   <li>The email address cannot have an IP Address domain
   *   <li>The email address cannot have a dotless domain
   *   <li>The email address cannot have explicit source routing
   * </ul>
   *
   * @return the new {@link EmailValidator} instance
   */
  public static EmailValidator strictValidator() {
    return new EmailValidator()
    		.withRule(new DisallowIpDomainRule())
    	    .withRule(new RequireTopLevelDomainRule())
    	    .withRule(new DisallowExplicitSourceRoutingRule());
  }

  /**
   * Return true if the given email address passes basic RFC validation. See
   * {@link #tryParse(String)} for details on what is required of an email address
   * within basic validation.
   *
   * @param email the email address to validate
   * @return true if the given string is a valid email address, false otherwise
   */
  public static boolean isValid(String email) {
    return tryParse(email).isPresent();
  }

  /**
   * Return true if the given email address fails basic RFC validation. See
   * {@link #tryParse(String)} for details on what is required of an email address
   * within basic validation.
   *
   * @param email the email address to validate
   * @return true if the given string is not a valid email address, false otherwise
   */
  public static boolean isInvalid(String email) {
    return !tryParse(email).isPresent();
  }

  /**
   * Require that the given email address passes basic RFC validation, throwing
   * {@link InvalidEmailException} if the address is invalid. See {@link #tryParse(String)} for
   * details on what is required of an email address within basic validation.
   *
   * @param email the email address to validate
   * @throws InvalidEmailException if the validation fails
   */
  public static void enforceValid(String email) throws InvalidEmailException {
    if (!tryParse(email).isPresent()) {
      throw new InvalidEmailException();
    }
  }

  /**
   * Determine if the given email address is valid, returning a new {@link EmailValidationResult}
   * object that contains details on the result of the validation. Use this method if you need to
   * see the {@link FailureReason} upon validation failure. See {@link #tryParse(String)}
   * for details on what is required of an email address within basic validation.
   *
   * @param email the email address to validate
   * @return a {@link EmailValidationResult} containing success or failure, along with the parsed
   *         {@link Email} object if successful, or the {@link FailureReason} if not
   */
  public static EmailValidationResult validate(String email) {
    return validateInternal(email);
  }

  /**
   * Parse the given email address into a new {@link Email} object. This method does basic
   * validation on the input email address. This method does not claim to be 100%
   * accurate in determining if an email address is valid or invalid due to the
   * complexity of the email RFC standards. That being said, if you come across
   * an email address that you expect to be valid that fails validation, or
   * vice-versa, please open an issue at the
   * <a href="https://github.com/RohanNagar/jmail">GitHub repo</a> so it can be fixed.
   *
   * <p>In general, this method should be more or less compliant with the latest RFCs.
   *
   * @param email the email address to parse
   * @return an {@link Optional} containing the parsed {@link Email}, or empty if the email
   *         is invalid
   */
  public static Optional<Email> tryParse(String email) {
    EmailValidationResult result = validateInternal(email);

    return result.getEmail();
  }

  /**
   * Internal parsing method.
   *
   * @param email the email address to parse
   * @return a new {@link Email} instance if valid, empty if invalid
   */
  private static EmailValidationResult validateInternal(String email) {
	    EmailValidationResult initialValidationResult = initialValidationChecks(email);
	    if (initialValidationResult != null) return initialValidationResult;
	
	    List<String> sourceRoutes = Collections.emptyList();
	    String fullSourceRoute = "";
	
	    if (email.charAt(0) == '@') {
	    	Optional<SourceRouteDetail> sourceRoute = validateSourceRouting(email);

	        // If the sourceRoute is not present, then either the route was invalid or there was no
	        // source routing. In either case, starting with the @ symbol would be invalid.
	        if (!sourceRoute.isPresent()) {
	          return EmailValidationResult.failure(FailureReason.BEGINS_WITH_AT_SYMBOL);
	        }

	        // Otherwise, update the email to validate to be just the actual email
	        SourceRouteDetail detail = sourceRoute.get();
	        sourceRoutes = detail.routes;
	        fullSourceRoute = detail.fullRoute.toString();

	        email = email.substring(fullSourceRoute.length());
	    }
	
	    int size = email.length();
	    EmailValidationResult sizeValidationResult = sizeValidationChecks(email, size);
	    if (sizeValidationResult != null) return sizeValidationResult;
	
	    EmailParsingContext context = new EmailParsingContext(size);
	    EmailValidationResult parsingResult = parseEmail(email, size, context);
	    if (parsingResult != null) return parsingResult;
	
	    return finalValidationChecks(context, fullSourceRoute, sourceRoutes);
	}
	
	private static EmailValidationResult initialValidationChecks(String email) {
	    if (email == null) return EmailValidationResult.failure(FailureReason.NULL_ADDRESS);
	    if (email.length() < 3) return EmailValidationResult.failure(FailureReason.ADDRESS_TOO_SHORT);
	    return null;
	}
	
//	private static EmailValidationResult validateSourceRouting(String email) {
//	    Optional<SourceRouteDetail> sourceRoute = validateSourceRouting(email);
//	    if (!sourceRoute.isPresent()) {
//	        return EmailValidationResult.failure(FailureReason.BEGINS_WITH_AT_SYMBOL);
//	    }
//	    return null;
//	}
//	
	private static EmailValidationResult sizeValidationChecks(String email, int size) {
	    if (size > MAX_EMAIL_LENGTH) return EmailValidationResult.failure(FailureReason.ADDRESS_TOO_LONG);
	    if (email.charAt(0) == '.') return EmailValidationResult.failure(FailureReason.STARTS_WITH_DOT);
	    if (email.charAt(size - 1) == '.') return EmailValidationResult.failure(FailureReason.ENDS_WITH_DOT);
	    if (email.charAt(size - 1) == '-') return EmailValidationResult.failure(FailureReason.DOMAIN_PART_ENDS_WITH_DASH);
	    return null;
	}
	
	private static EmailValidationResult parseEmail(String email, int size, EmailParsingContext context) {
	    for (int i = 0; i < size; i++) {
	        char c = email.charAt(i);
	        if (c >= 128) context.isAscii = false;
	
	        if (c == '<' && !context.inQuotes && !context.previousBackslash) {
	            if (!(email.charAt(size - 1) == '>')) {
	                return EmailValidationResult.failure(FailureReason.UNQUOTED_ANGLED_BRACKET);
	            }
	            EmailValidationResult innerResult = validateInternal(email.substring(i + 1, size - 1));
	            return innerResult.getEmail()
	                .map(e -> EmailValidationResult.success(new Email(e, context.localPart.toString())))
	                .orElse(innerResult);
	        }
	
	        if (c == '@' && !context.inQuotes && !context.previousBackslash) {
	            if (context.atFound) return EmailValidationResult.failure(FailureReason.MULTIPLE_AT_SYMBOLS);
	            if (context.requireAngledBracket) return EmailValidationResult.failure(FailureReason.INVALID_WHITESPACE);
	            context.atFound = true;
	            context.requireAtOrDot = context.requireAtDotOrComment = false;
	            context.whitespace = false;
	            context.previousDot = true;
	            continue;
	        }
	
	        if (c == '\n') {
	            if (context.charactersOnLine <= 0) {
	                return EmailValidationResult.failure(FailureReason.INVALID_WHITESPACE);
	            }
	            context.charactersOnLine = 0;
	        } else if (c != '\r') {
	            context.charactersOnLine++;
	        }
	
	        if (context.requireAtOrDot) {
	            if (!isWhitespace(c) && c != '.') {
	                return EmailValidationResult.failure(FailureReason.INVALID_COMMENT_LOCATION);
	            } else context.requireAtOrDot = false;
	        }
	
	        if (context.requireAtDotOrComment) {
	            if (!isWhitespace(c) && c != '.' && c != '(') {
	                return EmailValidationResult.failure(FailureReason.INVALID_QUOTE_LOCATION);
	            } else context.requireAtDotOrComment = false;
	        }
	
	        if (context.whitespace) {
	            if (!context.previousDot && !context.previousComment) {
	                if (c != '.' && c != '@' && c != '(' && !isWhitespace(c)) {
	                    if (!context.atFound) context.requireAngledBracket = true;
	                    else return EmailValidationResult.failure(FailureReason.INVALID_WHITESPACE);
	                }
	            }
	        }
	
	        if (context.requireQuotedAtOrDot && context.inQuotes) {
	            if (c != '.' && c != '@' && !isWhitespace(c) && c != '"') {
	                context.removableQuotePair = false;
	            } else if (!isWhitespace(c) && c != '"') {
	                context.requireQuotedAtOrDot = false;
	            }
	        }
	
	        if (c == '(' && context.inQuotes && !context.previousBackslash) {
	            context.removableQuotePair = false;
	        }
	
	        if (c == '(' && !context.inQuotes) {
	            Optional<String> comment = validateComment(email.substring(i));
	            if (!comment.isPresent()) {
	                return EmailValidationResult.failure(FailureReason.INVALID_COMMENT);
	            }
	            String commentStr = comment.get();
	            int commentStrLen = commentStr.length();
	            if (!context.atFound && (i != 0 && !context.previousDot)) {
	                context.requireAtOrDot = true;
	            } else if (context.atFound && !context.firstDomainChar && !context.previousDot) {
	                if (!(i + commentStrLen == size)) context.requireAtOrDot = true;
	            }
	            i += (commentStrLen - 1);
	            if (!context.atFound) {
	                context.localPart.append(commentStr);
	                context.localPartCommentLength += commentStrLen;
	            } else {
	                context.domain.append(commentStr);
	                context.domainCommentLength += commentStrLen;
	            }
	            context.previousComment = true;
	            context.comments.add(commentStr.substring(1, commentStrLen - 1));
	            continue;
	        }
	
	        if (c == '.' && context.previousDot) {
	            if (!context.inQuotes) {
	                return EmailValidationResult.failure(FailureReason.MULTIPLE_DOT_SEPARATORS);
	            } else {
	                context.removableQuotePair = false;
	            }
	        }
	
	        if (!context.atFound) {
	            if (c == '"' && i > 0 && !context.previousDot && !context.inQuotes) {
	                return EmailValidationResult.failure(FailureReason.INVALID_QUOTE_LOCATION);
	            }
	            boolean mustBeQuoted = JMail.DISALLOWED_UNQUOTED_CHARACTERS.contains(c);
	            if (c != '"' && !context.inQuotes && !context.previousBackslash && mustBeQuoted) {
	                return EmailValidationResult.failure(FailureReason.DISALLOWED_UNQUOTED_CHARACTER);
	            }
	            if (mustBeQuoted && context.inQuotes && !context.previousBackslash && c != '"') {
	                context.removableQuotePair = false;
	            }
	            if (!context.inQuotes && context.previousBackslash && !mustBeQuoted && c != ' ' && c != '\\') {
	                return EmailValidationResult.failure(FailureReason.UNUSED_BACKSLASH_ESCAPE);
	            }
	            if (context.inQuotes) {
	                if (JMail.ALLOWED_QUOTED_WITH_ESCAPE.contains(c)) {
	                    if (!context.previousBackslash) {
	                        return EmailValidationResult.failure(FailureReason.MISSING_BACKSLASH_ESCAPE);
	                    }
	                    context.removableQuotePair = false;
	                }
	            }
	            context.localPart.append(c);
	            context.localPartWithoutComments.append(c);
	            if (c != '"') {
	                if (context.inQuotes) {
	                    context.currentQuote.append(c);
	                } else {
	                    context.localPartWithoutQuotes.append(c);
	                }
	            }
	        } else {
	            if (context.firstDomainChar && c == '[') {
	                String ipDomain = email.substring(i);
	                if (!ipDomain.startsWith("[") || !ipDomain.endsWith("]") || ipDomain.length() < 3) {
	                    return EmailValidationResult.failure(FailureReason.INVALID_IP_DOMAIN);
	                }
	                String ip = ipDomain.substring(1, ipDomain.length() - 1);
	                Optional<String> validatedIp = ip.startsWith(IPV6_PREFIX)
	                    ? InternetProtocolAddress.validateIpv6(ip.substring(IPV6_PREFIX.length()))
	                    .map(s -> IPV6_PREFIX + s)
	                    : InternetProtocolAddress.validateIpv4(ip);
	                if (!validatedIp.isPresent()) {
	                    return EmailValidationResult.failure(FailureReason.INVALID_IP_DOMAIN);
	                }
	                context.currentDomainPart.append(validatedIp.get());
	                context.domain.append(validatedIp.get());
	                context.domainWithoutComments.append(validatedIp.get());
	                context.isIpAddress = true;
	                break;
	            }
	            if (c == '.') {
	                if (context.currentDomainPart.length() > MAX_DOMAIN_PART_LENGTH) {
	                    return EmailValidationResult.failure(FailureReason.DOMAIN_PART_TOO_LONG);
	                }
	                if (context.currentDomainPart.charAt(0) == '-') {
	                    return EmailValidationResult.failure(FailureReason.DOMAIN_PART_STARTS_WITH_DASH);
	                }
	                if (context.currentDomainPart.charAt(context.currentDomainPart.length() - 1) == '-') {
	                    return EmailValidationResult.failure(FailureReason.DOMAIN_PART_ENDS_WITH_DASH);
	                }
	                context.domainParts.add(context.currentDomainPart.toString());
	                context.currentDomainPart = new StringBuilder();
	            } else {
	                if (!isWhitespace(c)) context.currentDomainPart.append(c);
	            }
	            context.domain.append(c);
	            context.domainWithoutComments.append(c);
	            context.firstDomainChar = false;
	        }
	
	        final boolean quotedWhitespace = isWhitespace(c) && context.inQuotes;
	        if (c == '"' && !context.previousBackslash) {
	            if (context.inQuotes) {
	                context.requireAtDotOrComment = true;
	                if (context.currentQuote.length() == 0) {
	                    context.removableQuotePair = false;
	                }
	                if (context.removableQuotePair) {
	                    context.localPartWithoutQuotes.append(context.currentQuote);
	                } else {
	                    context.localPartWithoutQuotes.append('"');
	                    context.localPartWithoutQuotes.append(context.currentQuote);
	                    context.localPartWithoutQuotes.append('"');
	                }
	            } else {
	                context.removableQuotePair = true;
	                context.currentQuote = new StringBuilder();
	            }
	            context.inQuotes = !context.inQuotes;
	        }
	
	        context.whitespace = isWhitespace(c) && !context.inQuotes && !context.previousBackslash;
	        if (context.whitespace) {
	            context.containsWhiteSpace = true;
	        }
	        if (!context.whitespace) {
	            context.previousDot = c == '.';
	        }
	        if (!quotedWhitespace) {
	            context.previousQuotedDot = c == '.';
	        }
	        if (quotedWhitespace) {
	            if (!context.previousQuotedDot && !context.previousBackslash) {
	                context.requireQuotedAtOrDot = true;
	            }
	        }
	        context.previousBackslash = (c == '\\' && !context.previousBackslash);
	    }
	    if (!context.atFound) return EmailValidationResult.failure(FailureReason.MISSING_AT_SYMBOL);
	    return null;
	}
	
	private static EmailValidationResult finalValidationChecks(EmailParsingContext context, String fullSourceRoute, List<String> sourceRoutes) {
	    int localPartLen = context.localPart.length() - context.localPartCommentLength;
	    if (localPartLen == 0) return EmailValidationResult.failure(FailureReason.LOCAL_PART_MISSING);
	    if (localPartLen > MAX_LOCAL_PART_LENGTH) return EmailValidationResult.failure(FailureReason.LOCAL_PART_TOO_LONG);
	
	    int domainLen = context.domain.length() - context.domainCommentLength;
	    if (domainLen == 0) return EmailValidationResult.failure(FailureReason.DOMAIN_MISSING);
	    if (domainLen > MAX_DOMAIN_LENGTH) return EmailValidationResult.failure(FailureReason.DOMAIN_TOO_LONG);
	
	    if (context.localPart.charAt(context.localPart.length() - 1) == '.') {
	        return EmailValidationResult.failure(FailureReason.LOCAL_PART_ENDS_WITH_DOT);
	    }
	
	    if (context.currentDomainPart.length() <= 0) {
	        return EmailValidationResult.failure(FailureReason.MISSING_TOP_LEVEL_DOMAIN);
	    }
	
	    if (context.currentDomainPart.length() > 63) {
	        return EmailValidationResult.failure(FailureReason.TOP_LEVEL_DOMAIN_TOO_LONG);
	    }
	
	    if (context.currentDomainPart.charAt(0) == '-') {
	        return EmailValidationResult.failure(FailureReason.DOMAIN_PART_STARTS_WITH_DASH);
	    }
	
	    if (context.currentDomainPart.toString().chars().allMatch(Character::isDigit)) {
	        return EmailValidationResult.failure(FailureReason.NUMERIC_TLD);
	    }
	
	    context.domainParts.add(context.currentDomainPart.toString());
	
	    if (!context.isIpAddress && !isValidIdn(context.domainWithoutComments.toString())) {
	        return EmailValidationResult.failure(FailureReason.INVALID_DOMAIN_CHARACTER);
	    }
	
	    Email parsed = new Email(
	        context.localPart.toString(), context.localPartWithoutComments.toString(),
	        context.localPartWithoutQuotes.toString(), context.domain.toString(), context.domainWithoutComments.toString(),
	        fullSourceRoute, null, context.domainParts, context.comments, sourceRoutes, context.isIpAddress,
	        context.containsWhiteSpace, context.isAscii);
	
	    return EmailValidationResult.success(parsed);
  }

  private static Optional<String> validateComment(String s) {
    if (s.length() < 2) return Optional.empty();

    StringBuilder builder = new StringBuilder(s.length());

    boolean previousBackslash = false;
    boolean foundClosingParenthesis = false;

    for (int i = 0, size = s.length(); i < size; i++) {
      char c = s.charAt(i);

      if (c == '(' && !previousBackslash && i != 0) {
        // comment within a comment??
        Optional<String> inner = validateComment(s.substring(i));

        if (!inner.isPresent()) return Optional.empty();

        i += inner.get().length() - 1;
        builder.append(inner.get());
        continue;
      }

      builder.append(c);

      if (c == ')' && !previousBackslash) {
        foundClosingParenthesis = true;
        break;
      }

      previousBackslash = c == '\\';
    }

    if (!foundClosingParenthesis) {
      return Optional.empty();
    }

    return Optional.of(builder.toString());
  }

  private static Optional<SourceRouteDetail> validateSourceRouting(String s) {
    boolean requireNewDomain = true;

    SourceRouteDetail detail = new SourceRouteDetail();
    StringBuilder sourceRoute = new StringBuilder();
    StringBuilder currentDomainPart = new StringBuilder();

    int i = 0;

    for (int size = s.length(); i < size; i++) {
      char c = s.charAt(i);

      // We need the @ character for a new domain
      if (requireNewDomain && c != '@') return Optional.empty();

      // We can't see the @ character unless we need it
      if (c == '@' && !requireNewDomain) return Optional.empty();

      // A . , : means we should validate the current domain part
      if (c == '.' || c == ',' || c == ':') {
        // Cannot be empty or more than 63 chars
        if (currentDomainPart.length() == 0 || currentDomainPart.length() > 63) {
          return Optional.empty();
        }

        // Cannot start or end with '-'
        if (currentDomainPart.charAt(0) == '-'
            || currentDomainPart.charAt(currentDomainPart.length() - 1) == '-') {
          return Optional.empty();
        }

        // TLD cannot be all numeric
        if ((c == ',' || c == ':')
            && currentDomainPart.toString().chars().allMatch(Character::isDigit)) {
          return Optional.empty();
        }

        currentDomainPart = new StringBuilder();
      } else {
        if (c != '@') currentDomainPart.append(c);
      }

      // A comma is the end of the current domain route
      requireNewDomain = c == ',';

      detail.fullRoute.append(c);

      if (c == ',' || c == ':') {
        String route = sourceRoute.toString();

        if (!isValidIdn(route)) return Optional.empty();

        detail.routes.add(route);

        sourceRoute = new StringBuilder();
      } else if (c != '@') {
        sourceRoute.append(c);
      }

      if (c == ':') break;
    }

    // If we haven't seen the end of the current part, its invalid
    if (currentDomainPart.length() > 0) return Optional.empty();

    // If we needed a new domain (last saw a comma), fail
    if (requireNewDomain) return Optional.empty();

    return Optional.of(detail);
  }

  private static boolean isValidIdn(String test) {
    String domain;

    try {
      domain = IDN.toASCII(test, IDN.ALLOW_UNASSIGNED);
    } catch (IllegalArgumentException e) {
      // If IDN.toASCII fails, it's not valid
      return false;
    }

    for (int i = 0, size = domain.length(); i < size; i++) {
      char c = domain.charAt(i);

      if (!JMail.ALLOWED_DOMAIN_CHARACTERS.contains(c)) return false;
    }

    return true;
  }

  /**
   * Returns true if the given character is a whitespace character.
   *
   * @param c the character to check
   * @return true if whitespace, false otherwise
   */
  private static boolean isWhitespace(char c) {
    return (c == ' ' || c == '\n' || c == '\r');
  }

  private static final class SourceRouteDetail {
    private final StringBuilder fullRoute = new StringBuilder();
    private final List<String> routes = new ArrayList<>();
  }

  private static final String IPV6_PREFIX = "IPv6:";

  // Set of characters that are not allowed in the local-part outside of quotes
  private static final Set<Character> DISALLOWED_UNQUOTED_CHARACTERS = new HashSet<>(
      Arrays.asList('\t', '(', ')', ',', ':', ';', '<', '>', '@', '[', ']', '"',
          // Control characters 1-8, 11, 12, 14-31
          '␁', '␂', '␃', '␄', '␅', '␆', '␇', '␈', '␋', '␌', '␎', '␏', '␐', '␑',
          '␒', '␓', '␔', '␕', '␖', '␗', '␘', '␙', '␚', '␛', '␜', '␝', '␟', '␁'));

  // Set of characters that are allowed in the domain
  private static final Set<Character> ALLOWED_DOMAIN_CHARACTERS = new HashSet<>(
      Arrays.asList(
          // A - Z
          'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
          'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
          // a - z
          'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
          's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
          // 0 - 9
          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
          // Hyphen and dot (also allow whitespace between parts)
          '-', '.', ' '));

  // Set of characters within local-part quotes that require an escape
  private static final Set<Character> ALLOWED_QUOTED_WITH_ESCAPE = new HashSet<>(
      Arrays.asList('\r', '␀', '\n'));
}
