package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.api.infra.ApiException;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.api.infra.ExceptionHelper;
import hudson.plugins.spotinst.model.azure.AzureV3GroupStatus;
import hudson.plugins.spotinst.model.azure.AzureV3GroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleResultNewVm;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
public class AzureV3GroupRepo implements IAzureV3GroupRepo {

    @Override
    public ApiResponse<List<AzureV3GroupVm>> getGroupVms(String groupId, String accountId) {
        ApiResponse<List<AzureV3GroupVm>> retVal;

        try {
            AzureV3GroupStatus   groupStatus = SpotinstApi.getAzureV3GroupStatus(groupId, accountId);
            List<AzureV3GroupVm> vms         = groupStatus.getVms();
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
            Boolean isDetached = SpotinstApi.azureV3DetachVm(groupId, vmId, accountId);

            retVal = new ApiResponse<>(isDetached);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }

    @Override
    public ApiResponse<List<AzureScaleResultNewVm>> scaleUp(String groupId, Integer adjustment, String accountId) {
        ApiResponse<List<AzureScaleResultNewVm>> retVal;

        try {
            List<AzureScaleResultNewVm> scaleUpVms = SpotinstApi.azureV3ScaleUp(groupId, adjustment, accountId);

            retVal = new ApiResponse<>(scaleUpVms);

        }
        catch (ApiException e) {
            retVal = ExceptionHelper.handleDalException(e);
        }

        return retVal;
    }
}
