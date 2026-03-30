package com.zenobase.mail;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.GetEmailAddressInsightsRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

public class SesEmailValidator implements EmailValidator {

	private static final Logger logger = LoggerFactory.getLogger(SesEmailValidator.class);

	private final SesV2Client client;

	@Inject
	public SesEmailValidator(SesV2Client client) {
		this.client = client;
	}

	@Override
	public boolean isValid(String email) {
		try {
			var response = client.getEmailAddressInsights(
					GetEmailAddressInsightsRequest.builder().emailAddress(email).build());
			return switch (response.mailboxValidation().isValid().confidenceVerdict()) {
				case HIGH, MEDIUM -> true;
				default -> false;
			};
		} catch (SesV2Exception e) {
			logger.error("Could not validate email", e);
			return false;
		}
	}
}
