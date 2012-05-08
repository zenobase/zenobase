package com.zenobase.controllers;

import java.util.regex.Pattern;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Strings;

import com.zenobase.json.DomainNode;
import com.zenobase.models.Bucket;

public class CreateBucketForm extends DomainNode {

	private static final Pattern LABEL_PATTERN = Pattern.compile("[a-zA-Z0-9-_ ]{1,20}");

	public CreateBucketForm(ObjectNode node) {
		super(node);
	}

	CreateBucketForm(String label, String description) {
		setValue(Bucket.LABEL, label);
		setValue(Bucket.DESCRIPTION, description);
	}

	public String getLabel() {
		return getValue(Bucket.LABEL);
	}

	public String getDescription() {
		return getValue(Bucket.DESCRIPTION);
	}

	public boolean valid() {
		return !Strings.isNullOrEmpty(getLabel()) &&
			LABEL_PATTERN.matcher(getLabel().trim()).matches();
	}
}
