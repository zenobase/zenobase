package controllers;

import java.util.Set;

import models.User;

import org.codehaus.jackson.node.ObjectNode;

import play.mvc.Controller;

import com.google.common.collect.Sets;
import common.Nodes;

public abstract class ControllerSupport extends Controller {

	private static final Set<String> USER_FILTER_FIELDS = Sets.newHashSet(User.PASSWORD.getName());

	protected static ObjectNode toJson(User user) { // TODO move to User
		ObjectNode node = user.toJson();
		Nodes.filter(node, USER_FILTER_FIELDS);
		return node;
	}
}
