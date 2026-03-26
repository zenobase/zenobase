package com.zenobase.tasks.lastfm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class SignatureTest {

	@Test
	public void test() {
		Map<String, String> params = ImmutableMap.<String, String>builder()
				.put("x", "foo")
				.put("z", "baz")
				.put("y", "bar")
				.build();
		assertThat(new Signature("secret").sign(params)).isEqualTo("7561fa7b834a4b4498e4670f791e5134");
	}
}
