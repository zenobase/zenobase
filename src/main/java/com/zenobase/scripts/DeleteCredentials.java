package com.zenobase.scripts;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.joda.time.DateTime;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "delete-credentials")
public class DeleteCredentials extends ClientSupport {

	@Parameters(index = "0", description = "Credential type (e.g. misfit)")
	private String type;

	@CommandLine.Option(names = "--before", description = "Delete credentials older than this date")
	private String before;

	@CommandLine.Option(names = "--commit", description = "Perform deletion, don't just preview")
	private boolean commit;

	@Override
	protected void doRun() throws IOException {
		var cutoff = before != null ? DateTime.parse(before) : DateTime.now();
		for (var credential : find("type:" + type)) {
			var created = DateTime.parse(credential.path("created").asText());
			if (created.isBefore(cutoff)) {
				delete(credential.path("@id").asText(), commit);
			}
		}
	}

	private List<JsonNode> find(String query) throws IOException {
		List<JsonNode> credentials = new ArrayList<>();
		for (int offset = 0, limit = 100; credentials.addAll(find(query, offset, limit)); offset += limit) {}
		System.out.format("Found %d credentials for <%s>\n", credentials.size(), query);
		return credentials;
	}

	private List<JsonNode> find(String query, int offset, int limit) throws IOException {
		List<JsonNode> credentials = new ArrayList<>();
		var request = new HttpGet(String.format("%s/credentials/?q=%s&offset=%d&limit=%d", host, query, offset, limit));
		for (var node : execute(request).path("items")) {
			credentials.add(node);
		}
		return credentials;
	}

	private void delete(String credentialsId, boolean commit) throws IOException {
		var request = new HttpDelete(String.format("%s/credentials/%s", host, credentialsId));
		int status = commit ? execute(request, ClassicHttpResponse::getCode) : -1;
		System.out.format("Deleting %s... %s\n", credentialsId, status != -1 ? status : "SKIP");
	}

	void main(String[] args) {
		System.exit(new CommandLine(new DeleteCredentials()).execute(args));
	}
}
