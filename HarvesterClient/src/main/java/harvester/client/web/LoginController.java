package harvester.client.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.AuthenticationException;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.util.WebUtils;

// not used at the moment;
public class LoginController extends AbstractController {
	protected final Log logger = LogFactory.getLog(getClass());
	
//	public ModelAndView handleRequest(HttpServletRequest arg0, HttpServletResponse arg1) throws Exception {	
//		ModelAndView mv = new ModelAndView("Login");
//		return mv;
//	}

	@SuppressWarnings("unchecked")
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		//from http://forum.springframework.org/showthread.php?t=10917
		Map model = new HashMap();

		// Put the authentication exception on the model.
		AuthenticationException authenticationException = (AuthenticationException)WebUtils.getSessionAttribute(request, AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY);
		if (authenticationException != null) {
			logger.error("login error", authenticationException);
			logger.error("tostring: " + authenticationException.toString());
			logger.error("message: " + authenticationException.getMessage());
			model.put("acegiSecurityException", authenticationException);
		}
		
		return new ModelAndView("Login", model);
	}
		
}