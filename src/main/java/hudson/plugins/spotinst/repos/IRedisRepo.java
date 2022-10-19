package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;

public interface IRedisRepo {
    ApiResponse<String> setKey(String groupId,String accountId, String controllerIdentifier,Integer ttl);

    ApiResponse<Object> getValue(String groupId, String accountId);

    ApiResponse<Integer> deleteKey(String groupId, String accountId);

}
