package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.cloud.SpotSecretToken;
import hudson.plugins.spotinst.model.azure.AzureGroupStatus;
import hudson.plugins.spotinst.model.azure.AzureGroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;
import hudson.util.Secret;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
public class AzureVmGroupRepo implements IAzureVmGroupRepo {

    @Override
    public ApiResponse<List<AzureGroupVm>> getGroupVms(String groupId, String accountId, Secret token) {
        ApiResponse<List<AzureGroupVm>> retVal;

        try {
            AzureGroupStatus   groupStatus = SpotinstApi.getAzureVmGroupStatus(groupId, accountId, token);
            List<AzureGroupVm> vms         = groupStatus.getVms();
            retVal = new ApiResponse<>(vms);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Boolean> detachVM(String groupId, String vmId, String accountId, Secret token) {
        ApiResponse<Boolean> retVal;

        try {
            Boolean isDetached = SpotinstApi.azureVmDetach(groupId, vmId, accountId, token);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<List<AzureScaleUpResultNewVm>> scaleUp(String groupId, Integer adjustment, String accountId, Secret token) {
        ApiResponse<List<AzureScaleUpResultNewVm>> retVal;

        try {
            List<AzureScaleUpResultNewVm> scaleUpVms = SpotinstApi.azureVmScaleUp(groupId, adjustment, accountId, token);

            retVal = new ApiResponse<>(scaleUpVms);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
