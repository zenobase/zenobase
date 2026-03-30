package com.zenobase.scripts;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

public class Mailer {

	public static void main(String[] args) throws IOException {

		SesV2Client client = SesV2Client.create();

		String from = "";
		String body = Files.toString(new File("template.txt"), Charsets.UTF_8);

		for (String line : Files.readLines(new File("records.csv"), Charsets.UTF_8)) {
			String[] fields = line.split("\t");
			String username = fields[1];
			String to = fields[2];
			System.err.println("Mailing " + to + "...");
			client.sendEmail(SendEmailRequest.builder()
					.fromEmailAddress(from)
					.destination(Destination.builder()
							.toAddresses(String.format("%s <%s>", username, to))
							.build())
					.content(EmailContent.builder()
							.simple(Message.builder()
									.subject(Content.builder().data("Subject").build())
									.body(Body.builder()
											.text(Content.builder()
													.data(String.format(body, username))
													.build())
											.build())
									.build())
							.build())
					.build());
		}
	}
}
