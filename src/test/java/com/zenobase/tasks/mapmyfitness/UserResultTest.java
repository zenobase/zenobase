package com.zenobase.tasks.mapmyfitness;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.tasks.ResultTestSupport;
import org.junit.jupiter.api.Test;

public class UserResultTest extends ResultTestSupport {

	@Test
	public void test() {
		UserResult result = new UserResult(readObject("UserResultTest.json"));
		assertThat(result.getId()).isEqualTo("12345678");
		assertThat(result.isImperial()).isTrue();
	}
}
