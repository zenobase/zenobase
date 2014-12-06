package com.zenobase.tasks.trackthisforme;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class TrackthisformeCategoriesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		TrackthisformeCategoriesResult result = new TrackthisformeCategoriesResult(readObject("TrackthisformeCategoriesResultTest.json"));

		Category c1 = result.getCategory("foo");
		assertThat(c1).isNull();

		Category c2 = result.getCategory("weight");
		assertThat(c2).isNotNull();
		assertThat(c2.getId()).isEqualTo("3531");
		assertThat(c2.getName()).isEqualTo("weight");
		assertThat(c2.getSymbol()).isEqualTo("kg");

		Category c3 = result.getCategory("Drink");
		assertThat(c3).isNotNull();
		assertThat(c3.getId()).isEqualTo("3532");
		assertThat(c3.getName()).isEqualTo("Drink");
		assertThat(c3.getSymbol()).isNull();
	}
}
