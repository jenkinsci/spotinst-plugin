package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.azure.AzureGroupVm;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;

import java.util.List;

/**
 * Created by Shibel Karmi Mansour on 01/12/2020.
 */
public interface IAzureVmGroupRepo {
    ApiResponse<List<AzureGroupVm>> getGroupVms(String groupId, String accountId);

    ApiResponse<Boolean> detachVM(String groupId, String vmId, String accountId);

    ApiResponse<List<AzureScaleUpResultNewVm>> scaleUp(String groupId, Integer adjustment, String accountId);
}
