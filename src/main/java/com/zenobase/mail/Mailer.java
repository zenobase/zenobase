package com.zenobase.mail;

import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.PropertiesBuilder;

public class Mailer {

	private static final Logger logger = LoggerFactory.getLogger(Mailer.class);

	private final InternetAddress from;
	private final Session session;

	@Inject
	public Mailer(
		@Named("mail.user") String user,
		@Named("mail.pass") String pass,
		@Named("mail.smtp.auth") String auth,
		@Named("mail.smtp.starttls.enable") String starttls,
		@Named("mail.smtp.host") String host,
		@Named("mail.smtp.port") String port,
		@Named("mail.from") String from) throws AddressException {

		this(user, pass, from, new PropertiesBuilder()
			.put("mail.smtp.auth", auth)
			.put("mail.smtp.starttls.enable", starttls)
			.put("mail.smtp.ssl.protocols", "TLSv1.2")
			.put("mail.smtp.host", host)
			.put("mail.smtp.port", port)
			.put("mail.user", user).build());
	}

	public Mailer(String username, String password, String from, Properties properties) throws AddressException {
		this.from = InternetAddress.parse(from, true)[0];
		this.session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}

	public void send(Message message) {
		logger.info("Sending message: {}", message.getSubject());
		try {
			MimeMessage mime = new MimeMessage(session);
			mime.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(message.getTo()));
			mime.setSubject(message.getSubject());
			mime.setFrom(from);
			mime.setText(message.getText());
			Transport.send(mime);
		} catch (MessagingException e) {
			logger.error("Couldn't send message: " + message.getSubject(), e);
		}
	}
}
