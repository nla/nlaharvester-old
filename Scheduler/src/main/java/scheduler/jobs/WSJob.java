package scheduler.jobs;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;



/**
 * This class is designed to make generic calls to a WebService when activated by the scheduler.
 * The calls are always get requests, and nothing is done with the result. For this to be more 
 * useful outside of the nlaharvester, it would probably need to support put requests.
 * The arguments passed to the web service are just the parameters in the jobdetailmap, minus the
 * url passed in as "url". they are passed exactly as would be expected (key=value).
 * 
 * Exception handling:
 * Throws a JobExecutionException if it fails fatally. This could be caused by parameter parsing issues,
 * or by the url being invalid. The exception message should detail this.
 * 
 */
public class WSJob implements Job {

	private static Logger logger = Logger.getLogger(WSJob.class);
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		logger.info("WSJob called at: " +(new Date()));
		
		//should recieve a url parameter
		String url = ctx.getJobDetail().getJobDataMap().getString("url");
		if(url == null)
			throw new JobExecutionException("no url");
		ctx.getJobDetail().getJobDataMap().remove("url");
		ctx.getJobDetail().getJobDataMap().put("jobid", ctx.getJobDetail().getName());
		//params is going to be appended to the url when the request is sent
		StringBuilder params = new StringBuilder();
		//read the params one by one and add them to  parms
		try{
			for(String key : ctx.getJobDetail().getJobDataMap().getKeys())
			{
				logger.info("KEY=" + key + " |VALUE=" + ctx.getJobDetail().getJobDataMap().get(key));
				if(ctx.getJobDetail().getJobDataMap().get(key) != null)
					params.append(key + "=" + 
							URLEncoder.encode(ctx.getJobDetail().getJobDataMap().get(key).toString(), "UTF-8")
							+ "&");
				
			}	
			//there will be an extra & at the end, so we need to remove it
			params.deleteCharAt(params.length()-1);
		} catch(Exception e)
		{
			throw new JobExecutionException("error working with passed parameters: " + e.getMessage());
		}
		
		ctx.getJobDetail().getJobDataMap().put("url", url);
		
		//connect to the Web Service
		logger.info("URL| " + url + "?" + params.toString());
		
		try {
			URL requesturl = new URL(url + "?" + params.toString());
			HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
			conn.setRequestMethod("GET");
			conn.connect();
				logger.info("Response Code=" +  conn.getResponseCode());
			conn.disconnect();
			
		} catch (Exception e) {
			throw new JobExecutionException("unable to connect to Web Service: " + e.getMessage());
		}
		
		logger.info("web service request complete");
	}

	public void WSJob()
	{
		
	}
	
}
