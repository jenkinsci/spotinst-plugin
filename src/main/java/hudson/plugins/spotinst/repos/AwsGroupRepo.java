package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.aws.AwsGroupInstance;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.util.Secret;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public class AwsGroupRepo implements IAwsGroupRepo {

    @Override
    public ApiResponse<List<AwsGroupInstance>> getGroupInstances(String groupId, String accountId, Secret token) {
        ApiResponse<List<AwsGroupInstance>> retVal;

        try {
            List<AwsGroupInstance> instances = SpotinstApi.getAwsGroupInstances(groupId, accountId, token);

            retVal = new ApiResponse<>(instances);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Boolean> detachInstance(String instanceId, String accountId, Secret token) {
        ApiResponse<Boolean> retVal;

        try {
            Boolean isDetached = SpotinstApi.awsDetachInstance(instanceId, accountId, token);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<AwsScaleUpResult> scaleUp(String groupId, Integer adjustment, String accountId, Secret token) {
        ApiResponse<AwsScaleUpResult> retVal;

        try {
            AwsScaleUpResult scaleUpResult = SpotinstApi.awsScaleUp(groupId, adjustment, accountId, token);
            retVal = new ApiResponse<>(scaleUpResult);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
