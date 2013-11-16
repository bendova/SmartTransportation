package util.protocols;

public class FSMException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 * @param cause
	 */
	FSMException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	FSMException(String message) {
		super(message);
	}

}

