package harvester.client.profileconfig.customized;


import harvester.client.connconfig.StepParameterView;
import harvester.client.connconfig.actions.LoadStepActions;
import harvester.client.data.dao.DAOFactory;
import harvester.client.profileconfig.ICustomizedStep;
import harvester.client.profileconfig.ParameterComparator;
import harvester.client.profileconfig.ProfileSession;
import harvester.client.util.ControllerUtil;
import harvester.client.util.KeyValue;
import harvester.client.util.WebUtil;
import harvester.data.ProfileStep;
import harvester.data.ProfileStepParameter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;



/**
 * This handles surprisingly complex pre and post processing for the convertvalue step.
 * Most of the difficulty is caused by having to support both uploaded files as well as 
 * directly inputted data.
 */
@Controller
@SuppressWarnings("unchecked")
public class ConvertValue implements ICustomizedStep {

    protected final Log logger = LogFactory.getLog(getClass());
	
    //values for these are injected by the servlet xml file
    private int mappingfileid;
    private int conversionid;
    private int fieldnameid;

    public int getFieldnameid() {
		return fieldnameid;
	}

	public void setFieldnameid(int fieldnameid) {
		this.fieldnameid = fieldnameid;
	}

	private DAOFactory daofactory;
    private ProfileSession profilesession;   
    
    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

    @Autowired
    public void setProfilesession(ProfileSession ps) {
    	this.profilesession = ps;
    }
    
    
    /** returns the document 
     * @throws IOException */
    @RequestMapping("/GetConvertValueCSV.htm")
    public ModelAndView getConvertValueCSV(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	logger.info("get convert value csv called");

    	//String filename = request.getParameter("filename");
    	String data = request.getParameter("data");
    	
		response.setContentType("text/plain;charset=UTF-8");	//if set to csv forces download, below line only needed for filename
		response.setCharacterEncoding("UTF-8");
		//response.addHeader("Content-Disposition", "attachment; filename=" + filename);	//shows as a download link
		ServletOutputStream outputStream = response.getOutputStream();

		outputStream.print(data);

		outputStream.flush();
		outputStream.close();

		return null;
    }
    
    @RequestMapping("/ConvertValuePostBack.htm")
    public ModelAndView convertValuePostBack(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	logger.info("ConvertValuePostBack CALLED!");
    	
		Map pmap = ControllerUtil.FileUploadRequestToMap(request);				

		/////////////////////////////////////////////////////////////////////
		
		profilesession.initProfileSession(pmap);
    	
		String mappingused = (String)pmap.get("mappingused");
		String rules = (String)pmap.get("mappingfile");    
    	boolean valid = true;    	 			
		
		//check with side the radio button is on
		if("true".equals(mappingused) ) {
			
			if(!rules.trim().equals("")) {			
				try {
				  ExcelCSVParser parser = new ExcelCSVParser(new StringReader(rules));
				  String values[] [] = parser.getAllValues();
				  //check all lines are right length
				  for(int i = 0; i < values.length; i++) {
					  if(values[i].length != 2) {
						  logger.info("found line with " + values[i].length + " length!");
						  valid = false;
					  }
				  }
				} catch (Exception e) {
					//If not valid return convert value page view, otherwise redirect as normal
					logger.info("csv doc not valid");
					
					valid = false;
				}			

			} else {
				logger.info("empty doc");
				valid = false;
			}
		} else {
			logger.info("no need to validate");
			valid = true;
		}
		
		logger.info("returning " + valid);

		response.setCharacterEncoding("utf-8");
		response.getOutputStream().print(valid ? "true" : "false");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getOutputStream().close();
		return null;
    }
    
    
    /**

     */
	public Map PostProcess(Map inmap) {
		
		logger.info("convert post processor called");
		
		String mappingfile = (String)inmap.get("mappingfile");
		String mappingused = (String)inmap.get("mappingused");
		String fieldname = (String)inmap.get("fieldname");		
		
		Map newmap = new HashMap();
		newmap.putAll(inmap);
		
		newmap.put(String.valueOf(fieldnameid), fieldname);
		
		if( mappingused == null || mappingused.equals("false") ) {
			newmap.put(String.valueOf(mappingfileid), "");	//in case it was null

			try {
				StringWriter out = new StringWriter();
				ExcelCSVPrinter printer = new ExcelCSVPrinter(out);
				
				int biggestfield = Integer.valueOf((String)inmap.get("biggestfield"));
				for(int i = 0 ; i <= biggestfield; i++) {
					String originalValue = (String)inmap.get("rule.original." + i);	
					String newValue = (String)inmap.get("rule.new." + i);
					if (originalValue != null && newValue != null) {
						printer.writeln(new String[] { originalValue, newValue});
					}				
				}
				
				newmap.put(String.valueOf(conversionid), out.toString());
				
			} catch (Exception e) {
				logger.error("error creating csv document", e);
			}
			
			logger.info("not using a mapping file");

		} else 
		{
			String mappingfilename = (String)inmap.get("mappingfilename");	
			newmap.put(String.valueOf(mappingfileid), mappingfilename);
			if(mappingfile == null || mappingfile.equals(""))
				mappingfile = (String)inmap.get("rules");
			newmap.put(String.valueOf(conversionid), mappingfile);
		}		
		
		return newmap;

	}

