package harvester.client.web;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.*;
import harvester.client.profileconfig.*;
import harvester.client.util.EscapeHTML;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class AjaxValidationController  implements Controller{

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
		
		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
			String type = request.getParameter("type");
			logger.info("processing an ajax validation request");
			
			for(Object key : request.getParameterMap().keySet())
				logger.info("key=" + key + " value=" + request.getParameter((String)key));
			
			boolean valid = true;
			
			if ("contributorname".equals(type)) {
				String oldname = request.getParameter("oldname");
				oldname = org.apache.commons.lang.StringEscapeUtils.unescapeXml(oldname);
				
				String collectionid = request.getParameter("collectionid");
				String name = request.getParameter("name");
				//logger.info("collectionid = " + collectionid  + " name = " + name);
				if(name != null && !name.equals(oldname))
					valid = daofactory.getContributorDAO().isNameInUse(name.trim(), collectionid);
				else valid = false;
				
			} else if ("collectionname".equals(type)) {
				String oldname = request.getParameter("oldname");
				String name = request.getParameter("name");
				if(name != null && !name.equals(oldname))
					valid = daofactory.getCollectionDAO().isNameInUse(name);
				else valid = false;
			} else if ("regex".equals(type)) {
				// we assume that the call only has one other parameter from type, and validate that
				Map pmap = new HashMap(request.getParameterMap());
				pmap.remove("type");
				String regex = null;
				for(Object key : pmap.keySet())
					regex = ((String[])pmap.get(key))[0];
				if(regex != null)
				{
					logger.info("regex is " + regex);
					try {
						Pattern p = Pattern.compile(regex);
						valid = false;
						logger.info("seems to be valid");
					}catch (PatternSyntaxException e) {
						logger.info("exception " + e.getMessage());
						valid = true;
					}
				}
				else { valid = false; }
			} else if ("xpath".equals(type)) {
				try {
					logger.info("attempting xpath validation");
					String xpath = null;
					Map pmap = new HashMap(request.getParameterMap());
					pmap.remove("type");
					for(Object key : pmap.keySet())
						xpath = ((String[])pmap.get(key))[0];
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpathobj = factory.newXPath();
					XPathExpression expr = xpathobj.compile(xpath);
					valid = false;
					logger.info("seems to be valid");
				} catch (Exception e) {
					logger.info("exception " + e.getMessage());
					valid = true;
				}
			}
			
			logger.info("returning " + !valid);
			
			response.setCharacterEncoding("utf-8");
			response.setContentType("application/json");
			response.setStatus(response.SC_OK);
			response.getOutputStream().print(!valid ? "true" : "false");
			return null;
		}
}
