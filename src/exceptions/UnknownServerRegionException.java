package exceptions;

public class UnknownServerRegionException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public UnknownServerRegionException() {
    super("Region is unknown!");
  }

}
