package harvester.processor.task;

import harvester.data.Collection;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.util.HibernateUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

/**
 * Handles dispatching of tasks to the correct task pool corresponding to that task's collection.
 * This dispatcher is configurable for the number of threads each of its pools will have
 * the default is specified in the properties file as "defaultThreadcount", and any 
 * single pool can have the default override by placing collectionname.threadcount in
 * the properties file, where collectionname is of course the collection corresponding
 * to the pool's name.
 * We use a pool with poolid 0 as a 'left over' pool for ids that have yet to be configured
 * in the properties file.
 * 
 * @author adefazio
 *
 */
public class TaskDispatcher {

	private static int DEFAULT_POOL = 0;
	private static Logger logger = Logger.getLogger(TaskDispatcher.class);
	
	DAOFactory daofactory = DAOFactory.getDAOFactory();
	
	Map<Integer, TaskPool> pools = new HashMap<Integer, TaskPool>();
	
   	private ServletContext ctx;
   	/** properties loaded from HarvesterProcessor.properties */
   	private Properties props;
	
   	public TaskDispatcher(ServletContext ctx, Properties props) {
   		this.ctx = ctx;
   		this.props = props;
   		
   		//create the default pool
   		Integer numThreads = Integer.valueOf((String)props.get("defaultThreadcount"));
   		pools.put(DEFAULT_POOL, new TaskPool(numThreads, ctx, props));
   		
   	}
   	
	public void signalShutdown() {
		for(TaskPool pool : pools.values())
			pool.signalShutdown();
	}
	
	public void forceShutdown() {
		for(TaskPool pool : pools.values())
			pool.forceShutdown();
	}
	
	public int stopTask(int poolid, String harvesterid) {
		if(!pools.containsKey(poolid)) {
			logger.info("dispatching request to default pool");
			return pools.get(DEFAULT_POOL).stopTask(harvesterid);
		}
		
		logger.info("Dispatching stop task ... (currently running " + pools.size() + " pools)");
		
		return pools.get(poolid).stopTask(harvesterid);
	}
	
	public int startTask(HashMap<String, Object> params) {
		
		try {
			logger.info("Dispatching ... (currently running " + pools.size() + " pools)");
			
			//just route to the pool with the given collection id
			Integer contributorid = null;
			if(params.containsKey("contributorid"))
				contributorid = Integer.parseInt((String) params.get("contributorid"));
			
			if(contributorid == null)
				return 0;
	
			//get the collection id for that contributor
			HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
			Collection c = daofactory.getcontributorDAO().getContributor(contributorid).getCollection();
			int collectionid = c.getCollectionid();
			String collectionname = c.getName();
			HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
		
			logger.info("dispatching for collection " + collectionname + ", contributorid " + params.get("contributorid"));			
			
			if(!pools.containsKey(collectionid)) {			
				if(props.containsKey(collectionname + ".threadcount")) {
					//create a new pool	
					Integer numThreads = Integer.valueOf((String)props.get(collectionname + ".threadcount"));
					TaskPool pool = new TaskPool(numThreads,ctx, props);
					pools.put(collectionid, pool);
					return pool.startTask(params);
				} else {
					logger.info("using the default pool");
					return pools.get(DEFAULT_POOL).startTask(params);
				}
				
			} else {
				return pools.get(collectionid).startTask(params);
			}
				
		} catch (Exception e) {
			logger.error("ARGH!!! failed during dispatching: " + e.getMessage(), e);
			
			if(HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().isActive())
				HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().rollback();
			
			return 0;
		}
	}
	
	public int runningTasks() {
		int running = 0;
		for(TaskPool pool : pools.values()) {
			running += pool.numRunningTasks();
		}
		return running;
	}
	
	public void listQueue(Writer out) throws IOException {
		logger.info("printing queue list");

		out.append("<?xml version='1.0'?>\n");
		out.append("<queue>\n");
		
		for(Entry<Integer, TaskPool> entry : pools.entrySet()) {
			List<TaskProcessor> queued = entry.getValue().getQueuedTasks();
			int i = 0;
			
			for(TaskProcessor task : queued) {
				i++;
				out.append("<harvest position=\"" + i + 
						"\" profileid=\"" + task.getProfileid() + 
						"\" type=\"" + task.getType() + 
						"\" from=\"" + (task.getFrom() == null ? "" : task.getFrom()) + 
						"\" until=\"" + (task.getUntil() == null ? "" : task.getUntil()) + 
						"\" contributorid=\"" + task.getContributorid() + 
						"\" collectionid=\"" + entry.getKey() + "\" />\n");
			}
			
		}
		out.append("</queue>");
		out.close();
	}
	
}
