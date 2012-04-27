package com.zenobase.services;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import play.Logger;

import com.zenobase.common.PropertiesBuilder;

public class Mailer {

	private final Session session;

	@Inject
	public Mailer(
		@Named("mail.user") final String user,
		@Named("mail.pass") final String pass,
		@Named("mail.smtp.auth") String auth,
		@Named("mail.smtp.starttls.enable") String starttls,
		@Named("mail.smtp.host") String host,
		@Named("mail.smtp.port") String port) {

		this(user, pass, new PropertiesBuilder()
			.put("mail.smtp.auth", auth)
			.put("mail.smtp.starttls.enable", starttls)
			.put("mail.smtp.host", host)
			.put("mail.smtp.port", port)
			.put("mail.user", user).build());
	}

	public Mailer(final String username, final String password, Properties properties) {
		session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}

	public void send(final Message message) {
		Logger.info("Sending message: " + message.getSubject());
		try {
			MimeMessage mime = new MimeMessage(session);
			mime.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(message.getTo()));
			mime.setSubject(message.getSubject());
			mime.setText(message.getText());
			Transport.send(mime);
		} catch (MessagingException e) {
			Logger.error("Couldn't send message: " + message.getSubject(), e);
		}
	}
}
