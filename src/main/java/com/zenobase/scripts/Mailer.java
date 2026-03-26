package com.zenobase.scripts;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

public class Mailer {

	public static void main(String[] args) throws IOException {

		SesClient client = SesClient.create();

		String from = "";
		String body = Files.toString(new File("template.txt"), Charsets.UTF_8);

		for (String line : Files.readLines(new File("records.csv"), Charsets.UTF_8)) {
			String[] fields = line.split("\t");
			String username = fields[1];
			String to = fields[2];
			System.err.println("Mailing " + to + "...");
			client.sendEmail(SendEmailRequest.builder()
					.source(from)
					.destination(Destination.builder()
							.toAddresses(String.format("%s <%s>", username, to))
							.build())
					.message(Message.builder()
							.subject(Content.builder().data("Subject").build())
							.body(Body.builder()
									.text(Content.builder()
											.data(String.format(body, username))
											.build())
									.build())
							.build())
					.build());
		}
	}
}
