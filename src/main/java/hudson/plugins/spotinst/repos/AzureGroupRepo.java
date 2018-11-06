package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;

import java.util.List;

/**
 * Created by ohadmuchnik on 06/11/2018.
 */
public class AzureGroupRepo implements IAzureGroupRepo {
    @Override
    public ApiResponse<List<AzureGroupInstance>> getGroupInstances(String groupId) {
        ApiResponse<List<AzureGroupInstance>> retVal;

        try {
            List<AzureGroupInstance> instances = SpotinstApi.getAzureGroupInstances(groupId);

            retVal = new ApiResponse<>(instances);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Boolean> detachInstance(String groupId, String instanceId) {
        ApiResponse<Boolean> retVal;

        try {
            Boolean isDetached = SpotinstApi.azureDetachInstance(groupId, instanceId);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Boolean> scaleUp(String groupId, Integer adjustment) {
        ApiResponse<Boolean> retVal;

        try {
            Boolean isSccueed = SpotinstApi.azureScaleUp(groupId, adjustment);

            retVal = new ApiResponse<>(isSccueed);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
