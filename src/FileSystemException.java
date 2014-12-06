/******************************************************************************
* FileSystemException.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/

// An exception class for sloppy developers.
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