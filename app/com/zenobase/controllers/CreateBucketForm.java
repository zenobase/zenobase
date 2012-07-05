package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.models.Bucket;

public class CreateBucketForm extends DomainNode {

	public CreateBucketForm(ObjectNode node) {
		super(node);
	}

	CreateBucketForm(String label, String description) {
		setValue(Bucket.LABEL, label);
		setValue(Bucket.DESCRIPTION, description);
	}

	public String getId() {
		return getValue(Bucket.ID);
	}

	public String getLabel() {
		return getValue(Bucket.LABEL);
	}

	public String getDescription() {
		return getValue(Bucket.DESCRIPTION);
	}
}
