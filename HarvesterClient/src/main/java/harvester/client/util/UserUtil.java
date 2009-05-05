package harvester.client.util;

import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

/**
 * This is made callable from within velocity templates, so that they can easily access the user's username
 * @author adefazio
 *
 */
public class UserUtil {

	public String getUserName() {
		SecurityContext sctx = SecurityContextHolder.getContext();
    	return sctx.getAuthentication().getName();
	}
	
}
