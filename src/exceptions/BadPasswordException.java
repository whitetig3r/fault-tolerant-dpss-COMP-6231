package exceptions;

public class BadPasswordException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BadPasswordException() {
		super("A valid password must have a minimum of 6 characters!");
	}

}
