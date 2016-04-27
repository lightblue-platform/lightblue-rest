/**
 *
 */
package com.redhat.lightblue.rest.metadata.hystrix;

import com.redhat.lightblue.crud.CRUDController;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.mongo.crud.MongoCRUDController;
import com.redhat.lightblue.rest.RestConfiguration;

/**
 * @author bvulaj
 *
 */
public class ReIndexCommand extends AbstractRestCommand {

    private final String entity;
    private final String version;

    public ReIndexCommand(String clientKey, Metadata metadata, String entity, String version) {
        super(ReIndexCommand.class, clientKey, metadata);
        this.entity = entity;
        if ("default".equals(version)) {
            this.version = null;
        } else {
            this.version = version;
        }
    }


    /* (non-Javadoc)
     * @see com.netflix.hystrix.HystrixCommand#run()
     */
    @Override
    protected String run() throws Exception {
        EntityMetadata emd = getMetadata().getEntityMetadata(entity, version);
        CRUDController crudController = RestConfiguration.getFactory().getFactory().getCRUDController(emd);
        if (crudController instanceof MongoCRUDController) {
            MongoCRUDController mcc = (MongoCRUDController) crudController;
            mcc.reindex(emd);
            return String.format("Starting reindex of %s:%s", entity, version);
        }
        throw new UnsupportedOperationException("This command is only available on MongoDB backed instances");
    }

}
