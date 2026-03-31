package com.zenobase.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "delete-tasks")
public class DeleteTasks extends ClientSupport {

	@Parameters(index = "0", description = "Task type (e.g. misfit-steps)")
	private String type;

	@CommandLine.Option(names = "--commit", description = "Perform deletion, don't just preview")
	private boolean commit;

	@Override
	protected void doRun() throws IOException {
		for (var task : find("type:" + type)) {
			delete(task.path("@id").asText(), commit);
		}
	}

	private List<JsonNode> find(String query) throws IOException {
		List<JsonNode> tasks = new ArrayList<>();
		for (int offset = 0, limit = 100; tasks.addAll(find(query, offset, limit)); offset += limit) {}
		System.out.format("Found %d tasks for <%s>\n", tasks.size(), query);
		return tasks;
	}

	private List<JsonNode> find(String query, int offset, int limit) throws IOException {
		List<JsonNode> tasks = new ArrayList<>();
		var request = new HttpGet(String.format("%s/tasks/?q=%s&offset=%d&limit=%d", host, query, offset, limit));
		for (var node : execute(request).path("tasks")) {
			tasks.add(node);
		}
		return tasks;
	}

	private void delete(String taskId, boolean commit) throws IOException {
		var request = new HttpDelete(String.format("%s/tasks/%s", host, taskId));
		int status = commit ? execute(request, ClassicHttpResponse::getCode) : -1;
		System.out.format("Deleting %s... %s\n", taskId, status != -1 ? status : "SKIP");
	}

	void main(String[] args) {
		System.exit(new CommandLine(new DeleteTasks()).execute(args));
	}
}
