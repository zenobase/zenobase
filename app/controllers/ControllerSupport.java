package controllers;

import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Sets;

import play.mvc.Controller;
import secure.User;

import common.Nodes;

public abstract class ControllerSupport extends Controller {

	private static final Set<String> USER_FILTER_FIELDS = Sets.newHashSet(User.PASSWORD.getName());

	protected static ObjectNode toJson(User user) {
		ObjectNode object = user.toJson();
		Nodes.filter(object, USER_FILTER_FIELDS);
		return object;
	}
}
