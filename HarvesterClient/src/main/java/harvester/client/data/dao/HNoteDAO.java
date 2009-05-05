package harvester.client.data.dao;

import harvester.client.data.dao.interfaces.NoteDAO;
import harvester.data.*;

import org.apache.commons.logging.*;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

public class HNoteDAO implements NoteDAO {

	protected final Log logger = LogFactory.getLog(HNoteDAO.class);
	private SessionFactory sf;
	@Required
	public void setSessionFactory(SessionFactory sf) {
		this.sf = sf;
	}
	
	@Transactional
	public void addNote(Note n) {
		sf.getCurrentSession().save(n);
	}

	@Transactional
	public void deleteNote(Note n) {
		sf.getCurrentSession().delete(n);		
	}
	
}
