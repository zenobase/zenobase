package com.zenobase.commands;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.json.DomainNode;
import com.zenobase.json.Field;
import com.zenobase.json.JsonField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public abstract class UpdateCommandSupport extends Command {

	private static final TokenField OBJECT_ID = new TokenField("object");
	private static final ChangeField CHANGES = new ChangeField("changes");

	protected UpdateCommandSupport(ObjectNode node) {
		super(node);
	}

	protected UpdateCommandSupport(Command.Type type, Identity principal) {
		super(type, principal);
	}

	protected UpdateCommandSupport(Command.Type type, Identity principal, String taskId, Iterable<Change> patches) {
		super(type, principal);
		setParameter(OBJECT_ID, taskId);
		addParameters(CHANGES, patches);
	}

	protected String getObjectId() {
		return getParameter(OBJECT_ID);
	}

	protected ImmutableList<Change> getChanges() {
		return getParameters(CHANGES);
	}

	@Override
	public Command reverse(Identity principal) {
		return new UpdateTaskCommand(principal, getObjectId(), Iterables.transform(Lists.reverse(getChanges()), new Function<Change, Change>() {
			@Override
			public Change apply(Change change) {
				return change.reverse();
			}
		}));
	}

	public static abstract class Builder<T extends UpdateCommandSupport> {

		private final List<Change> patches = Lists.newArrayList();

		public <V> Builder<T> set(Field<V> field, V fromValue, V toValue) {
			if (!Objects.equal(fromValue, toValue)) {
				JsonNode from = field.toJson(fromValue);
				JsonNode to = field.toJson(toValue);
				patches.add(new Change(field.getName(), from, to));
			}
			return this;
		}

		protected List<Change> getPatches() {
			return patches;
		}

		public abstract T build();
	}

	protected static class Change extends DomainNode {

		private static final TokenField FIELD = new TokenField("field");
		private static final JsonField FROM = new JsonField("from");
		private static final JsonField TO = new JsonField("to");

		public Change(ObjectNode node) {
			super(node);
		}

		public Change(String field, JsonNode from, JsonNode to) {
			setValue(FIELD, field);
			setValue(FROM, from);
			setValue(TO, to);
		}

		public String getField() {
			return getValue(FIELD);
		}

		public JsonNode getFrom() {
			return getValue(FROM);
		}

		public JsonNode getTo() {
			return getValue(TO);
		}

		public void apply(ObjectNode node) {
			String field = getField();
			JsonNode from = getFrom();
			JsonNode to = getTo();
			JsonNode current = node.path(field);
			Preconditions.checkState(current.equals(from), "Expected %s but got %s", from, current);
			if (!to.isNull()) {
				node.put(field, to);
			} else {
				node.remove(field);
			}
		}

		public Change reverse() {
			return new Change(getField(), getTo(), getFrom());
		}
	}

	private static class ChangeField extends Field<Change> {

		public ChangeField(String name) {
			super(name, Change.class, "object");
		}

		@Override
		protected Change getValue(JsonNode node) {
			return new Change((ObjectNode) node);
		}

		@Override
		public JsonNode toJson(Change value) {
			Preconditions.checkNotNull(value);
			return value.toJson();
		}
	}
}
