package harvester.client.web;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.ModelAndView;

import harvester.client.data.dao.DAOFactory;
import harvester.client.profileconfig.CollectionProfileView;
import harvester.client.profileconfig.ProfileConfigUtil;
import harvester.client.profileconfig.ProfileSession;
import harvester.data.Collection;
import harvester.data.Profile;
import harvester.data.ProfileStep;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.*;


public class ViewProcessingProfilesController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());
    private DAOFactory daofactory;

    @Required
	public void setDaofactory(DAOFactory daofactory) {
		this.daofactory = daofactory;
	}

    private ProfileSession profilesession;

    @Required
    public void setProfilesession(ProfileSession ps) {
    	this.profilesession = ps;
    }
    
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		logger.info("processing view processing profiles request");
		
		String edit = request.getParameter("edit");
		String add = request.getParameter("add");
		if(edit != null)
			return EditRequest(request, response);
		if(add != null)		
			return AddRequest(request, response);

		
		//get all the passed fields we are expecting
		int collectionid = Integer.valueOf((String) request.getParameter("collectionid"));
		
		Map<String, Object> model = new HashMap<String, Object>();
		Collection col = daofactory.getCollectionDAO().getCollectionAndProfiles(collectionid);
		model.put("collection", col);
		
		//convert the profiles into CollectionProfileView objects
		List<CollectionProfileView> profiles = new LinkedList<CollectionProfileView>();		
		for(Profile p : col.getProfiles()) {
			CollectionProfileView pview = new CollectionProfileView();
			pview.setSteps(ProfileConfigUtil.getProfileView(p));
			pview.setName(p.getName());
			pview.setId(p.getProfileid().toString());
			pview.setDescription(p.getDescription());
			profiles.add(pview);
		}
		
		model.put("profiles", profiles);
		
		//the restriction types for profilesteps
		model.put("None", ProfileStep.NORMAL);
		model.put("Mandatory", ProfileStep.MANDATORY);
		model.put("Locked", ProfileStep.LOCKED);
		
		logger.info("view processing profiles model built");
        return new ModelAndView("ViewProcessingProfiles", "model", model);
    }

	private ModelAndView AddRequest(HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		logger.info("add request, gathering information for redirect");
		
		profilesession.initProfileSession(request);
		
		Profile dp = new Profile();
		dp.setProfileid(-1);		
		dp.setType(-1);

		profilesession.addCollectionProfile(dp, profilesession.getOwnerid());
		
		logger.info("redirecting to EditProcessingSteps");
		return new ModelAndView(new RedirectView(profilesession.getEditProfileUrl()));
	}

	private ModelAndView EditRequest(HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		logger.info("edit request, gathering information for redirect");
		
		profilesession.initProfileSession(request);
		
		Profile dp = daofactory.getProfileDAO().getProfile(profilesession.getProfileid());

		//the profile is extacted from the db by the profilesession, we need only tell it to save it
		logger.info("profile=" + dp.getProfileid());
		profilesession.addCollectionProfile(dp, profilesession.getOwnerid());
		
		logger.info("redirecting to EditProcessingSteps");
		return new ModelAndView(new RedirectView(profilesession.getEditProfileUrl()));
	}

}
