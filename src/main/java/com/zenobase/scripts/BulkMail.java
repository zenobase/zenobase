package com.zenobase.scripts;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

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

	@Parameters(
		index = "0",
		description = "Path to a CSV file with name, email, verified, suspended, and optedout columns"
	)
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
		try (var reader = new CSVReader(Files.newBufferedReader(recipientsFile, StandardCharsets.UTF_8))) {
			List<String[]> rows = reader.readAll();
			Preconditions.checkArgument(!rows.isEmpty(), "File is empty: %s", recipientsFile);
			String[] header = rows.getFirst();
			int nameIdx = columnIndex(header, "name");
			int emailIdx = columnIndex(header, "email");
			int verifiedIdx = columnIndex(header, "verified");
			int suspendedIdx = columnIndex(header, "suspended");
			int optedoutIdx = columnIndex(header, "optedout");
			return rows
				.stream()
				.skip(1)
				.filter(r -> "true".equals(r[verifiedIdx]))
				.filter(r -> !"true".equals(r[suspendedIdx]))
				.filter(r -> !"true".equals(r[optedoutIdx]))
				.map(r -> new Recipient(r[nameIdx], r[emailIdx]));
		}
	}

	private static int columnIndex(String[] header, String name) {
		for (int i = 0; i < header.length; ++i) {
			if (name.equals(header[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException("Missing column: " + name);
	}

	private BulkEmailEntry toEntry(Recipient recipient) {
		return BulkEmailEntry.builder()
			.destination(
				Destination.builder()
					.toAddresses(String.format("%s <%s>", recipient.username(), recipient.email()))
					.build()
			)
			.replacementEmailContent(
				ReplacementEmailContent.builder()
					.replacementTemplate(
						ReplacementTemplate.builder()
							.replacementTemplateData("{\"username\":\"" + recipient.username() + "\"}")
							.build()
					)
					.build()
			)
			.build();
	}

	private void createTemplate(SesV2Client client) {
		var content = EmailTemplateContent.builder().subject(TEMPLATE_SUBJECT).text(TEMPLATE_TEXT).build();
		try {
			System.out.printf("Creating template <%s>...%n", TEMPLATE_NAME);
			client.createEmailTemplate(
				CreateEmailTemplateRequest.builder().templateName(TEMPLATE_NAME).templateContent(content).build()
			);
		} catch (AlreadyExistsException e) {
			System.out.printf("Updating template <%s>...%n", TEMPLATE_NAME);
			client.updateEmailTemplate(
				UpdateEmailTemplateRequest.builder().templateName(TEMPLATE_NAME).templateContent(content).build()
			);
		}
	}

	private static final int BATCH_SIZE = 50;
	private static final Duration BATCH_DELAY = Duration.ofSeconds(4);

	private void sendEmails(SesV2Client client, List<BulkEmailEntry> entries) {
		System.out.printf("Sending bulk email to %d recipient(s)%n", entries.size());
		var content = BulkEmailContent.builder()
			.template(Template.builder().templateName(TEMPLATE_NAME).templateData("{\"username\":\"\"}").build())
			.build();
		for (int start = 0; start < entries.size(); start += BATCH_SIZE) {
			if (start > 0) {
				Uninterruptibles.sleepUninterruptibly(BATCH_DELAY);
			}
			var batch = entries.subList(start, Math.min(start + BATCH_SIZE, entries.size()));
			var response = client.sendBulkEmail(
				SendBulkEmailRequest.builder()
					.fromEmailAddress(FROM)
					.defaultContent(content)
					.bulkEmailEntries(batch)
					.build()
			);
			var results = response.bulkEmailEntryResults();
			for (int i = 0; i < results.size(); ++i) {
				System.out.printf("%s: %s%n", batch.get(i).destination().toAddresses(), results.get(i).status());
			}
		}
	}

	void main(String[] args) {
		System.exit(new CommandLine(new BulkMail()).execute(args));
	}
}
