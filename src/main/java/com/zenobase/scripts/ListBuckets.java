package com.zenobase.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;

public class ListBuckets extends ClientSupport {

	private final String userId;

	public ListBuckets(String userId) {
		this.userId = userId;
	}

	public static void main(String[] args) throws IOException {
		new ListBuckets("7ghkds6jar").run();
	}

	@Override
	protected void doRun() throws IOException {
		List<Bucket> buckets = new ArrayList<>();
		HttpGet request =
				new HttpGet(String.format("%s/buckets/?q=roles.principal:%s&offset=0&limit=100", host, userId));
		request.addHeader("Authorization", "Bearer " + token);
		ClassicHttpResponse response = client.execute(request);
		ObjectNode node = Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
		for (JsonNode eventNode : node.path("buckets")) {
			buckets.add(new Bucket((ObjectNode) eventNode));
		}
		System.out.format("Found %d buckets for <%s>\n", buckets.size(), userId);
	}
}
