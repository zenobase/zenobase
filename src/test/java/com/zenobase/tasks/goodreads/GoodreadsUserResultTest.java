package com.zenobase.tasks.goodreads;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.zenobase.tasks.ResultTestSupport;

public class GoodreadsUserResultTest extends ResultTestSupport {

	@Test
	public void test() {
		GoodreadsUserResult result = new GoodreadsUserResult(readXml("auth_user.xml"));
		assertThat(result.getId()).isEqualTo("63900829");
	}
}
