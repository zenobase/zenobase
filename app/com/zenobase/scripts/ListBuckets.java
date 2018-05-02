package com.zenobase.scripts;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

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
		List<Bucket> buckets = Lists.newArrayList();
		HttpGet request = new HttpGet(String.format("%s/buckets/?q=roles.principal:%s&offset=0&limit=100", host, userId));
		request.addHeader("Authorization", "Bearer " + token);
		HttpResponse response = client.execute(request);
		ObjectNode node = Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
		for (JsonNode eventNode : node.path("buckets")) {
			buckets.add(new Bucket((ObjectNode) eventNode));
		}
		System.out.format("Found %d buckets for <%s>\n", buckets.size(), userId);
	}
}
