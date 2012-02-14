package common;

import org.elasticsearch.common.UUID;

public class Generator {

	public static String id() {
		return UUID.randomBase64UUID().replace('_', '-').substring(0, 8);
	}

	public static String bucketId() {
		return id().toLowerCase();
	}
}
