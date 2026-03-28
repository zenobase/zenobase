package com.zenobase.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.joda.time.DateTime;

import com.zenobase.tasks.Credentials;

public class DeleteCredentials extends ClientSupport {

	private final String type;
	private final DateTime olderThan;

	public DeleteCredentials(String type, DateTime olderThan) {
		this.type = type;
		this.olderThan = olderThan;
	}

	public static void main(String[] args) throws IOException {
		new DeleteCredentials("dash", DateTime.parse("2020-11-01T00:00:00Z")).run();
	}

	@Override
	protected void doRun() throws IOException {
		for (Credentials credentials : find("type:" + type)) {
			if (credentials.getCreated().isBefore(olderThan)) {
				delete(credentials.getId());
			}
		}
	}

	private List<Credentials> find(String query) throws IOException {
		List<Credentials> credentials = new ArrayList<>();
		for (int offset = 0, limit = 100; credentials.addAll(find(query, offset, limit)); offset += limit) {}
		System.out.format("Found %d credentials for <%s>\n", credentials.size(), query);
		return credentials;
	}

	private List<Credentials> find(String query, int offset, int limit) throws IOException {
		List<Credentials> credentials = new ArrayList<>();
		HttpGet request = new HttpGet(
				String.format("%s/credentials/?code=%s&q=%s&offset=%d&limit=%d", host, token, query, offset, limit));
		ClassicHttpResponse response = client.execute(request);
		for (JsonNode eventNode : readObject(response).path("items")) {
			credentials.add(new Credentials((ObjectNode) eventNode));
		}
		return credentials;
	}

	private void delete(String credentialsId) throws IOException {
		HttpDelete request = new HttpDelete(String.format("%s/credentials/%s?code=%s", host, credentialsId, token));
		ClassicHttpResponse response = client.execute(request);
		System.out.format("Deleted <%s>: %d\n", credentialsId, response.getCode());
	}
}
