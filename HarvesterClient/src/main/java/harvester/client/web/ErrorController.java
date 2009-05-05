package harvester.client.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.*;
import org.apache.commons.logging.*;
/**
 * Any unhandled exception occuring during will result in this being invoked
 */
public class ErrorController implements HandlerExceptionResolver{
	
    protected final Log logger = LogFactory.getLog(getClass());

	public ModelAndView resolveException(HttpServletRequest req, HttpServletResponse res, Object o, Exception e) {

		long errorcode = (new Date()).getTime();

		ModelAndView mv = new ModelAndView("ErrorPage");
		
        mv.addObject("errormsg", e.getMessage());
        mv.addObject("errorcode", errorcode);
        
		logger.info("--------------------------------ERROR----------------------------------");
		logger.info(" Errorcode = " + errorcode,e);

		logger.info("returning error page");		
		return mv;
	}
}
