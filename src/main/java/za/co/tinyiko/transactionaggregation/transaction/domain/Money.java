package za.co.tinyiko.transactionaggregation.transaction.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A monetary amount and its ISO 4217 currency code (SAD 28.1, TDS 16).
 *
 * <p>Normalises {@code amount} to scale 2 in the compact constructor, via
 * {@code setScale(2, RoundingMode.UNNECESSARY)} - not scale 4, despite the {@code transactions}
 * table storing {@code NUMERIC(19,4)} (SAD 27.2/29.3). Scale 2 is a genuine business fact (ZAR's
 * ISO 4217 minor-unit precision, TDS 16: "scale must not exceed two decimal places"); scale 4 is
 * an incidental fact about the column type SAD's DDL happens to specify, and tying a domain
 * value object's own validation to a storage detail like that is exactly the persistence-driven
 * design SAD 26.1 says to avoid. This also resolves a real correctness problem the scale-4
 * approach would have had: PostgreSQL always returns {@code NUMERIC(19,4)} values padded to
 * scale 4, so a {@code Money} reconstructed after a round trip through the database would fail
 * its own scale-4 check on data it had itself written, unless normalised back down first. Using
 * {@code UNNECESSARY} rather than a rounding mode means this both normalises (strips the
 * database's zero-padding back to scale 2, which always succeeds since the removed digits are
 * genuinely zero) and validates (rejects genuine sub-cent precision loss) in one operation.
 */
public record Money(BigDecimal amount, String currency) {

	private static final Pattern ISO_CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		if (!ISO_CURRENCY_PATTERN.matcher(currency).matches()) {
			throw new IllegalArgumentException("currency must be a three-letter uppercase ISO 4217 code");
		}
		try {
			amount = amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException("amount must not have more than two decimal places", e);
		}
	}

}
