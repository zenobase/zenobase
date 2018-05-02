package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class SignUpFormTest {

	@Test
	public void test() {
		test("tester", "secret123", "jdoe@zenobase.com", true);
	}

	@Test
	public void testUsernameIsTooShort() {
		test("me", "secret123", "jdoe@zenobase.com", false);
	}

	@Test
	public void testUsernameIsTooLong() {
		test("me", "secretsecretsecretsecret", "jdoe@zenobase.com", false);
	}

	@Test
	public void testUsernameHasForbiddenCharacters() {
		test("Tester", "secret123", "jdoe@zenobase.com", false);
	}

	@Test
	public void testUsernameIsReserved() {
		test("guest", "secret123", "jdoe@zenobase.com", false);
	}

	@Test
	public void testUsernameIsMissing() {
		test("", "secret123", "jdoe@zenobase.com", false);
	}

	@Test
	public void testPasswordIsMissing() {
		test("tester", "", "jdoe@zenobase.com", false);
	}

	@Test
	public void testPasswordIsTooShort() {
		test("tester", "secret", "jdoe@zenobase.com", false);
	}

	@Test
	public void testEmailDoesntParse() {
		test("tester", "secret123", "@zenobase", false);
	}

	@Test
	public void testEmailHasNoDomain() {
		test("tester", "secret123", "jdoe", false);
	}

	private void test(String username, String password, String email, boolean valid) {
		SignUpForm form = new SignUpForm(username, password, email);
		assertThat(form.valid()).as("valid").isEqualTo(valid);
	}
}
