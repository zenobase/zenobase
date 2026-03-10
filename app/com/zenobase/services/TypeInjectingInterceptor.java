package com.zenobase.services;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.opensearch.client.HeapBufferedAsyncResponseConsumer;
import org.opensearch.client.RequestOptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Injects {@code "_type":"_doc"} into JSON responses from OpenSearch 2.x,
 * which removed the {@code _type} field. This restores compatibility with
 * the OpenSearch HLRC 1.3.x client that requires {@code _type} to be present.
 *
 * <p>Use {@link #OPTIONS} instead of {@link RequestOptions#DEFAULT} when making
 * HLRC calls against OpenSearch 2.x.
 */
public class TypeInjectingInterceptor extends HeapBufferedAsyncResponseConsumer {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final int BUFFER_LIMIT = 100 * 1024 * 1024;

	public static final RequestOptions OPTIONS;
	static {
		RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
		builder.setHttpAsyncResponseConsumerFactory(TypeInjectingInterceptor::new);
		OPTIONS = builder.build();
	}

	public TypeInjectingInterceptor() {
		super(BUFFER_LIMIT);
	}

	@Override
	protected HttpResponse buildResult(HttpContext context) throws Exception {
		HttpResponse response = super.buildResult(context);
		HttpEntity entity = response.getEntity();
		if (entity == null || entity.getContentType() == null
				|| !entity.getContentType().getValue().contains("json")) {
			return response;
		}
		try {
			byte[] content = EntityUtils.toByteArray(entity);
			JsonNode root = mapper.readTree(content);
			if (injectType(root)) {
				byte[] modified = mapper.writeValueAsBytes(root);
				response.setEntity(new ByteArrayEntity(modified,
					ContentType.parse(entity.getContentType().getValue())));
			} else {
				response.setEntity(new ByteArrayEntity(content,
					ContentType.parse(entity.getContentType().getValue())));
			}
		} catch (IOException e) {
			// Leave response unchanged if we can't parse it
		}
		return response;
	}

	private static boolean injectType(JsonNode node) {
		boolean modified = false;
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			if (obj.has("_index") && !obj.has("_type")) {
				obj.put("_type", "_doc");
				modified = true;
			}
			for (JsonNode child : obj) {
				modified |= injectType(child);
			}
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				modified |= injectType(child);
			}
		}
		return modified;
	}
}
