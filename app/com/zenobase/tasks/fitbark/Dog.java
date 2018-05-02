package com.zenobase.tasks.fitbark;

import org.joda.time.DateTime;

public class Dog {

	private final String id;
	private final String name;
	private final DateTime created, modified;

	public Dog(String id, String name, DateTime created, DateTime modified) {
		this.id = id;
		this.name = name;
		this.created = created;
		this.modified = modified;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public DateTime getCreated() {
		return created;
	}

	public DateTime getModified() {
		return modified;
	}
}
