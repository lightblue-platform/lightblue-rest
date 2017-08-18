/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

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
package com.redhat.lightblue.rest.crud.cmd;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.rest.CallStatus;
import com.redhat.lightblue.rest.crud.RestCrudConstants;
import com.redhat.lightblue.rest.crud.health.CrudCheckResponse;
import com.redhat.lightblue.util.metrics.RequestMetrics;
import com.redhat.lightblue.util.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author dhaynes
 */
public class CheckDiagnosticsCommand extends AbstractRestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckDiagnosticsCommand.class);

    private HealthCheckRegistry healthCheckRegistry;
    private RequestMetrics metrics;

    public CheckDiagnosticsCommand(HealthCheckRegistry healthCheckRegistry, RequestMetrics metrics) {
        this(null, healthCheckRegistry, metrics);
    }

    public CheckDiagnosticsCommand(Mediator mediator, HealthCheckRegistry healthCheckRegistry, RequestMetrics metrics) {
        super(mediator);
        this.healthCheckRegistry = healthCheckRegistry;
        this.metrics = metrics;
    }

    @Override
    public CallStatus run() {

        RequestMetrics.Context context = metrics.startDiagnosticsRequest();
        LOGGER.debug("run: diagnostic");
        Error.reset();
        Error.push("rest");
        Error.push(getClass().getSimpleName());
        try {
            CallStatus callStatus = new CallStatus();
            Map<String, HealthCheck.Result> healthCheckResults = new LinkedHashMap<>();
            for (Map.Entry<String, Result> entry : healthCheckRegistry.runHealthChecks().entrySet()) {
                if (entry.getValue().isHealthy()) {
                    LOGGER.debug("diagnosticCheck is OK", entry.getKey());
                    healthCheckResults.put(entry.getKey(), entry.getValue());
                } else {
                    String errorDetails = entry.getKey().toString() + " " + entry.getValue().getDetails();
                    Error.push("diagnosticCheck failed! " + errorDetails);
                    LOGGER.debug("diagnosticCheck failed!", errorDetails);
                    return new CallStatus(Error.get(RestCrudConstants.ERR_REST_CHECK_DIAGNOSTICS, errorDetails));
                }
            }
            return new CallStatus(new CrudCheckResponse(healthCheckResults));
        } catch (Error e) {
            context.markRequestException(e);
            LOGGER.error("diagnosticCheck failure: {}", e);
            return new CallStatus(e);
        } catch (Exception e) {
            context.markRequestException(e);
            LOGGER.error("diagnosticCheck failure: {}", e);
            return new CallStatus(Error.get(RestCrudConstants.ERR_REST_CHECK_HEALTH, e.toString()));
        } finally {
            context.endRequestMonitoring();
        }
    }

}
