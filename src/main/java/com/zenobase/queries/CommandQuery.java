package com.zenobase.queries;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import com.zenobase.repositories.QuerySupport;
import com.zenobase.services.SearchOrder;

public class CommandQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(Command.TIMESTAMP.getName(), false);

	public CommandQuery principalEqualTo(Identity principal) {
		equalTo(Command.PRINCIPAL, principal.id());
		return this;
	}

	public CommandQuery queryString(String query) {
		super.queryString(query, Command.ID.getName());
		return this;
	}
}
