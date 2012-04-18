package com.zenobase.controllers;

import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Controller;

import com.google.common.collect.Sets;
import com.zenobase.common.Nodes;
import com.zenobase.models.User;

public abstract class ControllerSupport extends Controller {

	private static final Set<String> USER_FILTER_FIELDS = Sets.newHashSet(User.PASSWORD.getName());

	protected static ObjectNode toJson(User user) { // TODO move to User
		ObjectNode node = user.toJson();
		Nodes.filter(node, USER_FILTER_FIELDS);
		return node;
	}
}
