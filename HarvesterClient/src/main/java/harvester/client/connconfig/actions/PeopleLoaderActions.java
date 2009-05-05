package harvester.client.connconfig.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import harvester.client.util.WebUtil;

import org.dom4j.*;


public class PeopleLoaderActions implements LoadStepActions {

    private static Log logger = LogFactory.getLog(ArrowLoaderActions.class);
    
	String identityurl;
	public String getIdentityurl() {
		return identityurl;
	}

	public void setIdentityurl(String identityurl) {
		this.identityurl = identityurl;
	}
	
	public void deleteProductionRecords(int contributorid, String contributorname) {
		PeopleLoaderRun run = new PeopleLoaderRun(contributorid);
		logger.info("starting new thread to run task");
		//we want this to run async, so we start a new thread to run the task on
		new Thread(run).start();
		logger.info("thread started, returning to normal execution");
	}

	public Integer getCollectionSize() {
		logger.info("getting collection size");
		logger.info(getIdentityurl());
		HttpClient httpclient = new HttpClient();

		String url = getIdentityurl() + "/record";
		GetMethod get = new GetMethod(url);
		try {
			httpclient.executeMethod(get);
		} catch (Exception e) {
			logger.info("failed to execute method", e);
			return null;
		}

		try {
			String response = WebUtil.slurp(get.getResponseBodyAsStream());	//assumes UTF-8
			//logger.info(response);
			Document doc = DocumentHelper.parseText(response);
			logger.info("done parsing response");
			
			Node node = doc.selectSingleNode("//report/@total");
			logger.info(node.getText());
			return Integer.parseInt(node.getText());
		}catch (Exception e) {
			logger.error("error getting response", e);
		}

		return null;
	}
	
	public class PeopleLoaderRun implements Runnable {
		private int contributorid;
		
		public PeopleLoaderRun(int contributorid) {
			this.contributorid = contributorid;
		}
		
		public void run() {
			logger.info("deleting all production records");
			HttpClient httpclient = new HttpClient();

			String url = getIdentityurl() + "/record/?contributor_id=" + Integer.toString(contributorid);
			logger.info("posting delete to url: " + url);
			DeleteMethod del = new DeleteMethod(url);
			try {
				httpclient.executeMethod(del);
			} catch (Exception e) {
				logger.error("failed to execute method", e);
			}

			try {
				String response = WebUtil.slurp(del.getResponseBodyAsStream());	//assumes UTF-8
				logger.info(response);
			}catch (Exception e) {
				logger.error("error getting response", e);
			}
		}
		
	}
	
	
}
