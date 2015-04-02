package com.redhat.lightblue.rest.audit;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Created by lcestari on 4/2/15.
 */
public class MyPrincipal implements Principal {
    private final String userName;

    public MyPrincipal(String userName) {
        this.userName = userName;
    }

    @Override
    public String getName() {
        return userName;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }
}
