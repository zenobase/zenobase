import javax.inject.Inject;

import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStop;
import services.NodeManager;

@OnApplicationStop
public class Shutdown extends Job<Void> {

	@Inject
	static NodeManager manager;

	@Override
	public void doJob() throws Exception {
		Logger.info("Closing node...");
		manager.close();
	}
}
