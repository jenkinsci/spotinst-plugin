package hudson.plugins.spotinst.api;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.plugins.spotinst.api.infra.*;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.aws.*;
import hudson.plugins.spotinst.model.azure.*;
import hudson.plugins.spotinst.model.gcp.*;
import hudson.plugins.spotinst.model.redis.RedisDeleteKeyResponse;
import hudson.plugins.spotinst.model.redis.RedisGetValueResponse;
import hudson.plugins.spotinst.model.redis.RedisSetKeyRequest;
import hudson.plugins.spotinst.model.redis.RedisSetKeyResponse;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class SpotinstApi {

    //region Members
    private static final Logger LOGGER                  = LoggerFactory.getLogger(SpotinstApi.class);
    private final static String SPOTINST_API_HOST       = "https://api.spotinst.io";
    private final static String HEADER_AUTH             = "Authorization";
    private final static String AUTH_PREFIX             = "Bearer ";
    private final static String HEADER_CONTENT_TYPE     = "Content-Type";
    private final static String CONTENT_TYPE            = "application/json";
    private final static String QUERY_PARAM_ACCOUNT_ID  = "accountId";
    private final static String USER_AGENT_FORMAT       = "Jenkins/%s;spotinst-plugin/%s";
    private final static String PLUGIN_NAME             = "spotinst";
    private final static String HEADER_USER_AGENT       = "User-Agent";
    private final static String QUERY_PARAM_ADJUSTMENT  = "adjustment";
    private final static String AZURE_VM_SERVICE_PREFIX = "/azure/compute";
    //endregion

    //region Public Methods

    public static int validateToken(String token, String accountId) {
        int                 isValid;
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_AUTH, AUTH_PREFIX + token);
        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE);

        Map<String, String> queryParams = new HashMap<>();

        if (accountId != null && accountId.isEmpty() == false) {
            queryParams.put(QUERY_PARAM_ACCOUNT_ID, accountId);
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

    //region AWS
    public static List<AwsGroupInstance> getAwsGroupInstances(String groupId, String accountId) throws ApiException {
        List<AwsGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>    headers     = buildHeaders();
        Map<String, String>    queryParams = buildQueryParams(accountId);

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/status", headers, queryParams);

        AwsGroupInstancesResponse instancesResponse = getCastedResponse(response, AwsGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }

    public static AwsScaleUpResult awsScaleUp(String groupId, int adjustment, String accountId) throws ApiException {
        AwsScaleUpResult    retVal      = null;
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);
        queryParams.put(QUERY_PARAM_ADJUSTMENT, String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/scale/up", null, headers, queryParams);

        AwsScaleUpResponse scaleUpResponse = getCastedResponse(response, AwsScaleUpResponse.class);

        if (scaleUpResponse.getResponse().getItems().size() > 0) {
            retVal = scaleUpResponse.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static Boolean awsDetachInstance(String instanceId, String accountId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);

        AwsDetachInstancesRequest request = new AwsDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceId));
        request.setShouldDecrementTargetCapacity(true);
        request.setShouldTerminateInstances(true);

        String body = JsonMapper.toJson(request);

        RestResponse response =
                RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/instance/detach", body, headers, queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }

    public static List<AwsInstanceType> getAllAwsInstanceTypes(String accountId) throws ApiException {
        List<AwsInstanceType> retVal;
        Map<String, String>   headers     = buildHeaders();
        Map<String, String>   queryParams = buildQueryParams(accountId);
        queryParams.put("distinctTypesList","true");

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/instanceType", headers, queryParams);

        AwsInstanceTypesResponse
                allAwsInstanceTypesResponse = getCastedResponse(response, AwsInstanceTypesResponse.class);

        retVal = allAwsInstanceTypesResponse.getResponse().getItems();

        return retVal;
    }

    //endregion

    //region GCP
    public static GcpScaleUpResult gcpScaleUp(String groupId, int adjustment, String accountId) throws ApiException {

        GcpScaleUpResult    retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams(accountId);
        queryParams.put(QUERY_PARAM_ADJUSTMENT, String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/scale/up", null, headers, queryParams);

        GcpScaleUpResponse scaleUpResponse = getCastedResponse(response, GcpScaleUpResponse.class);

        if (scaleUpResponse.getResponse().getItems().size() > 0) {
            retVal = scaleUpResponse.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static Boolean gcpDetachInstance(String groupId, String instanceName, String accountId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);

        GcpDetachInstancesRequest request = new GcpDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceName));
        request.setShouldDecrementTargetCapacity(true);
        request.setShouldTerminateInstances(true);
        String body = JsonMapper.toJson(request);

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/detachInstances", body, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }

    public static List<GcpGroupInstance> getGcpGroupInstances(String groupId, String accountId) throws ApiException {
        List<GcpGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>    headers     = buildHeaders();
        Map<String, String>    queryParams = buildQueryParams(accountId);

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/status", headers, queryParams);

        GcpGroupInstancesResponse instancesResponse = getCastedResponse(response, GcpGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }
    //endregion

    //region Azure Scale Sets
    public static List<AzureGroupInstance> getAzureGroupInstances(String groupId,
                                                                  String accountId) throws ApiException {
        List<AzureGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>      headers     = buildHeaders();
        Map<String, String>      queryParams = buildQueryParams(accountId);

        RestResponse response = RestClient
                .sendGet(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/status", headers, queryParams);

        AzureGroupInstancesResponse instancesResponse = getCastedResponse(response, AzureGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }

    public static Boolean azureScaleUp(String groupId, int adjustment, String accountId) throws ApiException {
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams(accountId);
        queryParams.put("adjustment", String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/scale/up", null, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }

    public static Boolean azureDetachInstance(String groupId, String instanceId, String accountId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);

        AzureDetachInstancesRequest request = new AzureDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceId));
        request.setShouldDecrementTargetCapacity(true);
        String body = JsonMapper.toJson(request);

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/detachInstances", body, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }
    //endregion

    //region Azure VMs
    public static AzureGroupStatus getAzureVmGroupStatus(String groupId, String accountId) throws ApiException {
        AzureGroupStatus    retVal      = new AzureGroupStatus();
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);

        RestResponse response = RestClient
                .sendGet(SPOTINST_API_HOST + AZURE_VM_SERVICE_PREFIX + "/group/" + groupId + "/status", headers,
                         queryParams);

        AzureGroupStatusResponse vmsResponse = getCastedResponse(response, AzureGroupStatusResponse.class);

        if (vmsResponse.getResponse().getItems().size() > 0) {
            retVal = vmsResponse.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static List<AzureScaleUpResultNewVm> azureVmScaleUp(String groupId, int adjustment,
                                                               String accountId) throws ApiException {
        List<AzureScaleUpResultNewVm> retVal  = new LinkedList<>();
        Map<String, String>           headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams(accountId);
        queryParams.put("adjustment", String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + AZURE_VM_SERVICE_PREFIX + "/group/" + groupId + "/scale/up", null, headers,
                         queryParams);

        AzureScaleUpResponse scaleUpResponse = getCastedResponse(response, AzureScaleUpResponse.class);

        if (scaleUpResponse.getResponse().getItems().size() > 0) {
            retVal = scaleUpResponse.getResponse().getItems();
        }

        return retVal;
    }

    public static Boolean azureVmDetach(String groupId, String vmId, String accountId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams(accountId);

        AzureDetachVMsRequest request = new AzureDetachVMsRequest();
        request.setVmsToDetach(Collections.singletonList(vmId));
        request.setShouldDecrementTargetCapacity(true);
        request.setShouldTerminateVms(true);
        String body = JsonMapper.toJson(request);
        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + AZURE_VM_SERVICE_PREFIX + "/group/" + groupId + "/detachVms", body,
                         headers, queryParams);
        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }
    //endregion

    //Redis
    public static <T> T getRedisValue(String groupId, String accountId) throws ApiException {
        T retVal      = null;

        Map<String, String>    headers     = buildHeaders();

        Map<String, String>    queryParams = buildQueryParams(accountId);

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/jenkinsPlugin", headers, queryParams);

        RedisGetValueResponse<T> redisValue = getCastedResponse(response, RedisGetValueResponse.class);

        if (redisValue.getResponse().getItems().size() > 0) {
            retVal = redisValue.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static String setRedisKey(String groupId, String accountId, String controllerIdentifier, Integer ttl) throws ApiException {
        String retVal      = null;

        Map<String, String>    headers     = buildHeaders();

        Map<String, String>    queryParams = buildQueryParams(accountId);

        RedisSetKeyRequest request = new RedisSetKeyRequest();
        request.setGroupId(groupId);
        request.setControllerIdentifier(controllerIdentifier);
        request.setTtl(ttl);

        String body = JsonMapper.toJson(request);

        RestResponse response =
                RestClient.sendPost(SPOTINST_API_HOST + "/aws/ec2/group/jenkinsPlugin", body, headers, queryParams);

        RedisSetKeyResponse redisValue = getCastedResponse(response, RedisSetKeyResponse.class);

        if (redisValue.getResponse().getItems().size() > 0) {
            retVal = redisValue.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static Integer deleteRedisKey(String groupId, String accountId) throws ApiException {
        Integer retVal      = null;

        Map<String, String>    headers     = buildHeaders();

        Map<String, String>    queryParams = buildQueryParams(accountId);

        RestResponse response =
                RestClient.sendDelete(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/jenkinsPlugin", headers, queryParams);

        RedisDeleteKeyResponse redisValue = getCastedResponse(response, RedisDeleteKeyResponse.class);

        if (redisValue.getResponse().getItems().size() > 0) {
            retVal = redisValue.getResponse().getItems().get(0);
        }

        return retVal;
    }
    //endregion

    //region Private Methods
    private static String buildUserAgent() {
        String retVal         = null;
        String jenkinsVersion = Jenkins.getInstance().VERSION;
        Plugin spotinst       = Jenkins.getInstance().getPlugin(PLUGIN_NAME);

        if (spotinst != null) {
            PluginWrapper wrapper = spotinst.getWrapper();
            if (wrapper != null) {
                String pluginVersion = wrapper.getVersion();
                retVal = String.format(USER_AGENT_FORMAT, jenkinsVersion, pluginVersion);
            }
        }

        return retVal;
    }

    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_AUTH, AUTH_PREFIX + SpotinstContext.getInstance().getSpotinstToken());
        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE);
        String userAgent = buildUserAgent();

        if (userAgent != null) {
            headers.put(HEADER_USER_AGENT, userAgent);
        }

        return headers;
    }

    private static Map<String, String> buildQueryParams(String accountId) {
        Map<String, String> queryParams    = new HashMap<>();
        String              accountIdParam = accountId;

        if (accountIdParam == null || accountIdParam.isEmpty()) {
            accountIdParam = SpotinstContext.getInstance().getAccountId();
        }

        if (accountIdParam != null && accountIdParam.isEmpty() == false) {
            queryParams.put(QUERY_PARAM_ACCOUNT_ID, accountIdParam);
        }

        return queryParams;
    }

    private static <T> T getCastedResponse(RestResponse response, Class<T> contentClass) throws ApiException {
        T retVal;

        if (response.getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
            retVal = JsonMapper.fromJson(response.getBody(), contentClass);
            if (retVal == null) {
                throw new ApiException(String.format("Can't parse response to class: %s", contentClass.toString()));
            }

        }
        else {
            String message =
                    String.format("Got status code different then SC_OK : %s. Body: %s", response.getStatusCode(),
                                  response.getBody());
            LOGGER.error(message);

            ApiErrorsResponse apiErrorsResponse = JsonMapper.fromJson(response.getBody(), ApiErrorsResponse.class);
            if (apiErrorsResponse != null) {
                throw new ApiErrorsException(message, apiErrorsResponse.getResponse().getErrors());
            }
            else {
                throw new ApiException(message);
            }
        }

        return retVal;
    }
    //endregion
}
