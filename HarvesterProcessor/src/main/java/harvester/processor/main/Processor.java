package harvester.processor.main;

import harvester.processor.task.TaskDispatcher;
import harvester.processor.task.TaskProcessor;
import harvester.processor.util.HibernateUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.*;
import java.util.Properties;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;


/**
 * Servlet implementation class for Servlet: Processor.
 * Controls the thread pool that task processors run in, starting and stopping running tasks
 * When it receives web requests notifying it to do so.
 */
 public class Processor extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
    
	static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(Processor.class);

    private TaskDispatcher dispatcher;
    
	/** I'm having issues accessing the context later when stored in the parent class, so store it here */
   	private ServletContext ctx;
   	/** properties loaded from HarvesterProcessor.properties */
   	private Properties props;
   
   	public void init(ServletConfig config) throws ServletException
   	{
   		
   		logger.info("init of Processor called");

 		 try {
	 		 ctx = config.getServletContext();
	 		 
	 		 InputStream is;
	 	     is = ctx.getResourceAsStream("WEB-INF/classes/HarvesterProcessor.properties");
	 	     if(is == null) {	//needed for jetty?
	 	    	is = new FileInputStream(ctx.getRealPath("WEB-INF/classes/HarvesterProcessor.properties"));
	 	     }
	 	     if(is == null) {	//use the debugging one instead
	 	    	is = new FileInputStream(System.getProperty("user.dir") + "/src/main/resources/HarvesterProcessor.properties");
	 	     }
	 	     
	 		 props = new Properties();
	 		 props.load(is);
	 		 is.close();
	 		 logger.info("properties retrieved!");
	 		 
	 		 dispatcher = new TaskDispatcher(ctx, props);
	 		 
 		 } catch (Exception e) {
 			 logger.error("Can't open properties file", e);
 			 logger.error("path: " + ctx.getRealPath("WEB-INF/classes/HarvesterProcessor.properties"));
 		 }
 		 
 		
   	}
   
	public Processor() {
		super();
	}   	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.info("Processor request recieved");
		
		String action = request.getParameter("action");	
		
		if(action == null) {
			logger.error("request didn't have a action field");
			response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if(action.equals("list")) {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/xml");
			dispatcher.listQueue(response.getWriter());
			response.setStatus( HttpServletResponse.SC_OK);
		}
		
		if(action.equals("start")) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			
			Set names = request.getParameterMap().keySet();
			for(Object name : names)
				params.put((String) name, request.getParameter((String)name));

			logger.info("parsed paramters");
			
			if( dispatcher.startTask(params) == 0) {
				response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
				logger.info("sending response for start request: " +  HttpServletResponse.SC_BAD_REQUEST);
			} else {
				response.setStatus( HttpServletResponse.SC_OK);
			}
		}
		
		if(action.equals("stop")) {
			String harvesterid = request.getParameter("harvestid");
			Integer collectionid = Integer.valueOf(request.getParameter("collectionid"));
			
			if(dispatcher.stopTask(collectionid, harvesterid) == 0) {	//has failed?
				response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
				logger.info("sending response for stop request: " +  HttpServletResponse.SC_BAD_REQUEST);
			} else
				response.setStatus( HttpServletResponse.SC_OK);
		}
				
	}  	

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// we don't support any sort of post request at the moment
	}  
	
	/**
	 * The recommended way to shutdown a thread nicely is to set a variable that that thread regularily checks to see if it has been 
	 * told to shutdown. we use the stopFlags map for this. If it doesn't shut down quickly, we send it an interrupt.
	 */
	@Override
	public void destroy()
	{
		int SHUTDOWN_WAIT = 30;
		try
		{
			logger.info("DESTROY CALLED");
			dispatcher.signalShutdown();
			
			logger.info("attempting nice shutdown");
			
			int count = 0;
			while(dispatcher.runningTasks() != 0 && count++ < SHUTDOWN_WAIT)
				Thread.sleep(100);	//0.1 seconds
			
			if(count == SHUTDOWN_WAIT)
			{
				logger.error("could not shutdown nicely");
			} else logger.info("shutdown nicely!");
			
			logger.info("shutting down thread pool now");
			dispatcher.forceShutdown();	//this interrupts the processes running on the threads.
			Thread.sleep(500);	//0.5 seconds
			
			logger.info("shutting down hibernate");
			HibernateUtil.getSessionFactory().close();
			logger.info("shutdown everything");
			
		} catch (Exception e)
		{
			logger.error("error incounted while trying to shut down threads");
		}
	}
	
}
