package com.redhat.lightblue.rest.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lcestari on 4/10/15.
 */
public enum LightblueMetadataOperationChecker implements LightblueOperationChecker {
    //GET

    //AbstractMetadataResource @Path("/dependencies") or @Path("/{entity}/dependencies") or @Path("/{entity}/{version}/dependencies")
    getDepGraphVersionRegex("/+(\\w+)/+([\\S]+)/+dependencies/*", "GET /{entity}/{version}/dependencies", Info.GET_ENTITY_VERSION),
    getDepGraphEntityRegex("/+(\\w+)/+dependencies/*", "GET /{entity}/dependencies", Info.GET_ENTITY),
    getDepGraphRegex("/+dependencies/*", "GET /dependencies", Info.GET_NONE),
    //AbstractMetadataResource @Path("/roles") or @Path("/{entity}/roles") or @Path("/{entity}/{version}/roles") getEntityRoles
    getEntityRolesVersionRegex("/+(\\w+)/+([\\S]+)/+roles/*", "GET /{entity}/{version}/roles", Info.GET_ENTITY_VERSION),
    getEntityRolesEntityRegex("/+(\\w+)/+roles/*", "GET /{entity}/roles", Info.GET_ENTITY),
    getEntityRolesRegex("/+roles/*", "GET /roles", Info.GET_NONE),
    //AbstractMetadataResource @Path("/") or @Path("/s={statuses}") getEntityNames
    getEntityNamesRegex("/*", "GET /",Info.GET_NONE),
    getEntityNamesStatusRegex ("/+s=(\\w+)/*","GET /s={statuses}",Info.GET_ONLY_STATUS),
    //AbstractMetadataResource @Path("/{entity}") getEntityVersions
    getEntityVersionsRegex("/+(\\w+)/*","GET /{entity}",Info.GET_ENTITY),
    //AbstractMetadataResource @Path("/{entity}/{version}") getMetadata
    getMetadataRegex("/+(\\w+)/+([\\S]+)/*","GET /{entity}/{version}",Info.GET_ENTITY_VERSION),


    //POST

    //AbstractMetadataResource @Path("/{entity}/{version}/default") setDefaultVersion
    setDefaultVersionRegex("/+(\\w+)/+([\\S]+)/+default/*", "POST /{entity}/{version}/default", Info.GET_ENTITY_VERSION ),


    //PUT

    //AbstractMetadataResource @Path("/{entity}/{version}") createMetadata
    createMetadataRegex("/+(\\w+)/+([\\S]+)/*","PUT /{entity}/{version}",Info.GET_ENTITY_VERSION),
    //AbstractMetadataResource @Path("/{entity}/schema={version}") createSchema
    createSchemaRegex("/+(\\w+)/+schema=([\\S]+)/*","PUT /{entity}/schema={version}",Info.GET_ENTITY_STATUS),
    //AbstractMetadataResource @Path("/{entity}") updateEntityInfo
    updateEntityInfoRegex("/+(\\w+)/*","PUT /{entity}",Info.GET_ENTITY),
    //AbstractMetadataResource @Path("/{entity}/{version}/{status}") updateSchemaStatus
    updateSchemaStatusRegex("/+(\\w+)/+([\\S]+)/+(\\w+)/*","PUT /{entity}/{version}/{status}",Info.GET_ENTITY_VERSION_STATUS),


    //DELETE

    //AbstractMetadataResource @Path("/{entity}") removeEntity
    removeEntityRegex("/+(\\w+)/*","DELETE /{entity}",Info.GET_ENTITY),
    //AbstractMetadataResource @Path("/{entity}/default") clearDefaultVersion
    clearDefaultVersionRegex("/+(\\w+)/+default/*", "DELETE /{entity}/default", Info.GET_ENTITY_DEFAULT);


    private final Pattern pattern;
    private final String operation;
    private final String moreInfo;

    LightblueMetadataOperationChecker(String pattern, String operation, final String moreInfo) {
        this.pattern = Pattern.compile(pattern);
        this.operation = operation;
        this.moreInfo = moreInfo;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getOperation() {
        return operation;
    }

    public Info matches(String content){
        Matcher matcher = getPattern().matcher(content);
        boolean found = matcher.matches();
        return new Info(getOperation(),found,matcher, moreInfo);
    }
}
