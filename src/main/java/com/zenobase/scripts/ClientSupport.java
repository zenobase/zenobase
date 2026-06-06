package com.zenobase.scripts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import picocli.CommandLine.Option;

public abstract class ClientSupport implements Callable<Integer> {

	private static final Path TOKEN_FILE = Path.of(System.getProperty("user.home"), ".zeno", "token");
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final CloseableHttpClient client = HttpClients.createDefault();

	@Option(names = "--host", defaultValue = "https://api.zenobase.com", description = "API host")
	protected String host;

	private String token;

	protected ObjectNode execute(ClassicHttpRequest request) throws IOException {
		return execute(
			request,
			response -> (ObjectNode) MAPPER.readTree(EntityUtils.toByteArray(response.getEntity()))
		);
	}

	protected <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<T> handler) throws IOException {
		request.setHeader("Authorization", "Bearer " + token);
		return client.execute(request, handler);
	}

	@Override
	public Integer call() throws Exception {
		if (Files.exists(TOKEN_FILE)) {
			token = Files.readString(TOKEN_FILE).strip();
		}
		if (token == null || token.isBlank()) {
			System.err.printf("Save your API token to %s%n", TOKEN_FILE);
			return 1;
		}
		doRun();
		return 0;
	}

	protected abstract void doRun() throws IOException;
}
