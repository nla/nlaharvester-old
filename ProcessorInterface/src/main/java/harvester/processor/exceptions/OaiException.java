package harvester.processor.exceptions;

public class OaiException extends Exception {
	private static final long serialVersionUID = 1L;
	String code;
	public OaiException(String msg, String code) {
		super(msg);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
