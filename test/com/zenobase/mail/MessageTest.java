package com.zenobase.mail;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class MessageTest {

	@Test
	public void test() {
		new EqualsTester()
			.addEqualityGroup(new Message("jdoe@zenobase.com", "Plan A", "hide"), new Message("jdoe@zenobase.com", "Plan A", "hide"))
			.addEqualityGroup(new Message("jdoe@zenobase.com", "Plan A", "run"))
			.addEqualityGroup(new Message("jdoe@zenobase.com", "Plan B", "run"))
			.addEqualityGroup(new Message("jdoe@zenobase.org", "Plan B", "run")).testEquals();
	}
}
