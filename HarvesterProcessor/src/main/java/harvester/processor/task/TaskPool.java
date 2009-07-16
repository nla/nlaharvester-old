package harvester.processor.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

/**
 * Wraps a threadpool and provides the logic for creating a TaskProcessor object and adding it to the thread pool.
 * @author adefazio
 *
 */
public class TaskPool {

	private static Logger logger = Logger.getLogger(TaskPool.class);

	public static int MAX_QUEUE_SIZE = 300;
	
	/** I'm having issues accessing the context later when stored in the parent class, so store it here */
   	private ServletContext ctx;
   	/** properties loaded from HarvesterProcessor.properties */
   	private Properties props;
	
	 /** the threadpool controling execution of task processor threads */
   	private ThreadPoolExecutor threadPool;
	/**Table that a thread can check to see if it should stop execution. 
	 * key is string representing the harvesterid, value is the flags in taskProcessor*/
   	private Hashtable<String, Integer> stopFlags;
   	/** contributors for which there are currently running harvests
   	 * Note that we need to initialized this with a thread safe synchronized set for everything to work properly **/
   	private Set<Integer> runningContributors;
   	
   	public TaskPool(int numThreads, ServletContext ctx, Properties props) {

   		this.props = props;
   		this.ctx = ctx;
   		
  		stopFlags = new Hashtable<String, Integer>();
   		runningContributors = Collections.synchronizedSet(new HashSet<Integer>());
 		 
   		
 		logger.info("creating thread pool with " + numThreads + " threads");
    	threadPool = new ThreadPoolExecutor(numThreads, Integer.MAX_VALUE, 60*60,
    			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE));
 		logger.info("thread pool setup"); 		
   	}
	
   	public int numRunningTasks() {
   		return stopFlags.size();	//TODO: could be runningContributors instead?
   	}
   	
    public int startTask(HashMap<String, Object> params) {
		
		logger.info("----------------------------------------------queueing task---------------------------------");
		logger.info(" profileid=" + params.get("profileid"));
		logger.info(" contributorid=" + params.get("contributorid"));
		logger.info(" type=" + params.get("type"));
		logger.info(" retry=" + params.get("retry"));
		
		try {
			params.put("ctx", ctx);
			params.put("props", props);
			params.put("stopFlags", stopFlags);
			params.put("runningContributors", runningContributors);
			TaskProcessor tp = new TaskProcessor(params);
			logger.info("task processor object created, filling with values");
			
			//add to thread pool
			logger.info("adding task processor thread to thread pool queue");
			threadPool.execute(tp);
		
		} catch (NumberFormatException e) {
			logger.error("Error converting passed parameters to integers");
			logger.error(e.getMessage());
			return 0;
		} catch (Exception e) {
			logger.error("problem passing paramters to the task processor");
		}
		logger.info("added to queue");
		try {
			Thread.sleep(200);	//0.2 seconds
		} catch (Exception e) {
			logger.error("could not pause for 200ms!!!");	// its silly that thread.sleep can throw exceptions, there should be a flag to turn it off.
		}
		int queuesize = threadPool.getQueue().size();
		if(queuesize != 0) {
			logger.info("queue currenly contains " + queuesize + " elements");
			logger.info("current contents of queue(head first)");
			int i = 0;
			for(Iterator itor = threadPool.getQueue().iterator(); itor.hasNext(); i++)
				logger.info("position " + i + " : contributor id = " + ((TaskProcessor)itor.next()).getContributorid());
			logger.info("--");
		} else logger.info("queue is currently empty.");
		logger.info("--------------------------------------------------------------------------------------------");
		return 1;
	}	
   	
   	public List<TaskProcessor> getQueuedTasks() {
   		List<TaskProcessor> queued = new LinkedList<TaskProcessor>();
   		
		for(Iterator itor = threadPool.getQueue().iterator(); itor.hasNext();)
			queued.add((TaskProcessor)itor.next());
   		
   		return queued;
   	}
   	
	public int stopTask(String harvesterid) {
		
		logger.info("stoping task with harvesterid=" + harvesterid);
		
		//is task currently running?
		if(stopFlags.get(harvesterid) != null) {
			stopFlags.put(harvesterid, TaskProcessor.STOP_EXECUTION);	//tell it to stop next time it checks the table
			return 1;
		}
		else {
			logger.error("Task that they want us to stop doesn't exist");
			return 0;
		}
	}
	
	public void signalShutdown() {
		stopFlags.put("ALL", TaskProcessor.SERVER_SHUTTING_DOWN);
	}
	
	public void forceShutdown() {
		threadPool.shutdownNow();
	}
}
