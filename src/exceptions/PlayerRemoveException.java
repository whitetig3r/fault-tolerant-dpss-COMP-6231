package exceptions;

public class PlayerRemoveException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public PlayerRemoveException() {
    super("There was an error encountered while deleting a player account!");
  }
}
