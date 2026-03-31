package com.zenobase.mail;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

public class SesMailer implements Mailer {

	private static final Logger logger = LoggerFactory.getLogger(SesMailer.class);

	private final SesV2Client client;
	private final String from;

	@Inject
	public SesMailer(SesV2Client client, @Named("mail.from") String from) {
		this.client = client;
		this.from = from;
	}

	@Override
	public void send(Message message) {
		logger.info("Sending message: {}", message.subject());
		try {
			client.sendEmail(SendEmailRequest.builder()
					.fromEmailAddress(from)
					.destination(Destination.builder().toAddresses(message.to()).build())
					.content(EmailContent.builder()
							.simple(software.amazon.awssdk.services.sesv2.model.Message.builder()
									.subject(Content.builder()
											.data(message.subject())
											.build())
									.body(Body.builder()
											.text(Content.builder()
													.data(message.text())
													.build())
											.build())
									.build())
							.build())
					.build());
		} catch (SesV2Exception e) {
			logger.error("Couldn't send message: {}", message.subject(), e);
		}
	}
}
