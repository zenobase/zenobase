package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import com.google.common.base.Objects;

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

		@Override
		public boolean equals(Object that) {
			return that instanceof Command.Type &&
				equals((Command.Type) that);
		}

		private boolean equals(Command.Type that) {
			return name.equals(that.getName()) &&
				version == that.getVersion();
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(name, version);
		}

		@Override
		public String toString() {
			return String.format("%s (%s)", name, version);
		}
	}
}
