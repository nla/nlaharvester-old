package harvester.client.profileconfig.customized;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.connconfig.StepParameterView;
import harvester.client.data.dao.DAOFactory;
import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.util.ControllerUtil;
import harvester.data.Step;
import harvester.data.StepFile;

@Controller
@SuppressWarnings("unchecked")
public class XSLTTranslator implements ICustomizedStep {

    protected final Log logger = LogFactory.getLog(getClass());
	
	private DAOFactory daofactory;
    private int fileidid;
    private static int MAX_CLASHES_TO_SHOW = 6;	//note that twise this can be shown if you count collection clashes and contributor clashes together
    
    
    public void setFileidid(int fileidid) {
		this.fileidid = fileidid;
	}

	@Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}
    
	/** deletes the given file
	 * 
	 */
	@RequestMapping("/DeleteFile.htm")
	public ModelAndView deleteFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
    	int fileid = Integer.valueOf(request.getParameter("fileid"));
		
		logger.info("delete file called, fileid=" + fileid);
		
		daofactory.getStepFileDAO().deleteFile(fileid);
		
		response.setCharacterEncoding("utf-8");
		response.getOutputStream().print("deleted");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getOutputStream().close();
		return null;	
		
	}
	
    /** returns the stylesheet
     * @throws Exception */
    @RequestMapping("/GetFile.htm")
    public ModelAndView getFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	logger.info("get file called");

    	int fileid = Integer.valueOf(request.getParameter("fileid"));
    	    	
    	StepFile file = daofactory.getStepFileDAO().getFile(fileid);
    	
		response.setContentType("text/xml;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Content-Disposition", "attachment; filename=" + file.getFilename());	//shows as a download link
		ServletOutputStream outputStream = response.getOutputStream();

		outputStream.print(daofactory.getStepFileDAO().getFileData(fileid));

		outputStream.flush();
		outputStream.close();

		return null;
    }
    
    @RequestMapping("/SaveOrUpdateFile.htm")
    public ModelAndView saveOrUpdateFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	logger.info("saveOrUpdateFile called");
    	Integer newfileid = null;
    	
		Map pmap = ControllerUtil.FileUploadRequestToMap(request);
		
		//extract parameters
		String replace = "";
		if(pmap.get("replacefileid") != null && !((String)pmap.get("replacefileid")).equals(""))
			replace = "replace";
		//if we are in a replace request all of these have the word "replace" before them.
		
		String filename = (String)pmap.get(replace + "stylesheetfilename");
		String filedata = (String)pmap.get(replace + "stylesheetfile"); 
		String description = (String)pmap.get(replace + "sdescription");
		Integer stepid = null;
		if(pmap.containsKey("stepid"))	//no replace word before this
			stepid = Integer.valueOf((String)pmap.get("stepid"));			
		Integer fileid = null;
		if(pmap.containsKey(replace + "fileid"))
			fileid = Integer.valueOf((String)pmap.get(replace + "fileid"));		
		
		logger.info("file has length " + filedata.length());
		
		//validate		
		boolean valid = true;
		
		try {
			TransformerFactory xformFactory = TransformerFactory.newInstance();
			xformFactory.newTemplates(new StreamSource(new StringReader(filedata)));
			
		} catch (Exception e) {
			valid = false;
			logger.info("xslt invalid: " + e.getMessage(), e);
		}
	
		
		if(valid) {
			try {
				//attempt to save
				StepFile file = new StepFile(Hibernate.createBlob(filedata.getBytes("UTF-8")),description,fileid, filename,stepid);
				daofactory.getStepFileDAO().saveOrUpdateFile(file);
				newfileid = file.getFileid();
			} catch (Exception e) {
				logger.info("failed to save", e);
			}
			
		}
		
		response.setCharacterEncoding("utf-8");
		String returnstring = newfileid == null ? "invalid" : (newfileid.toString() + "=" + filename);
		response.getOutputStream().print(returnstring);
		logger.info("returning " + returnstring);
		response.setStatus(HttpServletResponse.SC_OK);
		response.getOutputStream().close();
		return null;		
    }
    
	public Map PostProcess(Map inmap) {
		Map newmap = new HashMap();
		newmap.putAll(inmap);
		String fileid = (String)newmap.get("stylesheet");
		logger.info("picked fileid=" + fileid);
		//newmap.put("fileid", fileid);
		newmap.put(String.valueOf(fileidid), fileid);
		
		return newmap;
	}

	public void PreProcess(Map<String, Object> model) {
		logger.info("Translate pre-processor called");
	
		Step step = (Step)model.get("step");		
		
		List<StepParameterView> steps = (List<StepParameterView>) model.get("parameters");
		
		//extract the fileid from the given list
		Integer fileid = null;
		
		for(StepParameterView spv : steps)
			if(spv.getId() == fileidid && spv.getValue() != null)
				fileid = Integer.valueOf(spv.getValue());
	
		logger.info("selected fileid=" + fileid);
		
		//fill in all the other files
		List<StepFile> files = daofactory.getStepFileDAO().getFiles(step.getStepid());
		model.put("files", files);
		
		
		//find clashes
		HashMap<Integer, String> allclashes = new HashMap<Integer, String>();
		HashMap<Integer, String> replaceMsgs = new HashMap<Integer, String>();

		if(model.get("colview") != null)
			logger.info("This is a collection view, collection name: " +  model.get("collectionname"));
		else
			logger.info("This is a contributor view, contributor name: " + model.get("contributorname"));
		
		for(StepFile f : files) {
			List<String> clashes = daofactory.getStepFileDAO().getClashes(f.getFileid(), fileidid);
			List<String> collectionClashes = daofactory.getStepFileDAO().getCollectionClashes(f.getFileid(), fileidid);
			
			//remove this step's selection from the list
			if(f.getFileid().equals(fileid)) {
				if(model.get("colview") != null) 	//remove a single instance of this  name from the list.
					collectionClashes.remove(model.get("collectionname"));
				else
					clashes.remove(model.get("contributorname"));
				logger.info("removed file from its own list fileid= " + fileid + ", clashes= " + clashes + " collectionClashes= " + collectionClashes);
			}
			
			StringBuilder deleteSb = new StringBuilder();
			StringBuilder replaceSb = new StringBuilder();
			
			Set<String> uniqueClashes = new HashSet<String>(clashes);
			int numClashes = uniqueClashes.size();
			
			if ( !uniqueClashes.isEmpty() ) {
				deleteSb.append("<p>This stylesheet can not be deleted as it is being used by " + numClashes
						+ " contributor" + (numClashes > 1 ? "s" : "") + ".</p>\n");
				replaceSb.append("<p>" + numClashes + " contributor" + (numClashes > 1 ? "s have" : " has") 
						+ " processing steps which are using this stylesheet and "
						+ "<strong><em>will be affected by your change!</em></strong> </p>");
				deleteSb.append("<ul>\n"); replaceSb.append("<ul>\n");
				int shown_clashes = 0;
				for(String clash : uniqueClashes) {
					if(shown_clashes == MAX_CLASHES_TO_SHOW)
						break;
					deleteSb.append("<li>" + clash + "</li>\n");
					replaceSb.append("<li>" + clash + "</li>\n");
					shown_clashes++;
				}
				if(shown_clashes == MAX_CLASHES_TO_SHOW) {
					deleteSb.append("<li>...</li>\n");
					replaceSb.append("<li>...</li>\n");
				}
				deleteSb.append("</ul>\n"); replaceSb.append("</ul>\n"); 				
			}
			
			Set<String> uniqueCollectionClashes = new HashSet<String>(collectionClashes);
			numClashes = uniqueCollectionClashes.size();
			
			if ( !uniqueCollectionClashes.isEmpty() ) {
				deleteSb.append("<p>This stylesheet can not be deleted as it is being used by " + numClashes
						+ " collection" + (numClashes > 1 ? "s" : "") + ".</p>\n");
				replaceSb.append("<p>" + numClashes + " collection" + (numClashes > 1 ? "s have" : " has") 
						+ " processing steps which are using this stylesheet and "
						+ "<strong><em>will be affected by your change!</em></strong> </p>");
				deleteSb.append("<ul>\n"); replaceSb.append("<ul>\n");
				int shown_clashes = 0;
				for(String clash : uniqueCollectionClashes) {
					if(shown_clashes == MAX_CLASHES_TO_SHOW)
						break;
					deleteSb.append("<li>" + clash + "</li>\n");
					replaceSb.append("<li>" + clash + "</li>\n");
					shown_clashes++;
				}
				if(shown_clashes == MAX_CLASHES_TO_SHOW) {
					deleteSb.append("<li>...</li>\n");
					replaceSb.append("<li>...</li>\n");
				}
				deleteSb.append("</ul>\n");	replaceSb.append("</ul>\n");			
			}
			
			if(deleteSb.length() != 0) {
				allclashes.put(f.getFileid(), deleteSb.toString());
				replaceMsgs.put(f.getFileid(), replaceSb.toString());
			}
		}
		
		model.put("replacemsgs", replaceMsgs);
		model.put("clashes", allclashes);		
		
		//set a default file if one is not yet selected
		if(fileid == null && !files.isEmpty())
			fileid = files.get(0).getFileid();
		
		////this is the one currently chosen	
		model.put("fileid", fileid);
		
	}

}
