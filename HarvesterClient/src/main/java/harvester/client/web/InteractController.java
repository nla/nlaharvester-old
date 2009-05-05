package harvester.client.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.connconfig.*;
import harvester.client.connconfig.actions.LoadStepActions;
import harvester.client.data.dao.DAOFactory;
import harvester.client.schedule.*;
import harvester.client.service.CollectionService;
import harvester.data.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class holds many mostly unrelated bits of code that are mostly called
 * by the user clicking various buttons. Any chunk of code that wasn't important
 * enough to get its own controller is in here.
 */
public class InteractController implements Controller {

    private String processorurl;

    private CollectionService collectionService;

    @Autowired
	public void setCollectionService(CollectionService collectionService) {
		this.collectionService = collectionService;
	}

	public String getProcessorurl() {
		return processorurl;
	}

	public void setProcessorurl(String processorurl) {
		this.processorurl = processorurl;
	}

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
	
    private SchedulerClient schedulerclient;

	public SchedulerClient getSchedulerclient() {
		return schedulerclient;
	}

	public void setSchedulerclient(SchedulerClient schedulerclient) {
		this.schedulerclient = schedulerclient;
	}
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
	
		logger.info("processing interact request");
		String url = null;

		Integer contributorid = null;
		//we always need contributor id, so get it before branching
		if(request.getParameter("contributorid") != null)
			contributorid = Integer.valueOf(request.getParameter("contributorid"));

