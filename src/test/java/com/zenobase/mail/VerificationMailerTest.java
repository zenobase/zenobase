package com.zenobase.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.zenobase.models.User;

public class VerificationMailerTest {

	@Test
	public void test() {
		Mailer mailer = mock(Mailer.class);
		User user = new User("tester");
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret123");
		new VerificationMailer(mailer, "http://localhost").send(user);
		ArgumentCaptor<Message> arg = ArgumentCaptor.forClass(Message.class);
		verify(mailer).send(arg.capture());
		assertThat(arg.getValue().getTo()).isEqualTo(user.getEmail());
	}
}
