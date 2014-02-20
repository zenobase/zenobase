package com.zenobase.tasks.reporter;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

import com.zenobase.tasks.ResultTestSupport;

public class QuestionTest extends ResultTestSupport {

	@Test
	public void test() {
		Question q1 = new Question("How are you?", "mood", "rating");
		Question q2 = new Question("How are you?", "mood", null);
		Question q3 = new Question("What are you doing?", null, null);
		new EqualsTester()
			.addEqualityGroup(q1, new Question("How are you?", "mood", "rating"))
			.addEqualityGroup(q2)
			.addEqualityGroup(q3)
			.testEquals();
	}
}
