package harvester.client.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.WebRequest;

/**
 * Contains some helper methods for manipulating WebRequest and HttpServletRequest objects
 * @author adefazio
 *
 */
public class ControllerUtil {

    protected static final Log logger = LogFactory.getLog(ControllerUtil.class);
	
	/**
	 * The web request object is more complicated the we want most of the time
	 * since it support multiple posted parameters with the same name, whereas most
	 * of our pages don't use that functionality. So We basically just flatten it
	 * into a normal map.
	 * @param request 
	 * @return flatterned version of parameter map
	 */
	public static Map<String, String> ConvertRequestToMap(WebRequest request) {
		
		Map<String, String> requestMap = new HashMap<String, String>();
		for( Object param : request.getParameterMap().keySet())
			requestMap.put((String)param, request.getParameter((String)param));
		
		return requestMap;
	}
	
	@SuppressWarnings("unchecked")
	public static Map FileUploadRequestToMap(HttpServletRequest request) throws Exception {
		
		Map pmap = new HashMap();

		ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
		List fileItemsList = servletFileUpload.parseRequest(request);
		
		for(Object ritem : fileItemsList) {
			FileItem item = (FileItem)ritem;
			
			if( item.isFormField()) {
				logger.info(item.getFieldName() + " = " + item.getString());						
				pmap.put(item.getFieldName(), item.getString());
			} else {
				logger.info("FILE: " + item.getFieldName() + " size=" + item.getSize() + " name=" + item.getName());
				pmap.put(item.getFieldName(), WebUtil.slurp(item.getInputStream()));	//converts to a string		
				pmap.put(item.getFieldName() + "name", item.getName());
			}
		}		
		
		return pmap;
	}
}
