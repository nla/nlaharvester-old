package harvester.processor.test.helpers;

import java.io.InputStream;
import java.net.*;
import java.util.*;
import javax.servlet.*;

/**
 * For use in unit tests. Procesing steps only use the get real path method of this, so thats all thats implemented.
 * Designed to be passed into the init method of a processing step.
 * @author adefazio
 *
 */
public class MockServletContext implements ServletContext {

	private String pathPrefix;
	
	
	public String getRealPath(String arg0) {
		return pathPrefix + arg0;
	}	
	
	/**
	 * This should be set for testing perposes with the path the main directory of the application.
	 * @param pathprefix getRealPath just does pathprefix + arg0, with this prefix
	 */
	public void setPathPrefix(String pathprefix) {
		pathPrefix = pathprefix;
	}
	
	
	
	
////////////////////////////////////////////////	
	
	
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public ServletContext getContext(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getInitParameter(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getInitParameterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getMimeType(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public URL getResource(String arg0) throws MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

	public InputStream getResourceAsStream(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set getResourcePaths(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServerInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public Servlet getServlet(String arg0) throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServletContextName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getServletNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getServlets() {
		// TODO Auto-generated method stub
		return null;
	}

	public void log(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void log(Exception arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void log(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

}
