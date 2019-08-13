package hudson.plugins.spotinst;

import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.*;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.common.AwsInstanceTypeEnum;
import hudson.plugins.spotinst.model.aws.AwsScaleResultNewSpot;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.plugins.spotinst.model.gcp.GcpMachineType;
import hudson.plugins.spotinst.model.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.repos.IAwsGroupRepo;
import hudson.plugins.spotinst.repos.IAzureGroupRepo;
import hudson.plugins.spotinst.repos.IGcpGroupRepo;
import hudson.plugins.spotinst.repos.RepoManager;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by ohadmuchnik on 19/03/2017.
 */
public class SpotinstCloudTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() {
        IAwsGroupRepo   awsGroupRepo   = Mockito.mock(IAwsGroupRepo.class);
        IGcpGroupRepo   gcpGroupRepo   = Mockito.mock(IGcpGroupRepo.class);
        IAzureGroupRepo azureGroupRepo = Mockito.mock(IAzureGroupRepo.class);

        RepoManager.getInstance().setAwsGroupRepo(awsGroupRepo);
        RepoManager.getInstance().setGcpGroupRepo(gcpGroupRepo);
        RepoManager.getInstance().setAzureGroupRepo(azureGroupRepo);

        SpotinstContext.getInstance().setSpotinstToken("TOKEN");

    }

    private PendingInstance buildPendingInstance(String id, PendingInstance.StatusEnum status, Integer executors) {
        PendingInstance pendingInstance = new PendingInstance();
        pendingInstance.setCreatedAt(new Date());
        pendingInstance.setNumOfExecutors(executors);
        pendingInstance.setId(id);
        pendingInstance.setStatus(status);

        return pendingInstance;
    }

    //region AWS
    @Test
    public void testAwsProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sir-1", buildPendingInstance("sir-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getAwsGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testAwsProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String groupId = "sig-1";
        AwsSpotinstCloud spotinstCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", "" ,null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sir-1", buildPendingInstance("sir-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        AwsScaleUpResult      result  = new AwsScaleUpResult();
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setSpotInstanceRequestId("sir-2");
        newSpot.setInstanceId("i-dqwadq");
        newSpot.setAvailabilityZone("us-east-1a");
        newSpot.setInstanceType(AwsInstanceTypeEnum.C4Large.getValue());
        result.setNewSpotRequests(Arrays.asList(newSpot));
        Mockito.when(RepoManager.getInstance().getAwsGroupRepo().scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAwsGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region GCP
    @Test
    public void testGcpProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String                       groupId          = "sig-1";
        BaseSpotinstCloud            spotinstCloud    = new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sin-1", buildPendingInstance("sin-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getGcpGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testGcpProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String                       groupId          = "sig-1";
        GcpSpotinstCloud             spotinstCloud    = new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sin-1", buildPendingInstance("sin-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);

        GcpScaleUpResult     result      = new GcpScaleUpResult();
        GcpResultNewInstance newInstance = new GcpResultNewInstance();
        newInstance.setInstanceName("sin-2");
        newInstance.setZone("us-east-1a");
        newInstance.setMachineType(GcpMachineType.F1Micro.getName());
        result.setNewInstances(Arrays.asList(newInstance));
        Mockito.when(RepoManager.getInstance().getGcpGroupRepo().scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);

        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(RepoManager.getInstance().getGcpGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region Azure
    @Test
    public void testAzureProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String                       groupId          = "sig-1";
        BaseSpotinstCloud            spotinstCloud    = new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("q3213", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        pendingInstances.put("41234", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getAzureGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testAzureProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String                       groupId          = "sig-1";
        AzureSpotinstCloud           spotinstCloud    = new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("asda", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        pendingInstances.put("ada", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        spotinstCloud.setPendingInstances(pendingInstances);

        Mockito.when(RepoManager.getInstance().getAzureGroupRepo().scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<Boolean>(new Boolean(true)));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAzureGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

}