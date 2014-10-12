package ebay.exception;

public class UserDataException extends Exception{

	private String message;

	public UserDataException(String message)
	{
		this.message =message;
	}
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
