package harvester.client.web;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import harvester.client.service.ContributorService;
import harvester.client.util.ControllerUtil;
import harvester.client.util.WebUtil;
import harvester.data.*;

import java.util.Map;

import org.apache.commons.logging.*;

@Controller
@SuppressWarnings("unchecked")
public class ContributorController {

    protected final Log logger = LogFactory.getLog(getClass());
    private ContributorService contributorService;
    
    @Autowired
	public void setContributorService(ContributorService contributorService) {
		this.contributorService = contributorService;
	}
	
	@RequestMapping("/ListContributors.htm")
    public String listContributors(@RequestParam("collectionid") int collectionid, Map model) {   
      
		Collection col = contributorService.getEscapedContributors(collectionid);
		
		model.put("dateformat", new WebUtil());
		model.put("collection", col);
        return "ListContributors";
    }
	
	@RequestMapping("/ViewContributor.htm")
    public String viewContributor(@RequestParam("contributorid") int contributorid, Map model) {   
		
		model.put("contributor", contributorService.getEscapedContributor(contributorid));		
        return "ViewContributor";
    }
	
	@RequestMapping("/EditContributor.htm")
	public String editContributor(@RequestParam(value="new", required=false) Boolean isNew, 
							      @RequestParam("contributorid") int contributorid, Map model) {
		
		model.put("FROM_NLA", ContributorContact.FROM_NLA);
		model.put("FROM_OTHER", ContributorContact.FROM_OTHER);
		model.put("BUSINESS_CONTACT", ContributorContact.BUSINESS_CONTACT);
		model.put("TECHNICAL_CONTACT", ContributorContact.TECHNICAL_CONTACT);
		
		Contributor c = contributorService.getContributorFromSession(contributorid);
		model.put("contributor", c);
		 
		if(isNew != null && isNew)
			 model.put("new", true);
		Integer stepid = contributorService.getSelectedHarvestType(contributorid);
		if(stepid != null)
			 model.put("selectedtype", stepid);
		 
		model.put("harvesttypes", contributorService.getHarvestTypes(c.getCollection()));
		
		contributorService.fillMissingCollectionContacts(c);
		
		model.put("contributor", c);
		model.put("numcontacts", c.getContacts().size() + c.getContactselections().size());
		
		return "EditContributor";
	}
	
	@RequestMapping("/AddContributor.htm")
	public String addContributor(@RequestParam("collectionid") int collectionid) {

		contributorService.addNewContributorToSession(collectionid);
		
		return "redirect:EditContributor.htm?contributorid=-1&new=true";
	}
	
	@RequestMapping("/StartEditContributor.htm")
	public String startEditContributor(@RequestParam("contributorid") int contributorid) {
		
		contributorService.addExistingContributorToSession(contributorid);
		
		return "redirect:EditContributor.htm?contributorid=" + contributorid;
	}
	
	@RequestMapping("/ModifyNewContributor.htm")
	public String modifyNewContributor(@RequestParam("contributorid") int contributorid, WebRequest request , 
									   @RequestParam("name") String name, 
									   @RequestParam("description") String description,
									   @RequestParam("platform") String platform, 
									   @RequestParam("biggestcontact") int biggestcontact,
									   @RequestParam("harvesttype") int harvesttype) {

		Map<String, String> requestMap = ControllerUtil.ConvertRequestToMap(request);
		
		Contributor c = contributorService.updateContributorInSession(contributorid, name, description, platform);
		contributorService.AddContacts(c, biggestcontact, requestMap);		
		contributorService.saveHarvestTypeInSession(contributorid, harvesttype);
		
		return "redirect:EditConnectionSettingsStep2.htm?new=true&contributorid=" + contributorid;
	}
	
	@RequestMapping("/ModifyContributor.htm")
	public String modifyContributor( @RequestParam("contributorid") int contributorid, WebRequest request , 
								     @RequestParam("name") String name, 
								     @RequestParam("description") String description,
								     @RequestParam("platform") String platform, 
								     @RequestParam("biggestcontact") int biggestcontact) {
		
		Map<String, String> requestMap = ControllerUtil.ConvertRequestToMap(request);
		
		Contributor c = contributorService.updateContributorInSession(contributorid, name, description, platform);
		contributorService.AddContacts(c, biggestcontact, requestMap);		
		
		contributorService.SaveContributor(contributorid, /* is New? */ false);
		
		return "redirect:ViewContributor.htm?contributorid=" + contributorid;
	}
	
}
