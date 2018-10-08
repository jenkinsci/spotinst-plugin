package hudson.plugins.spotinst.api;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.plugins.spotinst.api.infra.JsonMapper;
import hudson.plugins.spotinst.api.infra.RestClient;
import hudson.plugins.spotinst.api.infra.RestResponse;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstancesResponse;
import hudson.plugins.spotinst.model.elastigroup.azure.AzureDetachNodeRequest;
import hudson.plugins.spotinst.model.elastigroup.azure.AzureElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.azure.AzureElastigroupInstancesResponse;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstancesResponse;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResponse;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResponse;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.model.spot.SpotRequest;
import hudson.plugins.spotinst.model.spot.SpotRequestResponse;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


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

    private static String buildUserAgent() {
        String retVal         = null;
        String jenkinsVersion = Jenkins.getInstance().VERSION;
        Plugin spotinst       = Jenkins.getInstance().getPlugin("spotinst");
        if (spotinst != null) {
            PluginWrapper wrapper = spotinst.getWrapper();
            if (wrapper != null) {
                String pluginVersion = wrapper.getVersion();
                retVal = String.format("Jenkins/%s;spotinst-plugin/%s", jenkinsVersion, pluginVersion);
            }
        }
        return retVal;
    }

    //region Private Methods
    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + SpotinstContext.getInstance().getSpotinstToken());
        headers.put("Content-Type", "application/json");
        String userAgent = buildUserAgent();
        if (userAgent != null) {
            headers.put("User-Agent", userAgent);
        }
        return headers;
    }

    private static Map<String, String> buildQueryParams() {
        Map<String, String> queryParams = new HashMap<String, String>();
        String              accountId   = SpotinstContext.getInstance().getAccountId();
        if (accountId != null && accountId.isEmpty() == false) {
            queryParams.put("accountId", accountId);
        }

        return queryParams;
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
    //endregion

    //region Public Methods

    //region AWS
    public List<AwsElastigroupInstance> getAwsElastigroupInstances(String elastigroupId) {
        List<AwsElastigroupInstance> instances   = null;
        Map<String, String>          headers     = buildHeaders();
        Map<String, String>          queryParams = buildQueryParams();
        try {
            RestResponse response = RestClient
                    .sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + elastigroupId + "/status", headers, queryParams);

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

    public ScaleUpResult awsScaleUp(String elastigroupId, int adjustment) {
        ScaleUpResult       retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams();
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
        Map<String, String> queryParams = buildQueryParams();

        SpotRequest spotRequest = null;
        try {
            RestResponse response =
                    RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/spot/" + spotRequestId, headers, queryParams);
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
        boolean             retVal      = false;
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        String detachRequest =
                "{\"instancesToDetach\" :[\"{INSTANCE_ID}\"],\"shouldTerminateInstances\" : true, \"shouldDecrementTargetCapacity\" : true}";
        String body = detachRequest.replace("{INSTANCE_ID}", instanceId);
        try {
            RestResponse response =
                    RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/instance/detach", body, headers, queryParams);

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
    //endregion

    //region GCP
    public GcpScaleUpResult gcpScaleUp(String elastigroupId, int adjustment) {

        GcpScaleUpResult    retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams();
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
        boolean             retVal      = false;
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        String detachRequest =
                "{\"instancesToDetach\" :[\"{INSTANCE_ID}\"],\"shouldTerminateInstances\" : true, \"shouldDecrementTargetCapacity\" : true}";
        String body = detachRequest.replace("{INSTANCE_ID}", instanceName);
        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/detachInstances", body, headers,
                             queryParams);

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
        List<GcpElastigroupInstance> instances   = null;
        Map<String, String>          headers     = buildHeaders();
        Map<String, String>          queryParams = buildQueryParams();

        try {
            RestResponse response = RestClient
                    .sendGet(SPOTINST_API_HOST + "/gcp/gce/group/" + elastigroupId + "/status", headers, queryParams);

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

    //region Azure
    @Override
    public List<AzureElastigroupInstance> getAzureElastigroupInstances(String elastigroupId) {
        List<AzureElastigroupInstance> instances   = null;
        Map<String, String>            headers     = buildHeaders();
        Map<String, String>            queryParams = buildQueryParams();
        try {
            RestResponse response = RestClient
                    .sendGet(SPOTINST_API_HOST + "/compute/azure/group/" + elastigroupId + "/status", headers,
                             queryParams);

            if (response.getStatusCode() == HttpStatus.SC_OK) {
                instances = new LinkedList<>();
                AzureElastigroupInstancesResponse azureElastigroupInstancesResponse =
                        JsonMapper.fromJson(response.getBody(), AzureElastigroupInstancesResponse.class);
                if (azureElastigroupInstancesResponse.getResponse().getItems().size() > 0) {
                    instances = azureElastigroupInstancesResponse.getResponse().getItems();
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

    @Override
    public boolean azureScaleUp(String elastigroupId, int adjustment) {
        boolean             retVal  = false;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams();
        queryParams.put("adjustment", String.valueOf(adjustment));

        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + elastigroupId + "/scale/up", null, headers,
                             queryParams);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                retVal = true;
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

    @Override
    public boolean azureDetachInstance(String elastigroupId, String instanceId) {
        boolean             retVal      = false;
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        AzureDetachNodeRequest request = new AzureDetachNodeRequest();
        request.setNodesToDetach(Arrays.asList(instanceId));
        request.setShouldDecrementTargetCapacity(true);
        String body = JsonMapper.toJson(request);

        try {
            RestResponse response = RestClient
                    .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + elastigroupId + "/detachNodes", body,
                             headers, queryParams);

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
    //endregion

    @Override
    public int validateToken(String token, String accountId) {
        int                 isValid;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");

        Map<String, String> queryParams = new HashMap<>();
        if (accountId != null && accountId.isEmpty() == false) {
            queryParams.put("accountId", accountId);
        }

        try {
            RestResponse response =
                    RestClient.sendGet(SPOTINST_API_HOST + "/events/subscription", headers, queryParams);
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
    //endregion
}
