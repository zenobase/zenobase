package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.models.Identity;

public interface Command {

	String getId();

	Command.Type getType();

	Identity getPrincipal();

	DateTime getTimestamp();

	Command reverse(Identity principal);

	ObjectNode toJson();

	public static class Type {

		private final String name;
		private final int version;

		public Type(String name, int version) {
			this.name = name;
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public int getVersion() {
			return version;
		}
	}
}
