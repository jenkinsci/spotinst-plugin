package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.SpotSecretToken;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.util.Secret;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IGcpGroupRepo {
    ApiResponse<List<GcpGroupInstance>> getGroupInstances(String groupId, String accountId, Secret token);

    ApiResponse<Boolean> detachInstance(String groupId, String instanceId, String accountId, Secret token);

    ApiResponse<GcpScaleUpResult> scaleUp(String groupId, Integer adjustment, String accountId, Secret token);
}
