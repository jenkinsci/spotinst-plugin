package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;

public interface ILockRepo {
    ApiResponse<String> Lock(String groupId, String accountId, String controllerIdentifier, Integer ttl);

    ApiResponse<String> getLockValueById(String groupId, String accountId);

    ApiResponse<Integer> Unlock(String groupId, String accountId);

}
