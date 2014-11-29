public class FileSystemException extends RuntimeException {
	public FileSystemException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileSystemException(String message) {
		super(message);
	}

	public FileSystemException() {
		super("A FileSystemException occurred. you should've provided a "+
					"error you sloppy developer.");
	}
}