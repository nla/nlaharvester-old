package harvester.processor.util;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.*;
import org.hibernate.stat.Statistics;

/** makes starting a session easier */
public class HibernateUtil {

	private static Logger logger = Logger.getLogger(HibernateUtil.class);
    private static final SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            //sessionFactory = new Configuration().configure().buildSessionFactory();
            sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed: " + ex);
            ex.printStackTrace();
            
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
	public static void logHibernateStats() {
		Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
		stats.setStatisticsEnabled(true);

		logger.debug("##############");
		logger.debug("Maximum amount of memory the VM will attemp to use = " + Runtime.getRuntime().maxMemory()/1024 + " KiloBytes");
		logger.debug("Current amount of free memory availible for future allocation: " + Runtime.getRuntime().freeMemory()/1024 + " Kilobytes");
		logger.debug("Total amount of memory in the JVM: " + Runtime.getRuntime().totalMemory()/1024 + " Kilobytes");
		logger.debug("Close Statement count: " + stats.getCloseStatementCount());
		
		logger.debug("Collection fetch count: " + stats.getCollectionFetchCount());
		logger.debug("Collection load count: " + stats.getCollectionLoadCount());
		logger.debug("Collection Recreate count: " + stats.getCollectionRecreateCount());
		logger.debug("Collection remove count: " + stats.getCollectionRemoveCount());
		logger.debug("Collection updated count: " + stats.getCollectionUpdateCount());
		
		logger.debug("Entity fetch count: " + stats.getEntityFetchCount());
		logger.debug("Entity load count: " + stats.getEntityLoadCount());
		logger.debug("Entity Insert count: " + stats.getEntityInsertCount());
		logger.debug("Entity delete count: " + stats.getEntityDeleteCount());
		logger.debug("Entity updated count: " + stats.getEntityUpdateCount());
		
		logger.debug("Connection count: " + stats.getConnectCount());
		logger.debug("Flush count: " + stats.getFlushCount());
		logger.debug("Queries Executed: " + stats.getQueryExecutionCount());
		logger.debug("Query max execution time: " + stats.getQueryExecutionMaxTime());
		logger.debug("Sessions openned: " + stats.getSessionOpenCount());
		logger.debug("Sessions closed: " + stats.getSessionCloseCount());
		logger.debug("Transactions started: " + stats.getTransactionCount());
		logger.debug("Transactions completed: " + stats.getSuccessfulTransactionCount());
		logger.debug("##############");
	}
}
