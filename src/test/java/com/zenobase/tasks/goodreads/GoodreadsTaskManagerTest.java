package com.zenobase.tasks.goodreads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.Task.Status;
import com.zenobase.tasks.TaskManagerTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

/** Every send waits on the task manager's rate limiter (1/s), so keep the number of requests down. */
public class GoodreadsTaskManagerTest extends TaskManagerTestSupport {

	private static final String EMPTY_PAGE =
		"<GoodreadsResponse><reviews start=\"0\" end=\"0\" total=\"32\"/></GoodreadsResponse>";

	private final GoodreadsCredentialsManager credentialsManager = mock(GoodreadsCredentialsManager.class);
	private final GoodreadsTaskManager manager = new GoodreadsTaskManager(credentialsManager);
	private final OAuthCredentials credentials = mock(OAuthCredentials.class);

	@Test
	public void test() {
		Task task = newTask("2018-04-14T12:00:00Z");
		givenResponses(
			credentialsManager,
			credentials,
			xml(successResponse("auth_user.xml")),
			xml(successResponse("review_list.xml")),
			xml(response(200, EMPTY_PAGE))
		);

		Command commands = manager.execute(task, credentials);
		task = apply(commands, task);

		List<OAuthRequest> requests = sentRequests(credentialsManager);
		assertThat(requests).hasSize(3);
		assertThat(requests.get(0).getUrl()).isEqualTo("https://www.goodreads.com/api/auth_user");
		assertThat(requests.get(1).getUrl()).isEqualTo("https://www.goodreads.com/review/list.xml");
		assertThat(queryParam(requests.get(1), "id")).isEqualTo("63900829");
		assertThat(queryParam(requests.get(1), "shelf")).isEqualTo("read");
		assertThat(queryParam(requests.get(1), "page")).isEqualTo("1");
		assertThat(queryParam(requests.get(2), "page")).isEqualTo("2");

		assertThat(events(commands)).as("only the book finished after the marker").hasSize(1);
		assertThat(task.getStatus()).isEqualTo(Status.SUCCESS);
		assertThat(task.getMarker()).isEqualTo("2018-04-14T20:57:22.001-07:00");
		assertThat(task.getUndoId()).isEqualTo(commands.getId());
	}

	@Test
	public void testStopsAfterLastReview() {
		Task task = newTask(null);
		String review =
			"<review><id>%d</id><url>https://www.goodreads.com/review/show/%d</url>" +
			"<started_at>Sun Apr 01 20:57:22 -0700 2018</started_at>" +
			"<read_at>Sun Apr 08 20:57:22 -0700 2018</read_at></review>";
		String lastPage =
			"<GoodreadsResponse><reviews start=\"1\" end=\"2\" total=\"2\">" +
			review.formatted(1, 1) +
			review.formatted(2, 2) +
			"</reviews></GoodreadsResponse>";
		givenResponses(
			credentialsManager,
			credentials,
			xml(successResponse("auth_user.xml")),
			xml(response(200, lastPage))
		);

		Command commands = manager.execute(task, credentials);

		assertThat(sentRequests(credentialsManager)).as("no request for a second page").hasSize(2);
		assertThat(events(commands)).hasSize(2);
	}

	@Test
	public void testRejectsNonXmlResponse() {
		Task task = newTask(null);
		Response html = successResponse("auth_user.xml");
		when(html.getHeader("Content-Type")).thenReturn("text/html");
		givenResponses(credentialsManager, credentials, html);

		assertThatThrownBy(() -> manager.execute(task, credentials)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testRejectsMissingContentType() {
		Task task = newTask(null);
		givenResponses(credentialsManager, credentials, successResponse("auth_user.xml"));

		assertThatThrownBy(() -> manager.execute(task, credentials))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Expected application/xml but got <null>");
	}

	@Test
	public void parseXml() {
		assertThat(GoodreadsTaskManager.parseXml("<a><b>c</b></a>").getDocumentElement().getTagName()).isEqualTo("a");
	}

	@Test
	public void parseXmlRejectsDoctype() {
		String xml = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";
		assertThatThrownBy(() -> GoodreadsTaskManager.parseXml(xml)).isInstanceOf(RuntimeException.class);
	}

	private Task newTask(String marker) {
		return manager.newTask(
			Generator.id(),
			new Identity(),
			Nodes.newObject().put("tag", "Book").put("shelf", "read").put("marker", marker)
		);
	}

	private static Response xml(Response response) {
		when(response.getHeader("Content-Type")).thenReturn("application/xml; charset=utf-8");
		return response;
	}
}
