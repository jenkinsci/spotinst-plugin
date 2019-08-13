package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IAzureGroupRepo {
    ApiResponse<List<AzureGroupInstance>> getGroupInstances(String groupId, String accountId);

    ApiResponse<Boolean> detachInstance(String groupId, String instanceId, String accountId);

    ApiResponse<Boolean> scaleUp(String groupId, Integer adjustment, String accountId);
}
