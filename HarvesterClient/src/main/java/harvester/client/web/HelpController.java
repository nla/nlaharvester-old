package harvester.client.web;

import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * When the user is editing steps some fields are marked regex or xpath
 * These field have an extra link near them which pops up this page in a window
 * All this does is select from two choices of view and displays that.
 */
public class HelpController implements Controller
{
    protected final Log logger = LogFactory.getLog(getClass());

    private String regexUrl;
    private String xpathUrl;
    
    
	public void setRegexUrl(String regexUrl) {
		this.regexUrl = regexUrl;
	}
	public void setXpathUrl(String xpathUrl) {
		this.xpathUrl = xpathUrl;
	}


	public ModelAndView handleRequest(HttpServletRequest arg0, HttpServletResponse arg1) throws Exception 
	{
		logger.info(" help page invoked" );		
		String helptype = arg0.getParameter("type");
		String field = arg0.getParameter("field");
		logger.info("field=" + field);
		
		String view = null;
		if(helptype.equals("regex"))
			//view = "regexhelp";
			view = "redirect:" + regexUrl;
		else if(helptype.equals("xpath"))
			view = "redirect:" + xpathUrl;
			//view = "xpathhelp";
		else
			view = "xpathpicker";
		
		ModelAndView mv =  new ModelAndView(view);
		mv.addObject("field", field);
		
		return mv;
	}
}
