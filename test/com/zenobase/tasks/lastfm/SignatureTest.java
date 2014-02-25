package com.zenobase.tasks.lastfm;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import com.google.common.collect.ImmutableMap;

public class SignatureTest {

	@Test
	public void test() {
		Map<String, String> params = ImmutableMap.<String, String>builder()
			.put("x", "foo").put("z", "baz").put("y", "bar").build();
		assertThat(new Signature("secret").sign(params)).isEqualTo("7561fa7b834a4b4498e4670f791e5134");
	}
}
