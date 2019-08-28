package com.zenobase.scripts;

import java.io.File;
import java.io.IOException;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Mailer {

	public static void main(String[] args) throws IOException {

		AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient();

		String from = "";
		String body = Files.toString(new File("template.txt"), Charsets.UTF_8);

		for (String line: Files.readLines(new File("records.csv"), Charsets.UTF_8)) {
			String[] fields = line.split("\t");
			String username = fields[1];
			String to = fields[2];
			System.err.println("Mailing " + to + "...");
			client.sendEmail(new SendEmailRequest()
				.withSource(from)
				.withDestination(new Destination().withToAddresses(String.format("%s <%s>", username, to)))
				.withMessage(new Message()
					.withSubject(new Content("Subject"))
					.withBody(new Body(new Content(String.format(body, username))))
				)
			);
		}
	}
}
