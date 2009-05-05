package scheduler;

import java.util.*;

import javax.annotation.PreDestroy;
import javax.servlet.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

/**
 * A webservice wrapper around the SchedulerService class.
 * Makes use of the jersey framework to simplify web service request handling
 *  
 *  While this scheduler has been built primarily to be used with the nlaharvester, it is sufficiently general to be used else where.
 *  It can basically be used to set up a webservice request to be fired at specific times specified by a cron string.
 *  
 */
@Path("/") 
@Singleton
public class SchedulerResource
{
	boolean loaded = false;
	
	private static Logger logger = Logger.getLogger(Scheduler.class);
	//private Scheduler s;
	private SchedulerService service;
	@Context ServletContext ctx;
	
	public SchedulerResource()
	{
	}


	private void load() throws Exception {
	   
		try
		{
			if(ctx == null)
				System.out.println("ctx is null");
			
			StdSchedulerFactory factory = (StdSchedulerFactory) ctx.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
			
			Scheduler s = factory.getScheduler();
			logger.info("NAME: " + s.getSchedulerName());
			logger.info("Scheduler got successfully");	
			s.start();
			
			service = new SchedulerService(s);
			
			loaded = true;
			
		} catch( Exception e)
		{
			logger.fatal("path=" + ctx.getRealPath("WEB-INF/classes/quartz.properties"));
			logger.fatal("CANT GET SCHEDULER!!!", e);
			throw e;
		}	
		logger.info("finished loading scheduler");	
   }
   
   @GET
   @Path("{jobid}")
   @javax.ws.rs.Produces("text/xml")  
   /**  The get schedule/s command returns a custom built xml document that holds all information about the asked for job. The format is 
    *  best gleaned by running the command, since it is of basically the same form for all jobs. Besides the information 
    *  provided in the modify schedule request, it also shows the last and next run times.
    */
   public String getSchedule(@PathParam("jobid") String jobid) throws NotFoundException{
	   String result = null;
	   try {
		   if(!loaded) load();
	   
		   logger.info("getSchedule Called");
		   List<String> jobids = new LinkedList<String>();
		   jobids.add(jobid);
		   result = service.getschedule(jobids);
		   
	   } catch (Exception e) {
		   throw new NotFoundException(e.getMessage());
	   }
	   
	   return result;
   }
 
   @GET
   @javax.ws.rs.Produces("text/xml")  
   public String getSchedules(@QueryParam("jobid") List<String> jobids) throws NotFoundException{
	   String result = null;
	   try {
		   if(!loaded) load();
	   
		   logger.info("getSchedules Called");
		   result = service.getschedule(jobids);
		   
	   } catch (Exception e) {		   
		   throw new NotFoundException(e.getMessage());
	   }
	   return result;
   }   
   
   @DELETE
   @Path("{jobid}")
   public void removeSchedule(@PathParam("jobid") String jobid) {
	   try {
		   if(!loaded) load();
	   
		   logger.info("removeSchedule Called");
		   service.removeSchedule(jobid);
		   
	   } catch (Exception e) {
		   throw new NotFoundException(e.getMessage());
	   }
   }
   
   @POST
   @Path("{jobid}")
   @javax.ws.rs.Consumes("application/x-www-form-urlencoded") 
   public void doSchedule(@PathParam("jobid") String jobid, MultivaluedMap<String,String> params ) {
	   try {
		   if(!loaded) load();
	   
		   logger.info("doSchedule Called");
		   service.doSchedule(jobid, params);
		   
	   } catch (Exception e) {
		   throw new NotFoundException(e.getMessage());
	   }
   }
   
   @PUT
   @Path("{jobid}")
   @javax.ws.rs.Consumes("text/xml")    
   public void modifySchedule(@PathParam("jobid") String jobid, String xml ) {
	   try {
		   if(!loaded) load();
	   
		   logger.info("modifySchedule Called");
		   service.modifySchedule(jobid, xml);
		   
	   } catch (Exception e) {
		   
	   }
   }
   
   
}