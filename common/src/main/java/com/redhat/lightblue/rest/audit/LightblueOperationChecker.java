/*
 Copyright 2015 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.audit;

import java.util.regex.Matcher;

/**
 * Created by lcestari on 4/10/15.
 */
public interface LightblueOperationChecker {
    Info matches(String content);

    class Info {
        public static final String GET_NONE ="NONE";
        public static final String GET_ENTITY ="ENTITY";
        public static final String GET_ENTITY_DEFAULT ="ENTITYDEFAULT";
        public static final String GET_ENTITY_VERSION ="ENTITYVERSION";
        public static final String GET_ENTITY_VERSION_STATUS ="ENTITYVERSIONSTATUS";
        public static final String GET_ONLY_STATUS ="STATUS";
        public static final String GET_ENTITY_STATUS ="ENTITYSTATUS";

        public final Matcher matcher;
        public final boolean found;
        public final String operation;
        public final String otherFields;

        public String entity = null;
        public String version = null;
        public String status = null;

        public Info(String operation, boolean found, Matcher matcher, String otherFields) {
            this.operation = operation;
            this.found = found;
            this.matcher = matcher;
            this.otherFields = otherFields;
            if(found) {
                fillObject();
            }
        }

        public void fillEntity(int number){
            entity = matcher.group(number);
        }

        public void fillVersion(int number){
            version = matcher.group(number);
        }

        public void fillStatus(int number){
            status = matcher.group(number);
        }

        public void fillObject(){
            if(otherFields.equals(GET_ENTITY) || otherFields.equals(GET_ENTITY_VERSION) || otherFields.equals(GET_ENTITY_VERSION_STATUS) || otherFields.equals(GET_ENTITY_STATUS) || otherFields.equals(GET_ENTITY_DEFAULT) ) {
                fillEntity(1);
            }
            if(otherFields.equals(GET_ENTITY_VERSION) || otherFields.equals(GET_ENTITY_VERSION_STATUS)) {
                fillVersion(2);
            }
            if(otherFields.equals(GET_ENTITY_DEFAULT)){
                version = "default";
            }
            if(otherFields.equals(GET_ENTITY_VERSION_STATUS)){
                fillStatus(3);
            }
            if(otherFields.equals(GET_ENTITY_STATUS)){
                fillStatus(2);
            }
            if(otherFields.equals(GET_ONLY_STATUS)){
                fillStatus(1);
            }
        }
    }
}
