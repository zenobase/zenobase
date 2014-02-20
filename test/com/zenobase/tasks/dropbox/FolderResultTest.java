package com.zenobase.tasks.dropbox;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class FolderResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FolderResult result = new FolderResult(readObject("FolderResultTest.json"));
		assertThat(result.getFiles()).hasSize(1).contains("latest.txt");
	}
}
