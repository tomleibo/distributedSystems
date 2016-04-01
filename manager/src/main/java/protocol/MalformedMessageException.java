package protocol;

/**
 * Created by hagai_lvi on 30/03/2016.
 */
public class MalformedMessageException extends Exception {
	private final String message;

	public MalformedMessageException(String message) {
		this.message = message;
	}
}
