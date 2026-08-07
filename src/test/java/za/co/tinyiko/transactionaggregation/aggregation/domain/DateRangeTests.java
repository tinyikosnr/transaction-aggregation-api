package za.co.tinyiko.transactionaggregation.aggregation.domain;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DateRangeTests {

	@Test
	void rejectsNullFrom() {
		assertThatNullPointerException().isThrownBy(() -> new DateRange(null, LocalDate.of(2026, 1, 31)));
	}

	@Test
	void rejectsNullTo() {
		assertThatNullPointerException().isThrownBy(() -> new DateRange(LocalDate.of(2026, 1, 1), null));
	}

	@Test
	void rejectsFromAfterTo() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
	}

	@Test
	void acceptsFromEqualToTo() {
		LocalDate date = LocalDate.of(2026, 1, 1);

		DateRange dateRange = new DateRange(date, date);

		assertThat(dateRange.from()).isEqualTo(date);
		assertThat(dateRange.to()).isEqualTo(date);
	}

	@Test
	void acceptsASpanOfExactlyTwentyFourMonths() {
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = from.plusMonths(24);

		DateRange dateRange = new DateRange(from, to);

		assertThat(dateRange.to()).isEqualTo(to);
	}

	@Test
	void rejectsASpanExceedingTwentyFourMonths() {
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = from.plusMonths(24).plusDays(1);

		assertThatIllegalArgumentException().isThrownBy(() -> new DateRange(from, to));
	}

}
