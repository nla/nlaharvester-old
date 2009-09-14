package harvester.client.web;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import harvester.client.service.CollectionService;
import harvester.client.util.ControllerUtil;
import harvester.data.Collection;
import harvester.data.CollectionContact;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.*;

@Controller
@SuppressWarnings("unchecked")
public class CollectionsController{

    protected final Log logger = LogFactory.getLog(getClass());
    private CollectionService collectionService;

    @Autowired
	public void setCollectionService(CollectionService collectionService) {
		this.collectionService = collectionService;
	}


	@RequestMapping("/ListCollections.htm")
    public String listCollections(Map model) {   
        model.put("collections", collectionService.getCollectionListEscaped());        
        return "ListCollections";
    }

    @RequestMapping("/ViewCollection.htm")
    public String viewCollection(Map model, @RequestParam("collectionid") int collectionid) {
    	model.putAll(collectionService.getCollectionModel(collectionid));
    	return "ViewCollection";
    }
       
    @RequestMapping("/EditCollection.htm")
    public String editCollection(Map model, @RequestParam("collectionid") int collectionid) {
	    model.put("outputstages", collectionService.getOutputStages());
    	model.putAll(collectionService.getCollectionModel(collectionid));
    	return "EditCollection";
    }
    
    @RequestMapping("/CreateCollection.htm")
    public String createCollection(Map model) {
    	model.put("collection", collectionService.getNewCollection());
    	model.put("newcollection", true);
		model.put("nocontacts", true);
		model.put("outputstages", collectionService.getOutputStages());
    	return "EditCollection";
    }

    @RequestMapping("/ModifyCollection.htm")
    public String modifyCollection(@RequestParam(value="collectionid", required=false) Integer collectionid,
    							   @RequestParam("name") String name, 
    							   @RequestParam("description") String description,
    							   @RequestParam("userguide") String userguide,
    							   @RequestParam(value="outputstage", required=false) Integer outputstage,
    							   @RequestParam(value="biggestcontact") int biggestcontact,
    							   WebRequest request) {
    	 	
		if(userguide != null)
			userguide = userguide.trim();
		if("".equals(userguide))
			userguide = null;
		if(outputstage == -1) outputstage = null;

		Map<String, String> requestMap = ControllerUtil.ConvertRequestToMap(request);
		
		//since collectionid in the collection is int not Integer, we need to detect if it is basically equivalent to null
		Collection c = collectionService.getCollection(collectionid);
		
		Set<CollectionContact> contacts = collectionService.extractContacts(biggestcontact, requestMap , c);
		collectionService.ModifyCollection(c, description, userguide, name, contacts, outputstage);

		collectionService.updateCollectionSize(c.getCollectionid());
		
    	return "redirect:ViewCollection.htm?collectionid=" + c.getCollectionid();
    }
    
    
}