package com.zenobase.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;

public class CreateBucketForm extends DomainNode {

	public CreateBucketForm(ObjectNode node) {
		super(node);
	}

	CreateBucketForm(String label, String description, Iterable<Alias> aliases) {
		setValue(Bucket.LABEL, label);
		setValue(Bucket.DESCRIPTION, description);
		setValues(Bucket.ALIASES, aliases);
	}

	@Override
	public String getId() {
		return getValue(Bucket.ID);
	}

	public String getLabel() {
		return getValue(Bucket.LABEL);
	}

	public String getDescription() {
		return getValue(Bucket.DESCRIPTION);
	}

	public Iterable<ObjectNode> getWidgets() {
		return getValues(Bucket.WIDGETS);
	}

	public List<Alias> getIncluded() {
		return getValues(Bucket.ALIASES);
	}
}
