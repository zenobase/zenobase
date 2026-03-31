package com.zenobase.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import com.zenobase.testing.Integration;

@Integration
public class SesEmailValidatorTest {

	private SesEmailValidator validator;

	@BeforeEach
	public void setUp() {
		try (var provider = DefaultCredentialsProvider.builder().build()) {
			provider.resolveCredentials();
		} catch (SdkClientException e) {
			Assumptions.abort("AWS credentials not available");
		}
		validator = new SesEmailValidator(SesV2Client.create());
	}

	@Test
	public void testValid() {
		assertThat(validator.isValid("jdoe@zenobase.com")).isTrue();
	}

	@Test
	public void testInvalid() {
		assertThat(validator.isValid("not-an-email")).isFalse();
	}
}
