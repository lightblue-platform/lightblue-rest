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
package com.redhat.lightblue.rest.metadata;

import java.util.List;

/**
 * An exception for when user does not have the proper roles to perform an action.
 *
 * @author dcrissman
 */
public class RoleException extends Exception{

    private static final long serialVersionUID = 8185195626339000384L;

    private final List<String> roles;

    public List<String> getRoles(){
        return roles;
    }

    public RoleException(List<String> roles){
        super("One of the following roles is required: " + roles);
        this.roles = roles;
    }

}
