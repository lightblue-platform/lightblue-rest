package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.LightblueRequestUtils;
import com.redhat.lightblue.rest.crud.cmd.FindCommand;
import com.redhat.lightblue.util.metrics.DropwizardRequestMetrics;
import com.redhat.lightblue.util.metrics.MetricRegistryFactory;
import com.redhat.lightblue.util.metrics.RequestMetrics;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Async(period = 10, unit = TimeUnit.SECONDS)
public class ReadHealthCheck extends HealthCheck {

    @Override
    protected Result check() throws Exception {
        String entity = "test";
        String version = "1.0.0";
        String request = getFindTestRequest(entity, version);

        RequestMetrics metrics = new DropwizardRequestMetrics(MetricRegistryFactory.getJmxMetricRegistry());
        CallStatus callStatus = new FindCommand(entity, version, request, metrics).run();

        if(!Response.Status.OK.equals(callStatus.getHttpStatus())){
            return Result.unhealthy("Reads are not healthy: " + callStatus.getReturnValue());
        }
        return Result.healthy();
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
