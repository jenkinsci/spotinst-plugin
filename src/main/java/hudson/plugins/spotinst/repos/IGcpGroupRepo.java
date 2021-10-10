package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.gcp.GcpGroupInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IGcpGroupRepo {
    ApiResponse<List<GcpGroupInstance>> getGroupInstances(String groupId, String accountId);

    ApiResponse<Boolean> detachInstance(String groupId, String instanceId, String accountId);

    ApiResponse<GcpScaleUpResult> scaleUp(String groupId, Integer adjustment, String accountId);
}
