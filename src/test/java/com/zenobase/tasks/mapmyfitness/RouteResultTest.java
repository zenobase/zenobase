package com.zenobase.tasks.mapmyfitness;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class RouteResultTest extends ResultTestSupport {

	@Test
	public void test() {
		RouteResult result = new RouteResult(readObject("RouteResultTest.json"));
		assertThat(result.getLocation()).isEqualTo(new Location("47.62688383", "-122.36104538"));
	}
}
