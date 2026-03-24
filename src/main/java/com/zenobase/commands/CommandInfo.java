package com.zenobase.commands;

import com.zenobase.json.CommandTypeField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;

public class CommandInfo extends DomainNode {

	public static final TokenField ID = new TokenField("@id", false);
	public static final CommandTypeField TYPE = new CommandTypeField("@type");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField TIMESTAMP = new DateTimeField("timestamp");
	public static final TextField LABEL = new TextField("label");
	public static final IntegerField COST = new IntegerField("cost");

	public CommandInfo(Command command) {
		setValue(ID, command.getId());
		setValue(TYPE, command.getType());
		setValue(PRINCIPAL, command.getPrincipal());
		setValue(TIMESTAMP, command.getTimestamp());
		setValue(LABEL, command.toString());
		setValue(COST, command.getCost());
	}
}
