package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IAzureGroupRepo {
    ApiResponse<List<AzureGroupInstance>> getGroupInstances(String groupId);

    ApiResponse<Boolean> detachInstance(String groupId, String instanceId);

    ApiResponse<Boolean> scaleUp(String groupId, Integer adjustment);
}
