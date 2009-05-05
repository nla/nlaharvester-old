package harvester.processor.email;

import harvester.data.Harvest;
import harvester.data.HarvestLog;
import harvester.processor.data.dao.DAOFactory;
import harvester.processor.util.HibernateUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;

public class LogFormatter {

	private static Logger logger = Logger.getLogger(LogFormatter.class);
	
	private int harvestid;
	private DateFormat df;
	private DateFormat tf;
	
	public LogFormatter(int harvestid) {
		this.harvestid = harvestid;
		df = DateFormat.getDateInstance(DateFormat.LONG);
		tf = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
	}
	
	public String formatDate(Date d) {
		return df.format(d);
	}
	public String formatDateTime(Date d) {
		return tf.format(d);
	}
	
	public String stripHtml(String str) {
		return str.replaceAll("\\<\\s*a.*?\\>", "").replaceAll("\\</a\\>", "");
	}
	
	public HashMap<String, Object> generateEmailReport() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		Session s = HibernateUtil.getSessionFactory().getCurrentSession();
		s.beginTransaction();
		try {
			//harvest object
			Harvest h = (Harvest)s.get(Harvest.class, harvestid);
			map.put("harvest", h);
			
			//Duration
			try {
				long duration = h.getEndtime().getTime() - h.getStarttime().getTime();	        
				map.put("duration", duration / 1000);
			} catch (Exception e) {
				map.put("duration", 0);
			}
			
			//get error report
			List<HarvestError> errors = DAOFactory.getDAOFactory().getHarvestlogDAO().getErrorSummary(harvestid);
			map.put("errorReport", errors);
			
			//get individual error logs and props
			List<HarvestLog> recordErrors = new LinkedList<HarvestLog>();
			//List<HarvestLog> props = new LinkedList<HarvestLog>();
			List<HarvestLog> harvestErrors = new LinkedList<HarvestLog>();
			List<KeyValue> props = new LinkedList<KeyValue>();
			
			for(HarvestLog hl : h.getHarvestlogs()) {
				if(hl.getErrorlevel() == HarvestLog.PROP_INFO) {
					try {
						String[] parts = hl.getDescription().split("=");
						props.add(new KeyValue(parts[0], parts[1]));					
					} catch (Exception e) {
						logger.info("problem parsing a prop info log message. desc=" + hl.getDescription());
					}
				}
				
				if(hl.getErrorlevel() == HarvestLog.RECORD_ERROR)
					recordErrors.add(hl);
				if(hl.getErrorlevel() == HarvestLog.STEP_ERROR)
					harvestErrors.add(hl);
			}
			
			map.put("recordErrors", recordErrors);
			map.put("harvestErrors", harvestErrors);
			map.put("harvestProperties", props);
			
		} catch (Exception e) {
			logger.error("Error generating report email", e);
			
		} finally {
			if(s.getTransaction() != null)
				s.getTransaction().commit();
		}
		
		return map;
	}
	
}
