package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.azure.AzureV3GroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleResultNewVm;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
public interface IAzureV3GroupRepo {
    ApiResponse<List<AzureV3GroupVm>> getGroupVms(String groupId, String accountId);

    ApiResponse<Boolean> detachVM(String groupId, String vmId, String accountId);

    ApiResponse<List<AzureScaleResultNewVm>> scaleUp(String groupId, Integer adjustment, String accountId);
}
