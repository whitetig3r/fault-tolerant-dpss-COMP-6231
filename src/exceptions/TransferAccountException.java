package exceptions;

public class TransferAccountException extends Exception {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public TransferAccountException() {
    super("There was an error encountered while transferring a player account!");
  }
}
