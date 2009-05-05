package harvester.client.web;

import org.springframework.beans.factory.annotation.*;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import harvester.client.data.dao.DAOFactory;
import harvester.client.util.EscapeHTML;
import harvester.data.*;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Controller
public class NotesController {
	
    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Autowired
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

    @RequestMapping("/ViewNotes.htm")
    public String viewNotes(@RequestParam("contributorid") int contributorid, ModelMap model) {
		logger.info("processing ViewNotes request");
		
		Contributor c = daofactory.getContributorDAO().getContributorCollectionAndNotes(contributorid);
		model.put("contributor", c);
       
		for(Note n : c.getNotes())
		{
			n.setCreator(EscapeHTML.forHTML(n.getCreator()));
			n.setDescription(EscapeHTML.forHTML(n.getDescription()));
		}
		
		logger.info("viewnotes model built");
        return "ViewNotes";		
    }
    
    @RequestMapping("/ModifyNote.htm")
    public String modifyNotes(@RequestParam("contributorid") int contributorid, 
    						  //@RequestParam("creator") String creator,
    						  @RequestParam("note") String note) {
    	
		SecurityContext sctx = SecurityContextHolder.getContext();
    	String creator = sctx.getAuthentication().getName();
    	
		Note n = new Note();
		n.setCreator(creator);
		n.setDescription(note);
		n.setTimestamp(new Date());
		n.setContributorid(contributorid);
		daofactory.getNoteDAO().addNote(n);
		return "redirect:ViewNotes.htm?contributorid=" + contributorid;
    }
    
    @RequestMapping("/DeleteNote.htm")
    public String deleteNotes(@RequestParam("contributorid") int contributorid,
    						  @RequestParam("delete") int noteid) {
		Note n = new Note();
		n.setNoteid(noteid);
		daofactory.getNoteDAO().deleteNote(n);	//we only need to set the id for hibernate's delete to work
		return "redirect:ViewNotes.htm?contributorid=" + contributorid;
    }
    
}