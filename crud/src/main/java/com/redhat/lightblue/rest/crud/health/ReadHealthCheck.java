package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.AbstractCrudResource;
import com.redhat.lightblue.rest.crud.LightblueRequestUtils;
import com.redhat.lightblue.rest.crud.cmd.FindCommand;
import com.redhat.lightblue.util.metrics.DropwizardRequestMetrics;
import com.redhat.lightblue.util.metrics.MetricRegistryFactory;
import com.redhat.lightblue.util.metrics.NoopRequestMetrics;
import com.redhat.lightblue.util.metrics.RequestMetrics;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Async(period = 10, unit = TimeUnit.SECONDS)
public class ReadHealthCheck extends HealthCheck {
    private final RequestMetrics metrics;

    public ReadHealthCheck() {
        this(new NoopRequestMetrics());
    }

    public ReadHealthCheck(RequestMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected Result check() throws Exception {
        String entity = "test";
        String request = getFindTestRequest(entity, null);

        CallStatus callStatus = new FindCommand(entity, null, request, metrics).run();

        ResultBuilder resultBuilder;

        if (Response.Status.OK.equals(callStatus.getHttpStatus())) {
            resultBuilder = Result.builder().healthy();
        } else {
            resultBuilder = Result.builder().unhealthy().withMessage("Reads are not healthy");
        }
        resultBuilder.withDetail("readResponse", callStatus.getReturnValue().toJson());

        return resultBuilder.build();
    }

    private String getFindTestRequest(String entity, String version) throws Exception {
        String query = "objectType:" + entity;
        String projection = null;
        String sort = null;
        Long maxResults = 1L;
        String request = LightblueRequestUtils.buildSimpleRequest(entity,version,query,projection,sort,null,null,maxResults).toString();
        return request;
    }

}
