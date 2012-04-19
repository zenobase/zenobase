package com.zenobase.commands;

import com.zenobase.models.DomainNode;
import com.zenobase.schema.DateTimeField;
import com.zenobase.schema.IdentityField;
import com.zenobase.schema.TextField;
import com.zenobase.schema.TokenField;

public class CommandInfo extends DomainNode {

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("@type", false);
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final TextField LABEL = new TextField("label");

	public CommandInfo(Command command) {
		setValue(ID, command.getId());
		setValue(TYPE, command.getType());
		setValue(PRINCIPAL, command.getPrincipal());
		setValue(TIMESTAMP, command.getTimestamp());
		setValue(LABEL, command.toString());
	}
}
