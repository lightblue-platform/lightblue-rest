package com.redhat.lightblue.rest.integration;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

public class FakeIdentityManager implements IdentityManager {

    private final Map<String, FakeAccount> accounts = new HashMap<>();

    public FakeIdentityManager add(String name, String password) {
        accounts.put(name, new FakeAccount(name, password));

        return this;
    }

    public FakeIdentityManager add(String name, String password, Set<String> roles) {
        accounts.put(name, new FakeAccount(name, password, roles));

        return this;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        if (id == null || id.trim().length() == 0) {
            return null;
        }

        FakeAccount account = accounts.get(id);
        if ((credential instanceof PasswordCredential)
                && String.valueOf(((PasswordCredential) credential).getPassword()).equals(account.password)) {
            account.roles.add(
                    LightblueRestTestHarness.SECURITY_ROLE_AUTHENTICATED);
            return account;
        }

        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null; //Intentional
    }

    private class FakeAccount implements Account {

        public final String name;
        public final String password;
        public final Set<String> roles;

        public FakeAccount(String name, String password) {
            this(name, password, null);
        }

        public FakeAccount(String name, String password, Set<String> roles) {
            if (name == null) {
                throw new NullPointerException("name cannot be null");
            }
            if (password == null) {
                throw new NullPointerException("password cannot be null");
            }

            this.name = name;
            this.password = password;
            if (roles == null) {
                this.roles = new HashSet<>();
            }
            else {
                this.roles = roles;
            }
        }

        @Override
        public Principal getPrincipal() {
            return new Principal() {

                @Override
                public String getName() {
                    return name;
                }

            };
        }

        @Override
        public Set<String> getRoles() {
            return roles;
        }

    }

}
