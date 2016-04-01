package protocol.managertoworker;

import java.util.UUID;

/**
 * Created by hagai_lvi on 01/04/2016.
 */
public class NewAnalyzeCommand implements ManagerToWorkerCommand {

	private UUID uuid;
	private String link;

	@Override
	public String toString() {
		return "NewAnalyzeCommand{" +
				"uuid=" + uuid +
				", link='" + link + '\'' +
				'}';
	}

	public NewAnalyzeCommand(UUID uuid, String link) {
		this.uuid = uuid;
		this.link = link;
	}

	@Override
	public void execute() {
		// TODO implement buy the workers
		System.out.println(toString());
	}
}
