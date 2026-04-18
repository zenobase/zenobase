package com.zenobase.tasks.mapmyfitness;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.tasks.ResultTestSupport;
import org.junit.jupiter.api.Test;

public class TypeResultTest extends ResultTestSupport {

	@Test
	public void test() {
		TypeResult result = new TypeResult(readObject("TypeResultTest.json"));
		assertThat(result.getName()).isEqualTo("Hike / Rock Climb");
	}
}
