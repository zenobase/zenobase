package com.zenobase.tasks.dropbox;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;

public abstract class DropboxTaskManagerSupport extends OAuthTaskManager {

	protected DropboxTaskManagerSupport(String type, DropboxCredentialsManager credentialsManager) {
		super(type, credentialsManager);
	}

	protected ListFolderResult list(OAuthCredentials credentials, String path, String cursor) {
		OAuthRequest request;
		if (cursor != null) {
			request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/2/files/list_folder/continue");
			request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
			request.addPayload(Nodes.newObject("cursor", cursor).toString());
		} else {
			request = new OAuthRequest(Verb.POST, "https://api.dropbox.com/2/files/list_folder");
			request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
			request.addPayload(Nodes.newObject("path", path).toString());
		}
		Response response = send(request, credentials);
		return new ListFolderResult(parse(response));
	}

	protected ObjectNode download(OAuthCredentials credentials, String path) {
		var request = new OAuthRequest(Verb.POST, "https://content.dropboxapi.com/2/files/download");
		request.addHeader(HttpHeaders.CONTENT_TYPE, "");
		request.addHeader("Dropbox-API-Arg", Nodes.newObject("path", path).toString());
		Response response = send(request, credentials);
		return parseObject(response);
	}
}
