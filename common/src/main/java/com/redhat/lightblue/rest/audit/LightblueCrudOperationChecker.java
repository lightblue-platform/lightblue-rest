package com.redhat.lightblue.rest.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lcestari on 4/10/15.
 */
public enum LightblueCrudOperationChecker implements LightblueOperationChecker {
    //GET

    //AbstractCrudResource @Path("/find/{entity}") or @Path("/find/{entity}/{version}") w/ querystring simpleFind
    simpleFindVersionRegex("/+find/+(\\w+)/+([\\S]+)/*", "GET /find/{entity}/{version}", Info.GET_ENTITY_VERSION),
    simpleFindRegex("/+find/+(\\w+)/*", "GET /find/{entity}", Info.GET_ENTITY),
    //POST

    //AbstractCrudResource @Path("/save/{entity}") or @Path("/save/{entity}/{version}") save
    saveRegex("/+save/+(\\w+)/*", "POST /save/{entity}", Info.GET_ENTITY),
    saveVersionRegex("/+save/+(\\w+)/+([\\S]+)/*", "POST /save/{entity}/{version}", Info.GET_ENTITY_VERSION),
    //AbstractCrudResource @Path("/update/{entity}") or @Path("/update/{entity}/{version}") update
    updateRegex("/+update/+(\\w+)/*", "POST /update/{entity}", Info.GET_ENTITY),
    updateVersionRegex("/+update/+(\\w+)/+([\\S]+)/*", "POST /update/{entity}/{version}", Info.GET_ENTITY_VERSION),
    //AbstractCrudResource @Path("/delete/{entity}") or @Path("/delete/{entity}/{version}") delete
    deleteRegex("/+delete/+(\\w+)/*", "POST /delete/{entity}", Info.GET_ENTITY),
    deleteVersionRegex("/+delete/+(\\w+)/+([\\S]+)/*", "POST /delete/{entity}/{version}", Info.GET_ENTITY_VERSION),
    //AbstractCrudResource @Path("/find/{entity}") or @Path("/find/{entity}/{version}") find
    findRegex("/+find/+(\\w+)/*", "POST /find/{entity}", Info.GET_ENTITY),
    findVersionRegex("/+find/+(\\w+)/+([\\S]+)/*", "POST /find/{entity}/{version}", Info.GET_ENTITY_VERSION),
    //PUT

    //AbstractCrudResource @Path("/insert/{entity}") or @Path("/insert/{entity}/{version}")  insert
    insertRegex("/+insert/+(\\w+)/*", "PUT /insert/{entity}", Info.GET_ENTITY),
    insertVersionRegex("/+insert/+(\\w+)/+([\\S]+)/*", "PUT /insert/{entity}/{version}", Info.GET_ENTITY_VERSION),
    //AbstractCrudResource @Path("/{entity}") or @Path("/{entity}/{version}") is insertAlt
    insertAltRegex("/+(\\w+)/*", "PUT /{entity}", Info.GET_ENTITY),
    insertAltVersionRegex("/+(\\w+)/+([\\S]+)/*", "PUT /{entity}/{version}", Info.GET_ENTITY_VERSION);

    private final Pattern pattern;
    private final String operation;
    private final String moreInfo;

    LightblueCrudOperationChecker(String pattern, String operation, final String moreInfo) {
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

    @Override
    public Info matches(String content) {
        Matcher matcher = null;
        boolean found = false;
        if (content != null) {
            matcher = getPattern().matcher(content);
            found = matcher.matches();
        }
        return new Info(getOperation(), found, matcher, moreInfo);
    }
}
