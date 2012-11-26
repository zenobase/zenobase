package com.zenobase.tasks;

import com.zenobase.common.Generator;
import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public abstract class Task extends DomainNode {

	private static final TokenField ID = new TokenField("@id", false);

	protected Task() {
		this(Generator.id());
	}

	protected Task(String id) {
		setValue(ID, id);
	}

	public String getId() {
		return getValue(ID);
	}
}
