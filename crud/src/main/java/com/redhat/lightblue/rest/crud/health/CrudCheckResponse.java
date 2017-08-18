package com.redhat.lightblue.rest.crud.health;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.JsonObject;

import java.util.Map;

public class CrudCheckResponse extends JsonObject {

    private static JsonNodeFactory factory = JsonNodeFactory.withExactBigDecimals(true);
    private Map<String, HealthCheck.Result> healthCheckResults;

    public CrudCheckResponse(Map<String, HealthCheck.Result> healthCheckResults) {
        this.healthCheckResults = healthCheckResults;
    }

    @Override
    public JsonNode toJson() {
        ObjectNode healthCheckResponse = factory.objectNode();
        for (Map.Entry<String, HealthCheck.Result> result : healthCheckResults.entrySet()) {
            ObjectNode resultResponse = factory.objectNode();
            resultResponse.put("healthy", result.getValue().isHealthy());
            resultResponse.put("message", result.getValue().getMessage());
            healthCheckResponse.put(result.getKey(), resultResponse);
        }
        return healthCheckResponse;
    }

}
