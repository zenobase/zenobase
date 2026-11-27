package com.zenobase.scripts;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.io.UserPrinter;
import com.zenobase.models.User;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

public class BulkMailTest {

	@Test
	public void testReadsUserExport() throws IOException {
		StringWriter out = new StringWriter();
		UserPrinter printer = new UserPrinter(out);
		printer.call(user("u1", "Ann \"A\", Jr\\", false, false));
		printer.call(user("u2", "Suspended", true, false));
		printer.call(user("u3", "Opted out", false, true));
		printer.flush();

		assertThat(BulkMail.readRecipients(new StringReader(out.toString()))).containsExactly(
			new BulkMail.Recipient("Ann \"A\", Jr\\", "u1@example.com")
		);
	}

	private static User user(String id, String name, boolean suspended, boolean optedOut) {
		User user = new User(id, name);
		user.setEmail(id + "@example.com");
		user.setVerified(true);
		user.setSuspended(suspended);
		user.setOptedOut(optedOut);
		return user;
	}
}
