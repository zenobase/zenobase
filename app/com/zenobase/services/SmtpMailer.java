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

public class SmtpMailer implements Mailer {

	private final Session session;

	@Inject
	public SmtpMailer(
		@Named("mail.user") final String user,
		@Named("mail.pass") final String pass,
		@Named("mail.smtp.auth") String auth,
		@Named("mail.smtp.starttls.enable") String starttls,
		@Named("mail.smtp.host") String host,
		@Named("mail.smtp.port") String port) {

		Properties props = new Properties();
		props.put("mail.smtp.auth", auth);
		props.put("mail.smtp.starttls.enable", starttls);
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.user", user);
		session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, pass);
			}
		});
	}

	@Override
	// @Scheduled
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
