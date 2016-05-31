package hudson.plugins.spotinst.common;

import hudson.plugins.spotinst.elastigroup.*;
import hudson.plugins.spotinst.scale.ScaleUpResponse;
import hudson.plugins.spotinst.scale.ScaleUpResult;
import hudson.plugins.spotinst.spot.SpotRequest;
import hudson.plugins.spotinst.spot.SpotRequestResponse;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class SpotinstGateway {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstGateway.class);
    final static String SPOTINST_API_HOST = "https://api.spotinst.io";
    //endregion

    //region Private Methods
    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + SpotinstContext.getInstance().getSpotinstToken());
        headers.put("Content-Type", "application/json");

        return headers;
    }
    //endregion

    //region Public Methods
    public static List<String> getElastigroupInstances(String elastigroupId) {
        List<String> instances = new LinkedList<>();
        Map<String, String> headers = buildHeaders();

        try {
            RestResponse response = RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + elastigroupId + "/status", headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                ElastigroupInstancesResponse elastigroupResponse = JsonMapper.fromJson(response.getBody(), ElastigroupInstancesResponse.class);
                if (elastigroupResponse.getResponse().getItems().size() > 0) {
                    for (ElastigroupInstance instance : elastigroupResponse.getResponse().getItems()) {
                        if (instance.getInstanceId() != null) {
                            instances.add(instance.getInstanceId());
                        }
                    }
                }
            } else {
                LOGGER.error("Failed to get Elastigroup instances, error code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get Elastigroup instances, error: " + e.getMessage());
        }

        return instances;
    }

    public static int validateToken(String token) {
        int isValid;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");

        try {
            RestResponse response = RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group", headers, null);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                isValid = 0;
            } else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                isValid = 1;
            } else {
                isValid = 2;
            }
        } catch (Exception e) {
            isValid = 2;
        }
        return isValid;
    }

    public static ScaleUpResult scaleUp(String elastigroupId, int adjustment) {

        ScaleUpResult retVal = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("adjustment", String.valueOf(adjustment));

        try {
            RestResponse response = RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/group/" + elastigroupId + "/scale/up", null, headers, queryParams);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                ScaleUpResponse scaleResponse = JsonMapper.fromJson(response.getBody(), ScaleUpResponse.class);
                if (scaleResponse.getResponse().getItems().size() > 0) {
                    retVal = scaleResponse.getResponse().getItems().get(0);
                }
            } else {
                LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error: " + e.getMessage());
        }
        return retVal;
    }

    public static SpotRequest getSpotRequest(String spotRequestId) {

        Map<String, String> headers = buildHeaders();
        SpotRequest spotRequest = null;
        try {
            RestResponse response = RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/spot/" + spotRequestId, headers, null);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                SpotRequestResponse spotRequestResponse = JsonMapper.fromJson(response.getBody(), SpotRequestResponse.class);
                if (spotRequestResponse.getResponse().getItems().size() > 0) {
                    spotRequest = spotRequestResponse.getResponse().getItems().get(0);
                }
            } else {
                LOGGER.error("Failed to get spot request: " + spotRequestId + ", error code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get spot request: " + spotRequestId + ", error: " + e.getMessage());
        }
        return spotRequest;
    }

    public static boolean detachInstance(String instanceId) {
        boolean retVal = false;
        Map<String, String> headers = buildHeaders();
        String detachRequest = "{\"instancesToDetach\" :[\"{INSTANCE_ID}\"],\"shouldTerminateInstances\" : true, \"shouldDecrementTargetCapacity\" : true}";
        String body = detachRequest.replace("{INSTANCE_ID}", instanceId);
        try {
            RestResponse response = RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/instance/detach", body, headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                retVal = true;
            } else {
                LOGGER.error("Failed to detach instance:  " + instanceId + ", error code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to detach instance:  " + instanceId + ", error: " + e.getMessage());
        }
        return retVal;
    }
    //endregion
}
