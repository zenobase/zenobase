package com.zenobase.io;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.User;
import java.io.IOException;
import java.io.StringWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class UserPrinterTest {

	@Test
	public void test() throws IOException {
		User full = new User("u1", "Ann \"A\",\nJr\r\n\\ ü", new DateTime(2020, 1, 2, 3, 4, DateTimeZone.UTC));
		full.setEmail("ann@example.com");
		full.setExternalId("auth0|1");
		full.setVerified(true);
		full.setSuperuser(true);
		full.setQuota(100);

		StringWriter out = new StringWriter();
		UserPrinter printer = new UserPrinter(out);
		printer.call(full);
		printer.call(new User("u2", null, null));
		printer.flush();

		assertThat(out.toString()).isEqualTo(
			"\"id\",\"name\",\"email\",\"external_id\",\"verified\",\"suspended\",\"optedout\",\"superuser\",\"quota\",\"created\"\n" +
				"\"u1\",\"Ann \"\"A\"\",\nJr\r\n\\ ü\",\"ann@example.com\",\"auth0|1\",\"true\",\"false\",\"false\",\"true\",\"100\",\"2020-01-02T03:04:00.000Z\"\n" +
				"\"u2\",\"\",\"\",\"\",\"false\",\"false\",\"false\",\"false\",\"\",\"\"\n"
		);
	}
}
