package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;

public class LockRepo implements ILockRepo {

    @Override
    public ApiResponse<String> acquireGroupControllerLock(String groupId, String accountId, String controllerIdentifier,
                                                          Integer ttl) {
        ApiResponse<String> retVal;

        try {
            String lockResult = SpotinstApi.AcquireLockGroupController(groupId, accountId, controllerIdentifier, ttl);

            retVal = new ApiResponse<>(lockResult);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<String> setExpiry(String groupId, String accountId, String controllerIdentifier, Integer ttl) {
        ApiResponse<String> retVal;

        try {
            String lockResult = SpotinstApi.ExpandGroupControllerLock(groupId, accountId, controllerIdentifier, ttl);

            retVal = new ApiResponse<>(lockResult);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<String> getGroupControllerLockValue(String groupId, String accountId) {
        ApiResponse<String> retVal;

        try {
            String controllerIdentifier = SpotinstApi.getGroupLockValueById(groupId, accountId);
            retVal = new ApiResponse<>(controllerIdentifier);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Integer> deleteGroupControllerLock(String groupId, String accountId) {
        ApiResponse<Integer> retVal;

        try {
            Integer isKeyDeleted = SpotinstApi.UnlockGroupController(groupId, accountId);
            retVal = new ApiResponse<>(isKeyDeleted);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
