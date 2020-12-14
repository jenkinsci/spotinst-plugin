package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.azure.AzureGroupStatus;
import hudson.plugins.spotinst.model.azure.AzureGroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
public class AzureVmGroupRepo implements IAzureVmGroupRepo {

    @Override
    public ApiResponse<List<AzureGroupVm>> getGroupVms(String groupId, String accountId) {
        ApiResponse<List<AzureGroupVm>> retVal;

        try {
            AzureGroupStatus   groupStatus = SpotinstApi.getAzureVmGroupStatus(groupId, accountId);
            List<AzureGroupVm> vms         = groupStatus.getVms();
            retVal = new ApiResponse<>(vms);
        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<Boolean> detachVM(String groupId, String vmId, String accountId) {
        ApiResponse<Boolean> retVal;

        try {
            Boolean isDetached = SpotinstApi.AzureVmDetach(groupId, vmId, accountId);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<List<AzureScaleUpResultNewVm>> scaleUp(String groupId, Integer adjustment, String accountId) {
        ApiResponse<List<AzureScaleUpResultNewVm>> retVal;

        try {
            List<AzureScaleUpResultNewVm> scaleUpVms = SpotinstApi.AzureVmScaleUp(groupId, adjustment, accountId);

            retVal = new ApiResponse<>(scaleUpVms);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
