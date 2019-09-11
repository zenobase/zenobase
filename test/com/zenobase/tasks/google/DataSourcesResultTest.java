package com.zenobase.tasks.google;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;

public class DataSourcesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		DataSourcesResult result = new DataSourcesResult(readObject("DataSourcesResultTest.json"));
		List<DataStream> streams = result.get();
		assertThat(streams).as("streams").hasSize(1);
		assertThat(streams.get(0)).isEqualTo(new DataStream("derived:com.google.step_count.delta", "com.google.step_count.delta", new Resource("Google Fit", "https://fit.google.com/")));
	}
}
