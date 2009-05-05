package harvester.processor.util;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {
	public static String slurp (InputStream in) throws IOException {
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n, "UTF-8"));
	    }
	    return out.toString();
	}
}
