package hudson.plugins.spotinst.repos;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.model.aws.AwsGroup;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import hudson.plugins.spotinst.model.aws.AwsGroupInstance;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.plugins.spotinst.model.aws.stateful.AwsStatefulInstance;

import java.util.List;

/**
 * Created by ohadmuchnik on 05/11/2018.
 */
public interface IAwsGroupRepo {
    ApiResponse<AwsGroup> getGroup(String groupId, String accountId);

    ApiResponse<List<AwsGroupInstance>> getGroupInstances(String groupId, String accountId);

    ApiResponse<List<AwsStatefulInstance>> getStatefulInstances(String groupId, String accountId);

    ApiResponse<Boolean> detachInstance(String instanceId, String accountId);

    ApiResponse<Boolean> deallocateInstance(String groupId, String statefulInstanceId, String accountId);

    ApiResponse<AwsScaleUpResult> scaleUp(String groupId, Integer adjustment, String accountId);

    ApiResponse<List<AwsInstanceType>> getAllInstanceTypes(String accountId);
}
