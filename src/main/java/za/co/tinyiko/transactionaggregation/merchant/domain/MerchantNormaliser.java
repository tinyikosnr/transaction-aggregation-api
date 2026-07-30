package za.co.tinyiko.transactionaggregation.merchant.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Derives a merchant's canonical matching name from raw input text (TDS 38): trim, uppercase,
 * strip punctuation, collapse repeated whitespace, trim again.
 *
 * <p>A separate class, not folded into {@link Merchant#register}, because it is a distinct,
 * independently-testable text algorithm rather than an aggregate invariant — matching TDS 64,
 * which names it as its own class.
 *
 * <p>Stripping punctuation before collapsing whitespace (rather than TDS's literal step order)
 * is deliberate: it correctly handles punctuation padded by spaces on both sides (e.g.
 * {@code "A - B"}) without leaving a double space behind. Verified against TDS's own worked
 * example: {@code "  Checkers #104 Centurion "} normalises to {@code "CHECKERS 104 CENTURION"}.
 *
 * <p>This produces only the matching form. The display value is preserved separately by the
 * caller — see {@link za.co.tinyiko.transactionaggregation.merchant.application.MerchantResolutionService}
 * — trimmed only, never case- or punctuation-altered.
 */
public final class MerchantNormaliser {

	private static final Pattern NON_ALPHANUMERIC_OR_WHITESPACE = Pattern.compile("[^A-Z0-9\\s]");
	private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

	private MerchantNormaliser() {
	}

	public static String normalise(String rawName) {
		String upperTrimmed = rawName.trim().toUpperCase(Locale.ROOT);
		String withoutPunctuation = NON_ALPHANUMERIC_OR_WHITESPACE.matcher(upperTrimmed).replaceAll("");
		String withCollapsedWhitespace = REPEATED_WHITESPACE.matcher(withoutPunctuation).replaceAll(" ");
		return withCollapsedWhitespace.trim();
	}

}
