package com.danielagapov.spawn.ServiceTests;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Order(3)
@Execution(ExecutionMode.CONCURRENT)
class SpawnApplicationTests {

	@Test
	void contextLoads() {
		// Placeholder test to prActivity errors during test suite execution.
		assertTrue(true);
	}
}
