package harvester.processor.util;

import org.mozilla.universalchardet.UniversalDetector;
import java.util.*;
import java.net.*;
import java.io.*;
import java.io.ByteArrayInputStream;

public class Encoder {
	
	public static String getEncoding(byte[] buf) {
		UniversalDetector detector = new UniversalDetector(null);
		detector.handleData(buf, 0, buf.length);
		detector.dataEnd();
		return detector.getDetectedCharset();
	}

	public static String getEncoding(String s) throws Exception {
		return getEncoding(new ByteArrayInputStream( s.getBytes()));
	}

	public static String getEncoding(InputStream in) throws Exception {
		String encoding = null;

		BufferedInputStream instream = new BufferedInputStream(in);
        
		UniversalDetector detector = new UniversalDetector(null);

		byte[] buf = new byte[1024];
		int len;
	    
		while( (len=instream.read(buf, 0, buf.length)) != -1) {
			detector.handleData(buf,0, len);                
		}
 		detector.dataEnd();
		encoding = detector.getDetectedCharset();
		return encoding;
	}
}
