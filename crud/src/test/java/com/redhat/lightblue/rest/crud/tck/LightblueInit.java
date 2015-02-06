package com.redhat.lightblue.rest.crud.tck;

import com.redhat.lightblue.config.CrudConfiguration;
import com.redhat.lightblue.config.MetadataConfiguration;
import com.redhat.lightblue.rest.RestConfiguration;

public class LightblueInit {
    public static final String DEFAULT_PATH = "tck/";

    public static LightblueInit lastInstance = new LightblueInit();

    public String metadataConfigurationPath = DEFAULT_PATH+MetadataConfiguration.FILENAME;
    public String crudConfigurationPath = DEFAULT_PATH+CrudConfiguration.FILENAME;
    public String restConfigurationPath = DEFAULT_PATH+RestConfiguration.DATASOURCE_FILENAME;
    public String configPropertiesPath = DEFAULT_PATH+"config.properties";
    public String[] metadataResourcePaths = {};

    private LightblueInit(){}

    protected LightblueInit(String metadataConfigurationPath, String crudConfigurationPath, String restConfigurationPath, String configPropertiesPath, String[] metadataResourcePaths) {
        this.metadataConfigurationPath = metadataConfigurationPath;
        this.crudConfigurationPath = crudConfigurationPath;
        this.restConfigurationPath = restConfigurationPath;
        this.configPropertiesPath = configPropertiesPath;
        this.metadataResourcePaths = metadataResourcePaths;
    }


}
