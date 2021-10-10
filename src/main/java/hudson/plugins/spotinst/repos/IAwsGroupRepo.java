package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.aws.AwsGroupInstance;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.util.Secret;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IAwsGroupRepo {
    ApiResponse<List<AwsGroupInstance>> getGroupInstances(String groupId, String accountId, Secret token);

    ApiResponse<Boolean> detachInstance(String instanceId, String accountId, Secret token);

    ApiResponse<AwsScaleUpResult> scaleUp(String groupId, Integer adjustment, String accountId, Secret token);
}
