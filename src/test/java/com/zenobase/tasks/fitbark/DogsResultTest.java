package com.zenobase.tasks.fitbark;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import com.zenobase.tasks.ResultTestSupport;

public class DogsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		Dog dog = Iterables.getOnlyElement(new DogsResult(readObject("DogsResultTest.json")).getDogs());
		assertThat(dog.id()).isEqualTo("10a91cf6-ab2c-42f9-9e9a-1f3f7cab0532");
		assertThat(dog.name()).isEqualTo("Jessie");
		assertThat(dog.created().toString())
				.isEqualTo(dateTime("2015-08-28T12:03:04-07:00").toString());
		assertThat(dog.modified().toString())
				.isEqualTo(dateTime("2016-01-28T11:04:54-08:00").toString());
	}
}