		//branch based on action
		String action = request.getParameter("action");
		logger.info("action=" + action);
			
			
			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			if(action.equals("copyprofile"))
			{
				String profileid = request.getParameter("profileid");
				String copyover = request.getParameter("copyover");
				
				logger.info("copying over profile " + copyover + ", profileid of profile that is being copied=" + profileid);
				
				Contributor con = daofactory.getContributorDAO().getContributorCollectionAndDataprofile(contributorid);
				
				int oldprofileid = "test".equals(copyover) ? con.getTest().getProfileid() : con.getProduction().getProfileid();			
					
				//create new one
				Profile p = daofactory.getProfileDAO().copyProfile(Integer.valueOf(profileid), contributorid, "production".equals(copyover));
				
				//save the new profile under our contributor	
				if("test".equals(copyover))
					con.setTest(p);
				else
					con.setProduction(p);
				daofactory.getContributorDAO().updateContributor(con);
				
				//delete the old one
				daofactory.getProfileDAO().deleteProfile(oldprofileid);
				
				url= "ViewProcessingSteps.htm?contributorid=" + contributorid;
				
				
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			} else if(action.equals("stopharvest"))
			{
			
				//call the harvesterprocessor to tell it to stop the harvest
				//if it doesn't know about it, instead just change its status to STOPPED
				
				String harvestid = request.getParameter("harvestid");
				Harvest h = daofactory.getHarvestDAO().getHarvestContributorCollection(Integer.valueOf(harvestid));
				Integer collectionid = h.getContributor().getCollection().getCollectionid();
				
				String conurl = processorurl + "?action=stop&harvestid=" + harvestid + "&collectionid=" + collectionid;
				
				logger.info("url: " + conurl);
				
				logger.info("connecting to processor ws");
				URL requesturl = new URL(conurl.toString());
				HttpURLConnection conn = (HttpURLConnection) requesturl.openConnection();
				conn.setRequestMethod("GET");
				
				conn.connect();		
				int responsecode = conn.getResponseCode();
				logger.info("Response code:" + responsecode);
				conn.disconnect();
				
				//if the response code is HttpServletResponse.SC_BAD_REQUEST, it could not find it
				
				if(responsecode == HttpServletResponse.SC_BAD_REQUEST)
				{
					logger.info("harvest not found, changing status locally");
					daofactory.getHarvestDAO().doHardHarvestStop(Integer.valueOf(harvestid));
				}
				
				
				//url= "ListHarvestLogs.htm?contributorid=" + contributorid;
				url = "ViewHarvest.htm?harvestid=" + harvestid;
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			} else if(action.equals("getrecord"))
			{
				///Used in the log views so that the user can click a link to go straight to that record in the oai respository
				//we don't just use a direct link in the code since that would require that the processor's logging know more then
				//it really needs to.
				logger.info("get record called");
				
				Integer harvestid = Integer.valueOf(request.getParameter("harvestid"));
				
				Harvest h = daofactory.getHarvestDAO().getHarvestAndContributor(harvestid);
				Contributor con = daofactory.getContributorDAO().getContributorAndHarvestStepDetails(h.getContributor().getContributorid());
				
				Map<String, String> pmap = PUtil.parametersToMap(con);
				
				String baseurl = pmap.get("Base URL");
				String metadataprefix = pmap.get("Metadata Prefix");
				logger.info("gathered parameters for ws request");
				
				StringBuilder urlbuilder = new StringBuilder(); 
				urlbuilder.append(baseurl);
				urlbuilder.append("?verb=GetRecord&metadataPrefix=");
				urlbuilder.append(metadataprefix);
				urlbuilder.append("&identifier=");
				urlbuilder.append(request.getParameter("oaiid"));
				
				url = urlbuilder.toString();
				
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
			}
			else if(action.equals("removeharvest"))
			{
				/// In the harvest worktray the user has the ability to hide a harvest if they get sick of looking at it
				// clicking that buttton runs this code
				
				logger.info("setting harvest to hidden");
				daofactory.getContributorDAO().setHidden(contributorid);
				url="ListHarvests.htm?collectionid=" + request.getParameter("collectionid");
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("deletelocalrecords")) {
				String harvestid = request.getParameter("harvestid");
				logger.info("deleting records for harvestid " + harvestid + " contributorid " + contributorid + " from local store");
				daofactory.getHarvestDAO().deleterecords(Integer.valueOf(harvestid));
				logger.info("deletion complete");
				url="ListHarvestLogs.htm?contributorid=" + String.valueOf(contributorid);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("deleteproductionrecords"))
			{
				logger.info("deleting production records from local store");
				
				final int cf = contributorid;	// java doesn't have closures, so this has to be final
				
				new Thread(new Runnable() {
					public void run() { daofactory.getContributorDAO().doRemoveRecords(cf, 1); }
				}).start();
				
				Integer collectionid = Integer.valueOf(request.getParameter("collectionid"));
				
				collectionService.deleteProductionRecordsForContributor(collectionid, contributorid);				
				
				url="ViewContributor.htm?contributorid=" + String.valueOf(contributorid);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("deletetestrecords"))
			{
				///This only deletes from the local store of the records
				//its also currently not used
				logger.info("deleting test records");
				daofactory.getContributorDAO().doRemoveRecords(contributorid, 0);
				
				url="ViewContributor.htm?contributorid=" + String.valueOf(contributorid);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			} else if(action.equals("deleteAllOldTestRecords")) {
				logger.info("deleteAllOldTestRecords called");
				//get all old harvests
				//select harvestid, contributorid, starttime from harvest where starttime > ADD_MONTHS((select SYSDATE from dual), -1);
				//this does the delete, but is kinda slow... might be as fast as possible though
				//delete from harvestdata where harvestid in ( select harvestid from harvest where starttime > ADD_MONTHS((select SYSDATE from dual), -1) )
				url="ViewContributor.htm?contributorid=" + String.valueOf(contributorid);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////				
			} else if(action.equals("deletecontributor"))
			{
				daofactory.getContributorDAO().deletecontributor(contributorid);
				//delete the schedules for this contributor
				schedulerclient.deleteProductionSchedules(contributorid);
				url="ListContributors.htm?collectionid=" + request.getParameter("collectionid");
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}else if(action.equals("deletecollection")) 
			{
				Integer collectionid = Integer.valueOf(request.getParameter("collectionid"));
				daofactory.getCollectionDAO().deleteCollection(collectionid);
				url="ListCollections.htm";
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("toggleproduction"))
			{
				logger.info("toggling production");
				daofactory.getContributorDAO().toggleproduction(contributorid);
				
				String copyProfile = request.getParameter("copyProfile");
				if("true".equals(copyProfile)) {
					logger.info("copying profile");
					Contributor con = daofactory.getContributorDAO().getContributorCollectionAndDataprofile(contributorid);
					
					int oldprofileid = con.getProduction().getProfileid();			
					//create new one
					Profile p = daofactory.getProfileDAO().copyProfile(con.getTest().getProfileid(), contributorid, true);
					
					//save the new profile under our contributor	
					con.setProduction(p);
					daofactory.getContributorDAO().updateContributor(con);
					
					//delete the old one
					daofactory.getProfileDAO().deleteProfile(oldprofileid);
				}
				
				//currently they can only call this from the listharvests screen
				url="ListHarvests.htm?collectionid=" + request.getParameter("collectionid");
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("togglemonitor"))
			{
				daofactory.getContributorDAO().togglemonitored(contributorid);
				if(request.getParameter("harvestview") != null && request.getParameter("harvestview").equals("true"))
					url="ListHarvests.htm?collectionid=" + request.getParameter("collectionid");
				else
					url="ViewContributor.htm?contributorid=" + String.valueOf(contributorid);
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("harvestnow"))
			{
				Contributor c = daofactory.getContributorDAO().getContributorAndHarvestStepDetails(contributorid);		
				schedulerclient.runHarvestNow(contributorid);
				url = "ListHarvests.htm?collectionid=" + c.getCollection().getCollectionid();
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("harvestnownoschedule"))
			{
				Contributor c = daofactory.getContributorDAO().getContributorAndHarvestStepDetails(contributorid);			
				schedulerclient.runManualHarvest(null, SchedulerClient.FROM_EARLIEST, null, null, c, false, false, "none");
				url = "ListHarvests.htm?collectionid=" + c.getCollection().getCollectionid();
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////				
			}
			else if(action.equals("viewrecords"))
			{
				//get the viewrecords url from the contributor entry in the db
				Contributor con = daofactory.getContributorDAO().getContributor(contributorid);
				url = con.getViewrecordsurl();
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else if(action.equals("listrecords") || action.equals("identify") || 
					action.equals("listrecordsforoneweek") || action.equals("listrecordsforonemonth"))
			{
				/**
				 * There are a bunch of buttons on the list contributors, edit contributor, view contributor and harvest worktray screens
				 * that make various requests to the contributor's repositories. They are all handled through this method 
				 */
				Contributor con = daofactory.getContributorDAO().getContributorAndHarvestStepDetails(contributorid);
				
				//to list records we need to get the baseurl, from field, set and metadataprefix, identify just needs baseurl
				//first convert the datatime format to oai format
				Date lastharvest = null;
				
				Calendar cal = Calendar.getInstance();
				if(action.equals("listrecordsforoneweek"))
				{
					cal.add(cal.DATE, -7);
					lastharvest = cal.getTime();
				}
				else if (action.equals("listrecordsforonemonth"))
				{
					cal.add(cal.MONTH, -1);
					lastharvest = cal.getTime();
				}
				
				String from = null;
				if(lastharvest != null)
				{
					Format oai = new SimpleDateFormat(
				       con.getGranularity() == Contributor.SHORT_GRANULARITY 
				       ? SchedulerClient.oaiformatshort : SchedulerClient.oaiformat
				    );
					from =  oai.format(lastharvest);
				}
				
				//a map is easy to work with
				Map<String, String> pmap = PUtil.parametersToMap(con);
				
				//now its easy to get the parameters we want
				String baseurl = pmap.get("Base URL");
				String set = pmap.get("Set");
				String metadataprefix = pmap.get("Metadata Prefix");
				logger.info("gathered parameters for ws request");
				
				if(!action.equals("identify"))
				{
					//create the url
					StringBuilder urlbuilder = new StringBuilder(); 
					urlbuilder.append(baseurl);
					urlbuilder.append("?verb=ListRecords&metadataPrefix=");
					urlbuilder.append(metadataprefix);
					if(from != null)
						urlbuilder.append("&from=" + from);
					if(set != null)
						urlbuilder.append("&set=" + set);
					url = urlbuilder.toString();
				}
				else
				{
					url = baseurl + "?verb=Identify";
				}
				logger.info("URL| " + url);
				//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////				
			}
			else if(action.equals("starturl"))
			{
				Contributor con = daofactory.getContributorDAO().getContributorAndHarvestStepDetails(contributorid);
				Map<String, String> pmap = PUtil.parametersToMap(con);
				
				url = pmap.get("starturl");
				//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			}
			else
			{
				logger.info("unimplemented feature requested, or non-existant called!");
				//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////				
			}
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////		
		
		logger.info("interact complete redirecting...");
		//redirect to where they should go
		RedirectView rv = new RedirectView(url);
		ModelAndView mv = new ModelAndView(rv);
		return mv;
}
}
