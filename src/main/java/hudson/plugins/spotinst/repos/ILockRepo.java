package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;

public interface ILockRepo {
    ApiResponse<String> acquireLockGroupController(String groupId, String accountId, String controllerIdentifier,
                                                   Integer ttl);

    ApiResponse<String> expandGroupControllerLock(String groupId, String accountId, String controllerIdentifier,
                                                  Integer ttl);

    ApiResponse<String> getGroupControllerLock(String groupId, String accountId);

    ApiResponse<Integer> unlockGroupController(String groupId, String accountId);

}
