package za.co.tinyiko.transactionaggregation;

import org.springframework.boot.SpringApplication;

public class TestTransactionAggregationApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TransactionAggregationApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
