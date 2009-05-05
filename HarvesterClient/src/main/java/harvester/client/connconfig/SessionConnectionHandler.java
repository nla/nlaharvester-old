package harvester.client.connconfig;

import java.util.HashMap;


/**
 * this class tracks the changes of connection settings through the wizard
 * there is one lot of settings for each contributorid, to allow editing multiple
 * contributors at once.
 */
public class SessionConnectionHandler {

	/**
	 * map between a contributor and the currently open session for the connection settings wizard for that contributor.
	 */
	HashMap<Integer, ConnectionSettings> settings = new HashMap<Integer, ConnectionSettings>();
	
	public ConnectionSettings getConnectionSetting(int contributorid)
	{
		return settings.get(contributorid);
	}
	
	public HashMap<Integer, ConnectionSettings> getSettings()
	{
		return settings;
	}

	public void setSettings(HashMap<Integer, ConnectionSettings> settings) {
		this.settings = settings;
	}
	
}
