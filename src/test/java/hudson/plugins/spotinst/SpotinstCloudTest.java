package hudson.plugins.spotinst;

import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.*;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.common.AwsInstanceTypeEnum;
import hudson.plugins.spotinst.model.aws.AwsScaleResultNewSpot;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;
import hudson.plugins.spotinst.model.azure.AzureVmSizeEnum;
import hudson.plugins.spotinst.model.gcp.GcpMachineType;
import hudson.plugins.spotinst.model.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.repos.*;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by ohadmuchnik on 19/03/2017.
 */
public class SpotinstCloudTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() {
        IAwsGroupRepo     awsGroupRepo     = Mockito.mock(IAwsGroupRepo.class);
        IGcpGroupRepo     gcpGroupRepo     = Mockito.mock(IGcpGroupRepo.class);
        IAzureGroupRepo   azureGroupRepo   = Mockito.mock(IAzureGroupRepo.class);
        IAzureVmGroupRepo azureVmGroupRepo = Mockito.mock(IAzureVmGroupRepo.class);

        RepoManager.getInstance().setAwsGroupRepo(awsGroupRepo);
        RepoManager.getInstance().setGcpGroupRepo(gcpGroupRepo);
        RepoManager.getInstance().setAzureGroupRepo(azureGroupRepo);
        RepoManager.getInstance().setAzureVmGroupRepo(azureVmGroupRepo);

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
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", false, true, "", null,
                                     null, null);
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
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", false, true, "", null,
                                     null, null);
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
        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
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
        BaseSpotinstCloud            spotinstCloud    =
                new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);
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
        GcpSpotinstCloud             spotinstCloud    =
                new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sin-1", buildPendingInstance("sin-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);

        GcpScaleUpResult     result      = new GcpScaleUpResult();
        GcpResultNewInstance newInstance = new GcpResultNewInstance();
        newInstance.setInstanceName("sin-2");
        newInstance.setZone("us-east-1a");
        newInstance.setMachineType(GcpMachineType.F1Micro.getName());
        result.setNewInstances(Arrays.asList(newInstance));
        Mockito.when(RepoManager.getInstance().getGcpGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);

        ArgumentCaptor<String> accountIdCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(RepoManager.getInstance().getGcpGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region Azure
    @Test
    public void testAzureProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String                       groupId          = "sig-1";
        BaseSpotinstCloud            spotinstCloud    =
                new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, false, "", null, null, null);
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
        AzureSpotinstCloud           spotinstCloud    =
                new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, false, "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("asda", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        pendingInstances.put("ada", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        spotinstCloud.setPendingInstances(pendingInstances);

        Mockito.when(RepoManager.getInstance().getAzureGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<Boolean>(new Boolean(true)));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAzureGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region Azure V3
    @Test
    public void testAzureV3Provision_whenThereArePendingInstancesForAllExecutors_thenShouldNotScaleUp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);

        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("vm-1", buildPendingInstance("vm-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void testAzureV3Provision_whenThereArePendingInstancesForPartOfTheExecutors_thenShouldScaleUpTheRest() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("vm-1", buildPendingInstance("vm-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);

        AzureScaleUpResultNewVm newSpot = new AzureScaleUpResultNewVm();
        newSpot.setVmName("vm-2");
        newSpot.setLifeCycle("spot");
        newSpot.setVmSize(AzureVmSizeEnum.STANDARD_A1_V2.getValue());

        List<AzureScaleUpResultNewVm> vms = Collections.singletonList(newSpot);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture());

        assertEquals(unitsCapture.getValue().intValue(), 2);
    }

    @Test
    public void testAzureV3Provision_whenUnrecognizedVmSize_thenDefaultTo1Executor() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);

        AzureScaleUpResultNewVm newSpot = new AzureScaleUpResultNewVm();
        newSpot.setVmName("vm-2");
        newSpot.setLifeCycle("spot");
        newSpot.setVmSize("iDontExistType");

        List<AzureScaleUpResultNewVm> vms = Collections.singletonList(newSpot);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 4);

        Node node = Jenkins.get().getNode("vm-2");
        assertNotEquals(node, null);
        assertEquals(1, node.getNumExecutors());
    }

    @Test
    public void testAzureV3Provision_whenNewInstancesAreLaunched_thenTheirSizeIsAccountedForInNodes() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);

        AzureVmSizeEnum vmSizeBasicA1 = AzureVmSizeEnum.BASIC_A1;
        AzureVmSizeEnum vmSizeBasicA2 = AzureVmSizeEnum.BASIC_A2;

        AzureScaleUpResultNewVm newVm1 = new AzureScaleUpResultNewVm();
        newVm1.setVmName("vm-2");
        newVm1.setLifeCycle("spot");
        newVm1.setVmSize(vmSizeBasicA1.getValue());

        AzureScaleUpResultNewVm newVm2 = new AzureScaleUpResultNewVm();
        newVm2.setVmName("vm-3");
        newVm2.setLifeCycle("spot");
        newVm2.setVmSize(vmSizeBasicA2.getValue());

        List<AzureScaleUpResultNewVm> vms = Arrays.asList(newVm1, newVm2);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 4);

        List<Node> allNodes = Jenkins.get().getNodes();

        int numOfExecutorsInNodes     = allNodes.stream().map(Node::getNumExecutors).mapToInt(Integer::intValue).sum();
        int numOfExecutorsInInstances = vmSizeBasicA1.getExecutors() + vmSizeBasicA2.getExecutors();

        assertEquals(numOfExecutorsInInstances, numOfExecutorsInNodes);
    }

    @Test
    public void testAzureV3Provision_whenNewInstancesAreLaunched_thenTheirSizeIsAccountedForInPendingInstances() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("vm-1", buildPendingInstance("vm-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);

        AzureVmSizeEnum vmSizeBasicA1 = AzureVmSizeEnum.BASIC_A1;
        AzureVmSizeEnum vmSizeBasicA2 = AzureVmSizeEnum.BASIC_A2;

        AzureScaleUpResultNewVm newVm1 = new AzureScaleUpResultNewVm();
        newVm1.setVmName("vm-2");
        newVm1.setLifeCycle("spot");
        newVm1.setVmSize(vmSizeBasicA1.getValue());

        AzureScaleUpResultNewVm newVm2 = new AzureScaleUpResultNewVm();
        newVm2.setVmName("vm-3");
        newVm2.setLifeCycle("spot");
        newVm2.setVmSize(vmSizeBasicA2.getValue());

        List<AzureScaleUpResultNewVm> vms = Arrays.asList(newVm1, newVm2);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 3);
        spotinstCloud.provision(null, 2);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.times(1)).scaleUp(groupId, 1, null);
    }
    //endregion

}