package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;

public interface ILockRepo {
    ApiResponse<String> acquireGroupControllerLock(String groupId, String accountId, String controllerIdentifier,
                                                   Integer ttl);

    ApiResponse<String> getGroupControllerLockValue(String groupId, String accountId);

    ApiResponse<Integer> deleteGroupControllerLock(String groupId, String accountId);

    ApiResponse<String> setExpiry(String groupId, String accountId, String controllerIdentifier, Integer ttl);
}
