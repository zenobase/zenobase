package com.zenobase.tasks.mapmyfitness;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class TypeResultTest extends ResultTestSupport {

	@Test
	public void test() {
		TypeResult result = new TypeResult(readObject("TypeResultTest.json"));
		assertThat(result.getName()).isEqualTo("Hike / Rock Climb");
	}
}
