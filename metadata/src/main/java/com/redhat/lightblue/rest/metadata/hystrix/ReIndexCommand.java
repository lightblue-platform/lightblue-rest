/**
 *
 */
package com.redhat.lightblue.rest.metadata.hystrix;

import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.metadata.EntityInfo;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.mongo.crud.MongoCRUDController;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.metadata.cmd.AbstractRestCommand;

/**
 * @author bvulaj
 *
 */
public class ReIndexCommand extends AbstractRestCommand {

    private final String entity;
    private final String version;
    private final QueryExpression query;

    public ReIndexCommand(String clientKey, Metadata metadata, String entity) {
        this(clientKey, metadata, entity, null, null);
    }

    public ReIndexCommand(String clientKey, Metadata metadata, String entity, String version, QueryExpression qe) {
        super(metadata);
        this.entity = entity;
        this.version = version;
        this.query = qe;
    }


    /* (non-Javadoc)
     * @see com.netflix.hystrix.HystrixCommand#run()
     */
    @Override
    public String run() {
        EntityInfo entityInfo = getMetadata().getEntityInfo(entity);
        CRUDController crudController = RestConfiguration.getFactory().getFactory().getCRUDController(entityInfo.getDataStore().getBackend());
        if (crudController instanceof MongoCRUDController) {
            MongoCRUDController mcc = (MongoCRUDController) crudController;
            mcc.reindex(entityInfo, getMetadata(), version, query);
            return String.format("Starting reindex of %s", entity);
        }
        throw new UnsupportedOperationException("This command is only available on MongoDB backed instances");
    }

}
