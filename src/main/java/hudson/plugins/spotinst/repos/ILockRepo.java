package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;

public interface ILockRepo {
    ApiResponse<String> acquireGroupControllerLock(String accountId, String groupId, String controllerIdentifier,
                                                   Integer ttl);

    ApiResponse<String> getGroupControllerLockValue(String accountId, String groupId);

    ApiResponse<Integer> deleteGroupControllerLock(String groupId, String accountId);

    ApiResponse<String> setGroupControllerLockExpiry(String accountId, String groupId, String controllerIdentifier,
                                                     Integer ttl);
}
