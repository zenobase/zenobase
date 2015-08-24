package com.zenobase.tasks.moodpanda;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;

class MoodPandaUserResult extends XmlResultSupport {

	public MoodPandaUserResult(Document document) {
		super(document);
	}

	public String getUserId() {
		return Preconditions.checkNotNull(selectText("/UserList/User/UserID"), "Missing UserID");
	}

	public double getOffset() {
		String value = Preconditions.checkNotNull(selectText("/UserList/User/TimeZone"), "Missing TimeZone");
		Preconditions.checkArgument(value.startsWith("GMT +"), "Can't parse timezone offset: %s", value);
		return Double.parseDouble(value.substring(5));
	}

	public double getZone() {
		String value = Preconditions.checkNotNull(selectText("/UserList/User/TimeZone"), "Missing TimeZone");
		Preconditions.checkArgument(value.startsWith("GMT +"), "Can't parse timezone offset: %s", value);
		return Double.parseDouble(value.substring(5));
	}
}
