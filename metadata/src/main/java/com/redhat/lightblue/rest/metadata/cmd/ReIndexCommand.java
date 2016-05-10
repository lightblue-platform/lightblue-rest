/**
 *
 */
package com.redhat.lightblue.rest.metadata.cmd;

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

    public ReIndexCommand(Metadata metadata, String entity) {
        super(metadata);
        this.entity = entity;
    }


    /* (non-Javadoc)
     * @see com.netflix.hystrix.HystrixCommand#run()
     */
    @Override
    public String run()  {
        try {
            EntityInfo entityInfo = getMetadata().getEntityInfo(entity);
            CRUDController crudController = RestConfiguration.getFactory().getFactory().getCRUDController(entityInfo.getDataStore().getBackend());
            if (crudController instanceof MongoCRUDController) {
                MongoCRUDController mcc = (MongoCRUDController) crudController;
                mcc.reindex(entityInfo);
                return String.format("Starting reindex of %s", entity);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new UnsupportedOperationException("This command is only available on MongoDB backed instances");
    }

}
