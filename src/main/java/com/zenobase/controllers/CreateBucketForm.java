package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.DomainNode;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class CreateBucketForm extends DomainNode {

	public CreateBucketForm(ObjectNode node) {
		super(node);
	}

	CreateBucketForm(String label, String description, Iterable<Alias> aliases) {
		setValue(Bucket.LABEL, label);
		setValue(Bucket.DESCRIPTION, description);
		setValues(Bucket.ALIASES, aliases);
	}

	public boolean hasId() {
		return contains(Bucket.ID);
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(Bucket.ID));
	}

	public @Nullable String getLabel() {
		return getValue(Bucket.LABEL);
	}

	public @Nullable String getDescription() {
		return getValue(Bucket.DESCRIPTION);
	}

	public Iterable<ObjectNode> getWidgets() {
		return getValues(Bucket.WIDGETS);
	}

	public List<Alias> getIncluded() {
		return getValues(Bucket.ALIASES);
	}
}
