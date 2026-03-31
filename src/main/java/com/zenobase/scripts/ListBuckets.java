package com.zenobase.scripts;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "list-buckets")
public class ListBuckets extends ClientSupport {

	@Parameters(index = "0", description = "User ID")
	private String userId;

	@Override
	protected void doRun() throws IOException {
		var request = new HttpGet(String.format("%s/buckets/?q=roles.principal:%s&offset=0&limit=100", host, userId));
		int count = execute(request).path("buckets").size();
		System.out.format("Found %d buckets for <%s>\n", count, userId);
	}

	void main(String[] args) {
		System.exit(new CommandLine(new ListBuckets()).execute(args));
	}
}
