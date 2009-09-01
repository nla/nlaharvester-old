package harvester.client.connconfig.actions;


/**
 * Loader actions for the Single Business Discovery Service.
 */
public class SBDSLoaderActions implements LoadStepActions {

	/**
	 * Delete production records for the supplied contributor.
	 * Currently not a supported action for this loader.
	 * @param contributorid    the identifier for the contributor
	 * @param contributorname  the name of the contributor
	 */
	public void deleteProductionRecords(int contributorid, String contributorname) {
		// do nothing - currently not a supported action
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

}
