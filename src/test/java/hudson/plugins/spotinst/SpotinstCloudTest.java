package hudson.plugins.spotinst;

import hudson.model.Node;
import hudson.plugins.spotinst.api.infra.ApiResponse;
import hudson.plugins.spotinst.cloud.*;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.common.AwsInstanceTypeEnum;
import hudson.plugins.spotinst.model.aws.AwsGroupInstance;
import hudson.plugins.spotinst.model.aws.AwsScaleResultNewSpot;
import hudson.plugins.spotinst.model.aws.AwsScaleUpResult;
import hudson.plugins.spotinst.model.azure.AzureScaleUpResultNewVm;
import hudson.plugins.spotinst.model.azure.AzureVmSizeEnum;
import hudson.plugins.spotinst.model.gcp.GcpMachineType;
import hudson.plugins.spotinst.model.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.model.gcp.GcpScaleUpResult;
import hudson.plugins.spotinst.repos.*;
import hudson.plugins.spotinst.slave.*;
import hudson.plugins.sshslaves.SSHConnector;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

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
                                     null, null, null, null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sir-1", buildPendingInstance("sir-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getAwsGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null);
    }

    @Test
    public void testAwsProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String groupId = "sig-1";
        AwsSpotinstCloud spotinstCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", false, true, "", null,
                                     null, null, null, null, null, null);

        jenkinsRule.jenkins.clouds.add(spotinstCloud);
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
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAwsGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture(), null);
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }

    @Test
    public void testAwCloud_whenNoConnectionMethodIsProvided_thenDefaultIsJNLP() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     null, null, false, null);
        jenkinsRule.jenkins.clouds.add(spotCloud);
        assertEquals(spotCloud.getConnectionMethod(), ConnectionMethodEnum.JNLP);

        AwsInstanceTypeEnum   vmSizeBasicA2 = AwsInstanceTypeEnum.C4Large;
        AwsScaleResultNewSpot newSpot       = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("spot");
        newSpot.setInstanceType(vmSizeBasicA2.getValue());

        List<AwsScaleResultNewSpot> spots  = Collections.singletonList(newSpot);
        AwsScaleUpResult            result = new AwsScaleUpResult();
        result.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));

        spotCloud.provision(null, 2);

        List<Node> allNodes = Jenkins.get().getNodes();

        for (Node node : allNodes) {
            SpotinstComputer computer = (SpotinstComputer) node.toComputer();
            assertEquals(computer.getLauncher().getClass(), SpotinstComputerLauncher.class);
        }

    }

    @Test
    public void testAwsCloud_whenSshConnectionMethod_andIpIsAvailable_thenCreateSshLauncher() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, null);
        jenkinsRule.jenkins.clouds.add(spotCloud);
        assertEquals(spotCloud.getConnectionMethod(), ConnectionMethodEnum.SSH);

        AwsInstanceTypeEnum   vmSizeBasicA2 = AwsInstanceTypeEnum.C4Large;
        AwsScaleResultNewSpot newSpot       = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("spot");
        newSpot.setInstanceType(vmSizeBasicA2.getValue());

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());
        incomingInstance.setPrivateIp(null);
        incomingInstance.setPublicIp("22.11.33.44");


        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        spotCloud.provision(null, 1);
        spotCloud.monitorInstances();

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(incomingInstance.getInstanceId());
        assertEquals(agent.getLauncher().getClass(), SpotSSHComputerLauncher.class);

    }

    @Test
    public void testAwsCloud_whenSshConnectionMethod_andIpIsNotAvailable_thenDoNotConnectYet() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, null);
        jenkinsRule.jenkins.clouds.add(spotCloud);
        assertEquals(spotCloud.getConnectionMethod(), ConnectionMethodEnum.SSH);

        AwsInstanceTypeEnum   vmSizeBasicA2 = AwsInstanceTypeEnum.C4Large;
        AwsScaleResultNewSpot newSpot       = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("spot");
        newSpot.setInstanceType(vmSizeBasicA2.getValue());

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());
        incomingInstance.setPrivateIp(null);
        incomingInstance.setPublicIp(null);


        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        spotCloud.provision(null, 1);

        spotCloud.monitorInstances();

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode("i-2");

        assertNotEquals(agent.getLauncher().getClass(), SpotSSHComputerLauncher.class);
        // but neither create our JNLP launcher
        assertNotEquals(agent.getLauncher().getClass(), SpotinstComputerLauncher.class);

    }

    @Test
    public void testAwCloud_whenUsePrivateIpIsNull_thenUsePublicIp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     null, null, null, null);

        assertEquals(spotCloud.getShouldUsePrivateIp(), false);
    }

    @Test
    public void testAwCloud_whenUsePrivateIpIsTrue_thenUsePrivateIp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     null, null, true, null);

        assertEquals(spotCloud.getShouldUsePrivateIp(), true);
    }

    @Test
    public void testAwCloud_whenUsePrivateIpIsFalse_thenUsePublicIp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     null, null, false, null);

        assertEquals(spotCloud.getShouldUsePrivateIp(), false);
    }
    //endregion

    //region GCP
    @Test
    public void testGcpProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                     null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sin-1", buildPendingInstance("sin-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getGcpGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null);
    }

    @Test
    public void testGcpProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String groupId = "sig-1";
        GcpSpotinstCloud spotinstCloud =
                new GcpSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                     null, null);
        jenkinsRule.jenkins.clouds.add(spotinstCloud);
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
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);

        ArgumentCaptor<String> accountIdCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(RepoManager.getInstance().getGcpGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture(), null);
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region Azure Scale Sets
    @Test
    public void testAzureProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, false, "", null, null, null, null,
                                       null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("q3213", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        pendingInstances.put("41234", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(RepoManager.getInstance().getAzureGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null);
    }

    @Test
    public void testAzureProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String groupId = "sig-1";
        AzureSpotinstCloud spotinstCloud =
                new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, false, "", null, null, null, null,
                                       null, null, null);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("asda", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        pendingInstances.put("ada", buildPendingInstance(groupId, PendingInstance.StatusEnum.PENDING, 1));
        spotinstCloud.setPendingInstances(pendingInstances);

        Mockito.when(RepoManager.getInstance().getAzureGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<Boolean>(new Boolean(true)));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAzureGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture(), null);
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }
    //endregion

    //region Azure VM
    @Test
    public void testAzureV3Provision_whenThereArePendingInstancesForAllExecutors_thenShouldNotScaleUp() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);

        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("vm-1", buildPendingInstance("vm-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.never())
               .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null);
    }

    @Test
    public void testAzureV3Provision_whenThereArePendingInstancesForPartOfTheExecutors_thenShouldScaleUpTheRest() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);
        jenkinsRule.jenkins.clouds.add(spotinstCloud);
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("vm-1", buildPendingInstance("vm-1", PendingInstance.StatusEnum.PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);

        AzureScaleUpResultNewVm newSpot = new AzureScaleUpResultNewVm();
        newSpot.setVmName("vm-2");
        newSpot.setLifeCycle("spot");
        newSpot.setVmSize(AzureVmSizeEnum.STANDARD_A1_V2.getValue());

        List<AzureScaleUpResultNewVm> vms = Collections.singletonList(newSpot);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String>  groupCapture     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture     = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String>  accountIdCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.times(1))
               .scaleUp(groupCapture.capture(), unitsCapture.capture(), accountIdCapture.capture(), null);

        assertEquals(unitsCapture.getValue().intValue(), 2);
    }

    @Test
    public void testAzureV3Provision_whenUnrecognizedVmSize_thenDefaultTo1Executor() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);
        jenkinsRule.jenkins.clouds.add(spotinstCloud);
        AzureScaleUpResultNewVm newSpot = new AzureScaleUpResultNewVm();
        newSpot.setVmName("vm-2");
        newSpot.setLifeCycle("spot");
        newSpot.setVmSize("iDontExistType");

        List<AzureScaleUpResultNewVm> vms = Collections.singletonList(newSpot);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
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
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);
        jenkinsRule.jenkins.clouds.add(spotinstCloud);
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
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
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
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);
        jenkinsRule.jenkins.clouds.add(spotinstCloud);
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
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(vms));

        spotinstCloud.provision(null, 3);
        spotinstCloud.provision(null, 2);

        Mockito.verify(RepoManager.getInstance().getAzureVmGroupRepo(), Mockito.times(1)).scaleUp(groupId, 1, null, null);
    }
    //endregion

    //region Test Cloud Common
    @Test
    public void testAzureV3Cloud_whenNoConnectionMethodIsProvided_thenDefaultIsJNLP() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotCloud =
                new AzureSpotCloud(groupId, "", "20", "/tmp", null, "", false, true, "", null, null, null, null, null,
                                   null, null);

        jenkinsRule.jenkins.clouds.add(spotCloud);
        assertEquals(spotCloud.getConnectionMethod(), ConnectionMethodEnum.JNLP);

        AzureVmSizeEnum         vmSizeBasicA2 = AzureVmSizeEnum.BASIC_A2;
        AzureScaleUpResultNewVm newSpot       = new AzureScaleUpResultNewVm();
        newSpot.setVmName("vm-2");
        newSpot.setLifeCycle("spot");
        newSpot.setVmSize(vmSizeBasicA2.getValue());

        List<AzureScaleUpResultNewVm> vms = Collections.singletonList(newSpot);

        Mockito.when(RepoManager.getInstance().getAzureVmGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(vms));

        spotCloud.provision(null, 2);

        List<Node> allNodes = Jenkins.get().getNodes();

        for (Node node : allNodes) {
            SpotinstComputer computer = (SpotinstComputer) node.toComputer();
            assertEquals(computer.getLauncher().getClass(), SpotinstComputerLauncher.class);
        }

    }
    // endregion

    // region Global Executor Override
    @Test
    public void testGlobalExecutorOverride_whenIsPassedAsNullInCloudConstructor_ThenDefaultIsNotEnabledAndExecutors1() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = null;
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        assertNotNull(cloud.getGlobalExecutorOverride());
        assertFalse(cloud.getGlobalExecutorOverride().getIsEnabled());
        assertEquals(cloud.getGlobalExecutorOverride().getExecutors().intValue(), 1);

    }

    //      enabled
    @Test
    public void testGlobalExecutorOverride_whenIsEnabledAndInstanceTypeIsMatchedInEnum_thenShouldUseGlobalOverrideValue() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, 23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), globalOverride.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsEnabledAndInstanceTypeIsNotMatchedInEnum_thenShouldUseGlobalOverrideValue() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, 23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large.unmatch");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), globalOverride.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsEnabledAndInstanceTypeIsInInstanceTypeWeights_thenShouldUseTypeWeight() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, 23);

        int expectedExecutors = 90;
        List<SpotinstInstanceWeight> executorForTypes =
                Collections.singletonList(new SpotinstInstanceWeight(AwsInstanceTypeEnum.C4Large, expectedExecutors));

        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", executorForTypes, null, "", true, null, null, null,
                                     null, null, ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), expectedExecutors);

    }


    //      disabled
    @Test
    public void testGlobalExecutorOverride_whenIsDisabledAndInstanceTypeIsNotMatchedInEnum_thenShouldUse1() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(false, 23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large.unmatch");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), 1);

    }

    @Test
    public void testGlobalExecutorOverride_whenIsDisabledAndInstanceTypeIsMatchedInEnum_thenShouldUseEnumValue() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(false, 23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), AwsInstanceTypeEnum.C4Large.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsDisabledAndInstanceTypeIsInInstanceTypeWeights_thenShouldUseTypeWeight() {
        String groupId = "sig-1";

        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(false, 23);

        int expectedExecutors = 90;
        List<SpotinstInstanceWeight> executorForTypes =
                Collections.singletonList(new SpotinstInstanceWeight(AwsInstanceTypeEnum.C4Large, expectedExecutors));

        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", executorForTypes, null, "", true, null, null, null,
                                     null, null, ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), expectedExecutors);

    }

    //      invalid
    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNegativeAndInstanceTypeIsMatchedInEnum_thenShouldUseEnumValue() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, -23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), AwsInstanceTypeEnum.C4Large.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNegativeAndInstanceTypeIsNotMatchedInEnum_thenShouldUse1() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, -23);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large.unmatched");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), 1);

    }

    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNegativeAndInstanceTypeIsInInstanceTypeWeights_thenShouldUseTypeWeight() {
        String groupId = "sig-1";

        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(false, -23);

        int expectedExecutors = 90;
        List<SpotinstInstanceWeight> executorForTypes =
                Collections.singletonList(new SpotinstInstanceWeight(AwsInstanceTypeEnum.C4Large, expectedExecutors));

        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", executorForTypes, null, "", true, null, null, null,
                                     null, null, ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), expectedExecutors);

    }

    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNullAndInstanceTypeIsMatchedInEnum_thenShouldUseEnumValue() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, null);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), AwsInstanceTypeEnum.C4Large.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNullAndInstanceTypeIsNotMatchedInEnum_thenShouldUse1() {
        String                     groupId        = "sig-1";
        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(true, null);
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large.unmatched");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), 1);

    }

    @Test
    public void testGlobalExecutorOverride_whenIsInvalidNullAndInstanceTypeIsInInstanceTypeWeights_thenShouldUseTypeWeight() {
        String groupId = "sig-1";

        SpotGlobalExecutorOverride globalOverride = new SpotGlobalExecutorOverride(false, null);

        int expectedExecutors = 90;
        List<SpotinstInstanceWeight> executorForTypes =
                Collections.singletonList(new SpotinstInstanceWeight(AwsInstanceTypeEnum.C4Large, expectedExecutors));

        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", executorForTypes, null, "", true, null, null, null,
                                     null, null, ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), expectedExecutors);

    }


    //      null
    @Test
    public void testGlobalExecutorOverride_whenIsNullAndInstanceTypeIsMatchedInEnum_thenShouldUseEnumValue() {
        String groupId = "sig-1";
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, null);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), AwsInstanceTypeEnum.C4Large.getExecutors().intValue());

    }

    @Test
    public void testGlobalExecutorOverride_whenIsNullAndInstanceTypeIsNotMatchedInEnum_thenShouldUse1() {
        String groupId = "sig-1";
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, null);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large.unmatched");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), 1);

    }

    @Test
    public void testGlobalExecutorOverride_whenIsNullAndInstanceTypeIsInInstanceTypeWeights_thenShouldUseTypeWeight() {
        String groupId = "sig-1";

        SpotGlobalExecutorOverride globalOverride = null;

        int expectedExecutors = 90;
        List<SpotinstInstanceWeight> executorForTypes =
                Collections.singletonList(new SpotinstInstanceWeight(AwsInstanceTypeEnum.C4Large, expectedExecutors));

        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", executorForTypes, null, "", true, null, null, null,
                                     null, null, ConnectionMethodEnum.SSH, getSSHConnector(), false, globalOverride);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsScaleResultNewSpot newSpot = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("c4.large");

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());
        assertEquals(agent.getNumExecutors(), expectedExecutors);

    }
    //endregion


    //region "SpotinstSlave"
    @Test
    public void testSpotinstSlaveTermination_ifAgentInPendingInstances_thenAgentIsRemovedFromPendingInstances() {
        String groupId = "sig-1";
        BaseSpotinstCloud cloud =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, null, "", true, null, null, null, null, null,
                                     ConnectionMethodEnum.SSH, getSSHConnector(), false, null);

        jenkinsRule.jenkins.clouds.add(cloud);
        AwsInstanceTypeEnum   vmSizeBasicA2 = AwsInstanceTypeEnum.C4Large;
        AwsScaleResultNewSpot newSpot       = new AwsScaleResultNewSpot();
        newSpot.setInstanceId("i-2");
        newSpot.setInstanceType("spot");
        newSpot.setInstanceType(vmSizeBasicA2.getValue());

        AwsGroupInstance incomingInstance = new AwsGroupInstance();
        incomingInstance.setInstanceId(newSpot.getInstanceId());
        incomingInstance.setInstanceType(newSpot.getInstanceType());

        List<AwsGroupInstance> result = Collections.singletonList(incomingInstance);

        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().getGroupInstances(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(result));


        List<AwsScaleResultNewSpot> spots         = Collections.singletonList(newSpot);
        AwsScaleUpResult            scaleUpResult = new AwsScaleUpResult();
        scaleUpResult.setNewSpotRequests(spots);

        Mockito.when(RepoManager.getInstance().getAwsGroupRepo()
                                .scaleUp(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(scaleUpResult));

        cloud.provision(null, 1);

        Boolean detachResult = true;
        Mockito.when(
                RepoManager.getInstance().getAwsGroupRepo().detachInstance(Mockito.anyString(), Mockito.anyString(), null))
               .thenReturn(new ApiResponse<>(detachResult));

        SpotinstSlave agent = (SpotinstSlave) Jenkins.get().getNode(newSpot.getInstanceId());

        assertEquals(cloud.isInstancePending(newSpot.getInstanceId()), true);
        agent.terminate();
        assertEquals(cloud.isInstancePending(newSpot.getInstanceId()), false);

    }
    //endregion

    //region Descriptors

    /**
     * Since we rely on this Descriptor for conditional rendering in Jelly template, it's good to verify it.
     */
    @Test
    public void testAzureSpotinstCloud_DescriptorReturnsAzureSpotinstCloudString() {
        String groupId = "sig-1";
        BaseSpotinstCloud spotinstCloud =
                new AzureSpotinstCloud(groupId, "", "20", "/tmp", null, "", false, false, "", null, null, null, null,
                                       null, null, null);

        assertTrue(spotinstCloud.getDescriptor().toString().contains("AzureSpotinstCloud"));
    }
    //endregion

    //region Helper Methods
    public SSHConnector getSSHConnector() {
        SSHConnector retVal = new SSHConnector(22, "testCredentials");
        return retVal;
    }
    //endregion
}