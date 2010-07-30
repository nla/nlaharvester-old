package harvester.client.connconfig.actions;


import harvester.client.data.dao.interfaces.ContributorDAO;
import harvester.client.util.KeyValue;
import harvester.data.Contributor;
import harvester.data.ProfileStep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Loader actions for the Single Business Discovery Service.
 */
public class SBDSLoaderActions implements LoadStepActions {
	
	private static Log logger = LogFactory.getLog(SBDSLoaderActions.class);
	
	protected String host = "";
	
	protected String port = "";
	
	protected ContributorDAO contributorDao;

	/**
	 * Delete production records for the supplied contributor.
	 * Currently not a supported action for this loader.
	 * @param contributorid    the identifier for the contributor
	 * @param contributorname  the name of the contributor
	 */
	public void deleteProductionRecords(int contributorid, String contributorname) {
		// do nothing - currently not a supported action
		Contributor contributor = contributorDao.getContributor(contributorid);

		int source = Integer.parseInt(contributor.getPlatform());
		
		Socket socket = null;
		try {
			byte[] message = new byte[] {0x0, 0x8, 0x0, 0x0, 0x0, 0x2, 0x0, 0x0 };
			message[6] = (byte) ((source >>> 8) & 0xFF);
			message[7] = (byte) (source & 0xFF);
			
			// open the connection
			socket = new Socket(host, Integer.parseInt(port));
			
			OutputStream os = socket.getOutputStream();
			os.write(message);
			os.flush();
			os.close();
			
			InputStream is = socket.getInputStream();
			int i;
			while ((i = is.read(message)) > -1) {
				// do nothing
			}
			if (message[1] == 3) {
				logger.warn("Delete All Records message for " + source + " failed");
			}
			is.close();

		} catch (UnknownHostException e) {
			logger.warn("Unknown host: " + host, e);
		} catch (IOException e) {
			logger.warn("Couldn't get I/O for the connection to: " + host, e);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					logger.warn("Unable to close socket to: " + host, e);
				}
			}
		}
	}

	/**
	 * Get the size of the collection.
	 * Currently not a supported action for this loader.
	 * @return always zero
	 */
	public Integer getCollectionSize() {
		// currently not a support action
		return new Integer(0);
	}

	public List<KeyValue> getSettings(ProfileStep load_step) {
		return null;
	}
	
	// Getters and Setters
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public ContributorDAO getContributorDao() {
		return contributorDao;
	}

	public void setContributorDao(ContributorDAO contributorDao) {
		this.contributorDao = contributorDao;
	}

}
