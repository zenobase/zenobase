package com.zenobase.tasks.dropbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.zenobase.tasks.ResultTestSupport;

public class ListFolderResultTest extends ResultTestSupport {

	@Test
	public void test() {
		ListFolderResult result = new ListFolderResult(readObject("ListFolderResultTest.json"));
		assertThat(result.hasMore()).isTrue();
		assertThat(result.getCursor()).isEqualTo("ZtkX9_EHj3x7PMkVuFIhwKYXEpwpLwyxp9vMKomUhllil9q7eWiAu");
		assertThat(result.getFiles()).hasSize(1).contains("prime_numbers.txt");
	}
}
