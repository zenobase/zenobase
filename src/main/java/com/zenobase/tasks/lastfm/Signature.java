package com.zenobase.tasks.lastfm;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Signature {

	private final String secret;

	public Signature(String secret) {
		this.secret = secret;
	}

	public String sign(Map<String, String> params) {
		String hash = Hashing.md5().hashString(toString(params) + secret, StandardCharsets.UTF_8).toString();
		Preconditions.checkState(hash.length() == 32, "Expected 32 chars in hash but got: %s", hash);
		return hash;
	}

	private static String toString(Map<String, String> params) {
		List<String> sortable = new ArrayList<>();
		for (Map.Entry<String, String> param : params.entrySet()) {
			if (!"format".equals(param.getKey())) {
				sortable.add(param.getKey() + param.getValue());
			}
		}
		Collections.sort(sortable);
		return Joiner.on("").join(sortable);
	}
}
