package com.zenobase.mail;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import com.zenobase.testing.Integration;

@Integration
public class SesMailerTest {

	private SesV2Client client;

	@BeforeEach
	public void setUp() {
		try (var provider = DefaultCredentialsProvider.builder().build()) {
			provider.resolveCredentials();
		} catch (SdkClientException e) {
			Assumptions.abort("AWS credentials not available");
		}
		client = SesV2Client.create();
	}

	@Test
	public void test() {
		var from = "info@zenobase.com";
		var mailer = new SesMailer(client, from);
		mailer.send(new Message(from, "SesMailerTest", "PASS"));
	}
}
