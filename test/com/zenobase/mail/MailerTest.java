package com.zenobase.mail;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Properties;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

public class MailerTest {

	@Test
	public void test() throws Exception {

		String from = "info@zenobase.com";
		Mailer mailer = new Mailer("username", "secret", from, new Properties());
		Message message = new Message("jdoe@zenobase.com", "Unit Test", "PASS");
		mailer.send(message);

		List<javax.mail.Message> inbox = Mailbox.get(message.getTo());
		assertThat(inbox).as("messages").hasSize(1);
		javax.mail.Message m = Iterables.getOnlyElement(inbox);
		assertThat(m.getFrom().length).as("number of senders").isEqualTo(1);
		assertThat(m.getFrom()[0].toString()).as("from").isEqualTo(from);
		assertThat(m.getSubject()).as("subject").isEqualTo(message.getSubject());
		assertThat(m.getContent()).as("content").isEqualTo(message.getText());
	}

	@After
	public void tearDown() {
		Mailbox.clearAll();
	}
}
