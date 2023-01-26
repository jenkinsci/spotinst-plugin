package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;

public class LockRepo implements ILockRepo {

    @Override
    public ApiResponse<String> acquireGroupControllerLock(String accountId, String groupId, String controllerIdentifier,
                                                          Integer ttl) {
        ApiResponse<String> retVal;

        try {
            String lockResult = SpotinstApi.acquireGroupControllerLock(accountId, groupId, controllerIdentifier, ttl);

            retVal = new ApiResponse<>(lockResult);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<String> setGroupControllerLockExpiry(String accountId, String groupId,
                                                            String controllerIdentifier, Integer ttl) {
        ApiResponse<String> retVal;

        try {
            String lockResult = SpotinstApi.setGroupControllerLockExpiry(accountId, groupId, controllerIdentifier, ttl);

            retVal = new ApiResponse<>(lockResult);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<String> getGroupControllerLockValue(String accountId, String groupId) {
        ApiResponse<String> retVal;

        try {
            String controllerIdentifier = SpotinstApi.getGroupLockValueById(accountId, groupId);
            retVal = new ApiResponse<>(controllerIdentifier);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Integer> deleteGroupControllerLock(String accountId, String groupId) {
        ApiResponse<Integer> retVal;

        try {
            Integer isKeyDeleted = SpotinstApi.deleteGroupControllerLock(accountId, groupId);
            retVal = new ApiResponse<>(isKeyDeleted);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
