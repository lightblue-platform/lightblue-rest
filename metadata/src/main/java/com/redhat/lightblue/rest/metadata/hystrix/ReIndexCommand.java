/**
 *
 */
package com.redhat.lightblue.rest.metadata.hystrix;

import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.metadata.EntityInfo;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.mongo.crud.MongoCRUDController;
import com.redhat.lightblue.rest.RestConfiguration;

/**
 * @author bvulaj
 *
 */
public class ReIndexCommand extends AbstractRestCommand {

    private final String entity;

    public ReIndexCommand(String clientKey, Metadata metadata, String entity) {
        super(ReIndexCommand.class, clientKey, metadata);
        this.entity = entity;
    }


    /* (non-Javadoc)
     * @see com.netflix.hystrix.HystrixCommand#run()
     */
    @Override
    protected String run() throws Exception {
        EntityInfo entityInfo = getMetadata().getEntityInfo(entity);
        CRUDController crudController = RestConfiguration.getFactory().getFactory().getCRUDController(entityInfo.getDataStore().getBackend());
        if (crudController instanceof MongoCRUDController) {
            MongoCRUDController mcc = (MongoCRUDController) crudController;
            mcc.reindex(entityInfo);
            return String.format("Starting reindex of %s", entity);
        }
        throw new UnsupportedOperationException("This command is only available on MongoDB backed instances");
    }

}
