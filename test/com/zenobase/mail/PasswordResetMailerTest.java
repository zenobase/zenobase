package com.zenobase.mail;

import org.fest.assertions.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.zenobase.models.User;

public class PasswordResetMailerTest {

	@Test
	public void test() {
		Mailer mailer = Mockito.mock(Mailer.class);
		User user = new User("tester");
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret123");
		new PasswordResetMailer(mailer, "http://localhost").send(user);
		ArgumentCaptor<Message> arg = ArgumentCaptor.forClass(Message.class);
		Mockito.verify(mailer).send(arg.capture());
		Assertions.assertThat(arg.getValue().getTo()).isEqualTo(user.getEmail());
	}
}
