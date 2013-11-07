package com.zenobase.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Field;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;

public class Migration {

	public static <T> void copy(Field<T> field, ObjectNode from, ObjectNode to) {
		JsonNode value = from.get(field.getName());
		if (value != null) {
			to.put(field.getName(), value);
		}
	}

	public static ObjectNode splitCredentials(ObjectNode taskNode) {
		String url = Credentials.AUTHORIZATION_URL.getValue(taskNode);
		ObjectNode config = Credentials.CREDENTIALS.getValue(taskNode);
		if (config == null && url == null) {
			return null;
		}
		Identity principal = Credentials.PRINCIPAL.getValue(taskNode);
		String type = Credentials.TYPE.getValue(taskNode).replaceAll("-.*", "");
		OAuthCredentials credentials = new OAuthCredentials(type, principal);
		copy(Credentials.CREATED, taskNode, credentials.toJson());
		credentials.setAuthorizationUrl(url);
		if (config != null) {
			if (config.get("userId") != null) {
				config.put("scope", config.get("userId").asText());
				config.remove("userId");
			}
		}
		Credentials.CREDENTIALS.setValue(credentials.toJson(), config);
		Credentials.AUTHORIZATION_URL.setValue(taskNode, null);
		Credentials.CREDENTIALS.setValue(taskNode, null);
		return credentials.toJson();
	}
}
