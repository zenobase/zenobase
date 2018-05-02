package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public abstract class UpdateCommandSupport extends Command {

	private static final TokenField OBJECT_ID = new TokenField("object");
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	protected UpdateCommandSupport(ObjectNode node) {
		super(node);
	}

	protected UpdateCommandSupport(Command.Type type, Identity principal, String objectId, ObjectNode from, ObjectNode to) {
		super(type, principal);
		setParameter(OBJECT_ID, objectId);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	protected abstract Command newInstance(Identity principal, String objectId, ObjectNode from, ObjectNode to);

	protected String getObjectId() {
		return getParameter(OBJECT_ID);
	}

	protected ObjectNode getFrom() {
		return getParameter(FROM);
	}

	protected ObjectNode getTo() {
		return getParameter(TO);
	}

	@Override
	public Command reverse(Identity principal) {
		return newInstance(principal, getObjectId(), getTo(), getFrom());
	}

	public static abstract class Builder {

		private final ObjectNode fromRoot = Nodes.newObject();
		private final ObjectNode toRoot = Nodes.newObject();
		private ObjectNode fromLeaf = fromRoot;
		private ObjectNode toLeaf = toRoot;

		public <V> Builder set(Field<V> field, V fromValue, V toValue) {
			if (!Objects.equal(fromValue, toValue)) {
				fromLeaf.set(field.getName(), field.toJson(fromValue));
				toLeaf.set(field.getName(), field.toJson(toValue));
			}
			return this;
		}

		public <V> Builder with(Field<V> field) {
			fromLeaf = fromLeaf.with(field.getName());
			toLeaf = toLeaf.with(field.getName());
			return this;
		}

		protected ObjectNode getFrom() {
			return fromRoot;
		}

		protected ObjectNode getTo() {
			return toRoot;
		}

		public abstract Command build();
	}
}