	public void PreProcess(Map<String, Object> model) {
		logger.info("convert pre processor called");
		
		//extract the rules data from the model
		String rules = null;		
		boolean mappingused = false;
		String mappingfilename = null; 
		
		List<StepParameterView> steps = (List<StepParameterView>) model.get("parameters");
		
		for(StepParameterView spv : steps) {
			
			if(spv.getId() == conversionid) {
				rules = spv.getValue();
			}
			
			if(spv.getId() == mappingfileid){
				if(spv.getValue() == null || spv.getValue().equals("")){
					logger.info("setting mapping used to false");	
				} else{
					logger.info("setting mapping used to true");
					model.put("mappingused", "true");
					mappingused = true;
					mappingfilename = spv.getValue();							
				}
			}
			
			if(spv.getId() == fieldnameid) {
				model.put("fieldname", spv.getValue());
			}
			
		}
		
		List<KeyValue> kvs = new LinkedList<KeyValue>();
		
		if(mappingused && rules != null) {
			logger.info("using mapping file and rules not null, filename= " + mappingfilename);
			model.put("rules", rules);
			model.put("mappingfile", mappingfilename);
			kvs.add(new KeyValue("", ""));
		} else {			
			//extract from csv
			
			if(rules != null && !rules.trim().equals("")) {
				String[][] values = ExcelCSVParser.parse(rules);
				
				for (int i=0; i<values.length; i++) {
				    	kvs.add( new KeyValue(values[i][0],values[i][1]) );
				}
			} else {
				kvs.add(new KeyValue("", ""));
			}
			
		}
		
		model.put("rulelist", kvs);		
		
	}

	public int getConversionid() {
		return conversionid;
	}

	public void setConversionid(int conversionid) {
		this.conversionid = conversionid;
	}

	public int getMappingfileid() {
		return mappingfileid;
	}

	public void setMappingfileid(int mappingfileid) {
		this.mappingfileid = mappingfileid;
	}

	public String renderPlainTextView(ProfileStep ps) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<dl>\n");
		
		ProfileStepParameter[] parameters = ps.getParameters().toArray(new ProfileStepParameter[0]);
		ParameterComparator comp = new ParameterComparator();
		
		
		Arrays.sort(parameters, comp);
		
		for(int i = 0 ; i < parameters.length; i++) {
			String psp_name = parameters[i].getPis().getParametername();
			String psp_value = parameters[i].getValue();
			
			if(psp_name.equals("mappingfile")){
				sb.append("<dt>Mapping File</dt>\n");
				if(psp_value != null)
					sb.append("<dd>" + psp_value + "</dd>\n");
				else
					sb.append("<dd>No File in Use</dd>\n");
			} else {
				sb.append("<dt>" + psp_name + "</dt>\n");
				sb.append("<dd>" + psp_value + "</dd>\n");
			}
		}
		
		sb.append("</dl>\n");
		
		return sb.toString();
	}

}
