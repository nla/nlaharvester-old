package harvester.processor.steps;

import harvester.processor.data.dao.DAOFactory;
import harvester.processor.data.dao.interfaces.HarvestdataDAO;
import harvester.processor.main.*;
import harvester.processor.util.StepLogger;

import java.util.*;
import javax.servlet.ServletContext;

/** Stores passed records in the database then returns the same list passed in.
 * Not used directly as a step in the pipeline anymore.
 * */
public class DatabaseStorer implements StagePluginInterface {

    private StepLogger logger;
    HashMap<String, Object> props;
    private DAOFactory daofactory;
    private HarvestdataDAO harvestdatadao;

    private int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return "DatabaseStorer";
    }

    public DatabaseStorer() {}

    public void Dispose() {}

    public void Initialise(HashMap<String, Object> props, StepLogger logger,
                           ServletContext servletContext) throws Exception {
        this.logger = logger;

        logger.locallog("initializing " + getName(), getName());

        daofactory = DAOFactory.getDAOFactory();
        harvestdatadao = daofactory.getHarvestdataDAO();

        this.props = props;
    }

    public Records Process(Records records)  throws Exception {

        logger.locallog("Processing", "DatabaseStorer");

        //get the harvest id to use
        int harvestid = Integer.valueOf(props.get("harvestid").toString());
        //stage is the "position" field that was in this stages profile
        int stage = Integer.valueOf(props.get("stage").toString());

        //all are added in one transaction
        harvestdatadao.AddToDatabaseBulk(records.getRecords(), harvestid, stage);

        return records;
    }
}
