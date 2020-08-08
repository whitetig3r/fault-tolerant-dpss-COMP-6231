package exceptions;

public class BadUserNameException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BadUserNameException() {
    super("A Valid username should have between 6 and 15 characters");
  }

}
