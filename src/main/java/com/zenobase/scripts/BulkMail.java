package com.zenobase.scripts;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.AlreadyExistsException;
import software.amazon.awssdk.services.sesv2.model.BulkEmailContent;
import software.amazon.awssdk.services.sesv2.model.BulkEmailEntry;
import software.amazon.awssdk.services.sesv2.model.CreateEmailTemplateRequest;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailTemplateContent;
import software.amazon.awssdk.services.sesv2.model.ReplacementEmailContent;
import software.amazon.awssdk.services.sesv2.model.ReplacementTemplate;
import software.amazon.awssdk.services.sesv2.model.SendBulkEmailRequest;
import software.amazon.awssdk.services.sesv2.model.Template;
import software.amazon.awssdk.services.sesv2.model.UpdateEmailTemplateRequest;

@CommandLine.Command(name = "bulk-mail")
public class BulkMail implements Callable<Integer> {

	private static final String FROM = "info@zenobase.com";
	private static final String TEMPLATE_NAME = "test";
	private static final String TEMPLATE_SUBJECT = "Test Message";
	private static final String TEMPLATE_TEXT = """
			Hello {{username}},

			This is a test message.

			Thanks!
			""";

	record Recipient(String username, String email) {
		Recipient {
			Preconditions.checkArgument(!username.isBlank(), "username is blank");
			Preconditions.checkArgument(!email.isBlank(), "email is blank");
		}
	}

	@Parameters(index = "0", description = "Path to a tab-delimited file containing usernames and email addresses")
	private Path recipientsFile;

	@Override
	public Integer call() throws Exception {
		var entries = readRecipients().map(this::toEntry).toList();
		try (var client = SesV2Client.create()) {
			createTemplate(client);
			sendEmails(client, entries);
		}
		return 0;
	}

	private Stream<Recipient> readRecipients() throws Exception {
		return Files.readAllLines(recipientsFile, StandardCharsets.UTF_8).stream()
				.filter(line -> !line.isBlank())
				.map(line -> {
					String[] fields = line.split("\t");
					Preconditions.checkArgument(fields.length == 2, "Cannot parse line: %s", line);
					return new Recipient(fields[0], fields[1]);
				});
	}

	private BulkEmailEntry toEntry(Recipient recipient) {
		return BulkEmailEntry.builder()
				.destination(Destination.builder()
						.toAddresses(String.format("%s <%s>", recipient.username(), recipient.email()))
						.build())
				.replacementEmailContent(ReplacementEmailContent.builder()
						.replacementTemplate(ReplacementTemplate.builder()
								.replacementTemplateData("{\"username\":\"" + recipient.username() + "\"}")
								.build())
						.build())
				.build();
	}

	private void createTemplate(SesV2Client client) {
		var content = EmailTemplateContent.builder()
				.subject(TEMPLATE_SUBJECT)
				.text(TEMPLATE_TEXT)
				.build();
		try {
			System.out.printf("Creating template <%s>...%n", TEMPLATE_NAME);
			client.createEmailTemplate(CreateEmailTemplateRequest.builder()
					.templateName(TEMPLATE_NAME)
					.templateContent(content)
					.build());
		} catch (AlreadyExistsException e) {
			System.out.printf("Updating template <%s>...%n", TEMPLATE_NAME);
			client.updateEmailTemplate(UpdateEmailTemplateRequest.builder()
					.templateName(TEMPLATE_NAME)
					.templateContent(content)
					.build());
		}
	}

	private void sendEmails(SesV2Client client, List<BulkEmailEntry> entries) {
		System.out.printf("Sending bulk email to %d recipient(s)%n...", entries.size());
		var response = client.sendBulkEmail(SendBulkEmailRequest.builder()
				.fromEmailAddress(FROM)
				.defaultContent(BulkEmailContent.builder()
						.template(Template.builder()
								.templateName(TEMPLATE_NAME)
								.templateData("{\"username\":\"\"}")
								.build())
						.build())
				.bulkEmailEntries(entries)
				.build());

		var results = response.bulkEmailEntryResults();
		for (int i = 0; i < results.size(); ++i) {
			System.out.printf(
					"%s: %s%n",
					entries.get(i).destination().toAddresses(), results.get(i).status());
		}
	}

	void main(String[] args) {
		System.exit(new CommandLine(new BulkMail()).execute(args));
	}
}
