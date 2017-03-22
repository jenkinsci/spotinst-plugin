package hudson.plugins.spotinst.api;

import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.api.infra.RestClient;
import hudson.plugins.spotinst.api.infra.RestResponse;
import hudson.plugins.spotinst.common.CloudProviderEnum;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstancesResponse;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstancesResponse;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResponse;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResponse;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.model.spot.SpotRequest;
import hudson.plugins.spotinst.model.spot.SpotRequestResponse;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class SpotinstApi implements ISpotinstApi {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstApi.class);
    private static SpotinstApi instance;
    final static String SPOTINST_API_HOST = "https://api.spotinst.io";
    //endregion

    private SpotinstApi() {
    }

    public static SpotinstApi getInstance() {
        if (instance == null) {
            instance = new SpotinstApi();
        }
        return instance;
    }

    public static void setInstance(SpotinstApi spotinstApi) {
        instance = spotinstApi;
    }

    //region Private Methods
    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + SpotinstContext.getInstance().getSpotinstToken());
        headers.put("Content-Type", "application/json");

        return headers;
    }
    //endregion

    //region Public Methods

    public List<AwsElastigroupInstance> getAwsElastigroupInstances(String elastigroupId) {
        List<AwsElastigroupInstance> instances = null;
        Map<String, String>          headers   = buildHeaders();

        try {
            RestResponse response = RestClient
                    .sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + elastigroupId + "/status", headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                instances = new LinkedList<AwsElastigroupInstance>();
                AwsElastigroupInstancesResponse elastigroupResponse =
                        JsonMapper.fromJson(response.getBody(), AwsElastigroupInstancesResponse.class);
                if (elastigroupResponse.getResponse().getItems().size() > 0) {
                    for (AwsElastigroupInstance instance : elastigroupResponse.getResponse().getItems()) {
                        instances.add(instance);
                    }
                }
            }
            else {
                LOGGER.error("Failed to get Elastigroup instances, error code: " + response.getStatusCode() +
                             ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to get Elastigroup instances, error: " + e.getMessage());
        }

        return instances;
    }

    private int awsValidateToken(String token) {
        int                 isValid;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");

        try {
            RestResponse response = RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group", headers, null);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                isValid = 0;
            }
            else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                isValid = 1;
            }
            else {
                isValid = 2;
            }
        }
        catch (Exception e) {
            isValid = 2;
        }
        return isValid;
    }

    public ScaleUpResult awsScaleUp(String elastigroupId, int adjustment) {

        ScaleUpResult       retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("adjustment", String.valueOf(adjustment));

        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/aws/ec2/group/" + elastigroupId + "/scale/up", null, headers,
                             queryParams);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                ScaleUpResponse scaleResponse = JsonMapper.fromJson(response.getBody(), ScaleUpResponse.class);
                if (scaleResponse.getResponse().getItems().size() > 0) {
                    retVal = scaleResponse.getResponse().getItems().get(0);
                }
            }
            else {
                LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error code: " +
                             response.getStatusCode() + ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error: " + e.getMessage());
        }
        return retVal;
    }

    public SpotRequest getSpotRequest(String spotRequestId) {

        Map<String, String> headers     = buildHeaders();
        SpotRequest         spotRequest = null;
        try {
            RestResponse response =
                    RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/spot/" + spotRequestId, headers, null);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                SpotRequestResponse spotRequestResponse =
                        JsonMapper.fromJson(response.getBody(), SpotRequestResponse.class);
                if (spotRequestResponse.getResponse().getItems().size() > 0) {
                    spotRequest = spotRequestResponse.getResponse().getItems().get(0);
                }
            }
            else {
                LOGGER.error(
                        "Failed to get spot request: " + spotRequestId + ", error code: " + response.getStatusCode() +
                        ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to get spot request: " + spotRequestId + ", error: " + e.getMessage());
        }
        return spotRequest;
    }

    public boolean awsDetachInstance(String instanceId) {
        boolean             retVal  = false;
        Map<String, String> headers = buildHeaders();
        String detachRequest =
                "{\"instancesToDetach\" :[\"{INSTANCE_ID}\"],\"shouldTerminateInstances\" : true, \"shouldDecrementTargetCapacity\" : true}";
        String body = detachRequest.replace("{INSTANCE_ID}", instanceId);
        try {
            RestResponse response =
                    RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/instance/detach", body, headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                retVal = true;
            }
            else {
                LOGGER.error("Failed to detach instance:  " + instanceId + ", error code: " + response.getStatusCode() +
                             ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to detach instance:  " + instanceId + ", error: " + e.getMessage());
        }
        return retVal;
    }

    private int gcpValidateToken(String token) {
        int                 isValid;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");

        try {
            RestResponse response = RestClient.sendGet(SPOTINST_API_HOST + "/gcp/gce/group", headers, null);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                isValid = 0;
            }
            else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                isValid = 1;
            }
            else {
                isValid = 2;
            }
        }
        catch (Exception e) {
            isValid = 2;
        }
        return isValid;
    }

    @Override
    public int validateToken(CloudProviderEnum cloudProvider, String token) {
        int retVal = 0;
        switch (cloudProvider) {
            case AWS: {
                retVal = awsValidateToken(token);
            }
            break;
            case GCP: {
                retVal = gcpValidateToken(token);
            }
            break;
        }
        return retVal;
    }

    public GcpScaleUpResult gcpScaleUp(String elastigroupId, int adjustment) {

        GcpScaleUpResult    retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("adjustment", String.valueOf(adjustment));

        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + elastigroupId + "/scale/up", null, headers,
                             queryParams);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                GcpScaleUpResponse scaleResponse = JsonMapper.fromJson(response.getBody(), GcpScaleUpResponse.class);
                if (scaleResponse.getResponse().getItems().size() > 0) {
                    retVal = scaleResponse.getResponse().getItems().get(0);
                }
            }
            else {
                LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error code: " +
                             response.getStatusCode() + ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to scale up Elastigroup: " + elastigroupId + ", error: " + e.getMessage());
        }
        return retVal;
    }

    public boolean gcpDetachInstance(String groupId, String instanceName) {
        boolean             retVal  = false;
        Map<String, String> headers = buildHeaders();
        String detachRequest =
                "{\"instancesToDetach\" :[\"{INSTANCE_ID}\"],\"shouldTerminateInstances\" : true, \"shouldDecrementTargetCapacity\" : true}";
        String body = detachRequest.replace("{INSTANCE_ID}", instanceName);
        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/detachInstances", body, headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                retVal = true;
            }
            else {
                LOGGER.error(
                        "Failed to detach instance:  " + instanceName + ", error code: " + response.getStatusCode() +
                        ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to detach instance:  " + instanceName + ", error: " + e.getMessage());
        }
        return retVal;
    }

    public List<GcpElastigroupInstance> getGcpElastigroupInstances(String elastigroupId) {
        List<GcpElastigroupInstance> instances = null;
        Map<String, String>          headers   = buildHeaders();

        try {
            RestResponse response = RestClient
                    .sendGet(SPOTINST_API_HOST + "/gcp/gce/group/" + elastigroupId + "/status", headers, null);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                instances = new LinkedList<GcpElastigroupInstance>();
                GcpElastigroupInstancesResponse elastigroupResponse =
                        JsonMapper.fromJson(response.getBody(), GcpElastigroupInstancesResponse.class);
                if (elastigroupResponse.getResponse().getItems().size() > 0) {
                    for (GcpElastigroupInstance instance : elastigroupResponse.getResponse().getItems()) {
                        instances.add(instance);
                    }
                }
            }
            else {
                LOGGER.error("Failed to get Elastigroup instances, error code: " + response.getStatusCode() +
                             ", error message: " + response.getBody());
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to get Elastigroup instances, error: " + e.getMessage());
        }

        return instances;
    }
    //endregion
}
