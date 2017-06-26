package hudson.plugins.spotinst.api;

import hudson.plugins.spotinst.model.elastigroup.aws.AwsElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.azure.AzureElastigroupInstance;
import hudson.plugins.spotinst.model.elastigroup.gcp.GcpElastigroupInstance;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.model.scale.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.model.spot.SpotRequest;

import java.util.List;

/**
 * Created by ohadmuchnik on 19/03/2017.
 */
public interface ISpotinstApi {

    List<AwsElastigroupInstance> getAwsElastigroupInstances(String elastigroupId);

    ScaleUpResult awsScaleUp(String elastigroupId, int adjustment);

    SpotRequest getSpotRequest(String spotRequestId);

    boolean awsDetachInstance(String instanceId);

    int validateToken(String token);

    GcpScaleUpResult gcpScaleUp(String elastigroupId, int adjustment);

    boolean gcpDetachInstance(String groupId, String instanceName);

    List<GcpElastigroupInstance> getGcpElastigroupInstances(String elastigroupId);

    List<AzureElastigroupInstance> getAzureElastigroupInstances(String elastigroupId);

    boolean azureScaleUp(String elastigroupId, int adjustment);

    boolean azureDetachInstance(String elastigroupId, String instanceId);
}
