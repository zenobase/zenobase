package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class UndoForm extends DomainNode {

	private static final TokenField UNDO = new TokenField("undo");

	public UndoForm(String undoId) {
		setValue(UNDO, undoId);
	}

	public UndoForm(ObjectNode node) {
		super(node);
	}

	public String getCommandId() {
		return getValue(UNDO);
	}

	public boolean valid() {
		return !Strings.isNullOrEmpty(getCommandId());
	}
}
