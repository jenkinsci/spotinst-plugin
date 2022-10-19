package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;

public class RedisRepo implements IRedisRepo {
    @Override
    public ApiResponse<String> setKey(String groupId, String accountId, String controllerIdentifier, Integer ttl) {
        ApiResponse<String> retVal;

        try {
            String isKeySet = SpotinstApi.setRedisKey(groupId, accountId, controllerIdentifier, ttl);

            retVal = new ApiResponse<>(isKeySet);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Object> getValue(String groupId, String accountId) {
        ApiResponse<Object> retVal;

        try {
            Object controllerIdentifier = SpotinstApi.getRedisValue(groupId, accountId);

            retVal = new ApiResponse<>(controllerIdentifier);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Integer> deleteKey(String groupId, String accountId) {
        ApiResponse<Integer> retVal;

        try {
            Integer isKeyDeleted = SpotinstApi.deleteRedisKey(groupId, accountId);

            retVal = new ApiResponse<>(isKeyDeleted);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
