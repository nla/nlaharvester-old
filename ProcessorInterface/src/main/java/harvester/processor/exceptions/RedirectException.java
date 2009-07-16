package harvester.processor.exceptions;

public class RedirectException extends Exception {
	private static final long serialVersionUID = 1144234L;
	String newurl;
	
	public RedirectException(String msg, String newurl) {
		super(msg);
		this.newurl = newurl;
	}

	public String getUrl() {
		return newurl;
	}
	
}
