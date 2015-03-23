package com.redhat.lightblue.rest.crud.tck;

public class LightblueInitBuilder {
    private String metadataConfigurationPath;
    private String crudConfigurationPath;
    private String restConfigurationPath;
    private String configPropertiesPath;
    private String[] metadataResourcePaths;

    public LightblueInitBuilder setMetadataConfigurationPath(String metadataConfigurationPath) {
        this.metadataConfigurationPath = metadataConfigurationPath;
        return this;
    }

    public LightblueInitBuilder setCrudConfigurationPath(String crudConfigurationPath) {
        this.crudConfigurationPath = crudConfigurationPath;
        return this;
    }

    public LightblueInitBuilder setRestConfigurationPath(String restConfigurationPath) {
        this.restConfigurationPath = restConfigurationPath;
        return this;
    }

    public LightblueInitBuilder setConfigPropertiesPath(String configPropertiesPath) {
        this.configPropertiesPath = configPropertiesPath;
        return this;
    }

    public LightblueInitBuilder setMetadataResourcePaths(String[] metadataResourcePaths) {
        this.metadataResourcePaths = metadataResourcePaths;
        return this;
    }

    public LightblueInit createLightblueInit() {
        LightblueInit lightblueInit = new LightblueInit(metadataConfigurationPath, crudConfigurationPath, restConfigurationPath, configPropertiesPath, metadataResourcePaths);
        LightblueInit.lastInstance = lightblueInit;
        return lightblueInit;
    }
}