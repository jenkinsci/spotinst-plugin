package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;

import java.util.List;

/**
 * Created by ohadmuchnik on 06/11/2018.
 */
public class GcpGroupRepo implements IGcpGroupRepo {
    @Override
    public ApiResponse<List<GcpGroupInstance>> getGroupInstances(String groupId) {
        ApiResponse<List<GcpGroupInstance>> retVal;

        try {
            List<GcpGroupInstance> instances = SpotinstApi.getGcpGroupInstances(groupId);

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
            Boolean isDetached = SpotinstApi.gcpDetachInstance(groupId, instanceId);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<GcpScaleUpResult> scaleUp(String groupId, Integer adjustment) {
        ApiResponse<GcpScaleUpResult> retVal;

        try {
            GcpScaleUpResult scaleUpResult = SpotinstApi.gcpScaleUp(groupId, adjustment);

            retVal = new ApiResponse<>(scaleUpResult);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
