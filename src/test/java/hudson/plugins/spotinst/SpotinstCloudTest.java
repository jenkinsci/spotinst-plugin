package hudson.plugins.spotinst;

import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.cloud.AwsSpotinstCloud;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.cloud.PendingInstance;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.model.scale.aws.ScaleResultNewSpot;
import hudson.plugins.spotinst.model.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.slave.SlaveUsageEnum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by ohadmuchnik on 19/03/2017.
 */
public class SpotinstCloudTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() {
        SpotinstApi spotinstApi = Mockito.mock(SpotinstApi.class);
        SpotinstApi.setInstance(spotinstApi);

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

    @Test
    public void testProvision_whenThereArePendingInsatcnesForAllExecutors_thenShouldNotSacleUp() {
        String                       groupId          = "sig-1";
        BaseSpotinstCloud spotinstCloud    =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", "");
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sir-1", buildPendingInstance("sir-1", PendingInstance.StatusEnum.SPOT_PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        spotinstCloud.provision(null, 2);
        Mockito.verify(SpotinstApi.getInstance(), Mockito.never()).awsScaleUp(Mockito.anyString(), Mockito.anyInt());
    }

    @Test
    public void testProvision_whenThereArePendingInsatcnesForPartOfTheExecutors_thenShouldSacleUpTheRest() {
        String                       groupId          = "sig-1";
        AwsSpotinstCloud spotinstCloud    =
                new AwsSpotinstCloud(groupId, "", "20", "/tmp", null, SlaveUsageEnum.NORMAL, "", "");
        Map<String, PendingInstance> pendingInstances = new HashMap<>();
        pendingInstances.put("sir-1", buildPendingInstance("sir-1", PendingInstance.StatusEnum.SPOT_PENDING, 2));
        spotinstCloud.setPendingInstances(pendingInstances);
        ScaleUpResult      result  = new ScaleUpResult();
        ScaleResultNewSpot newSpot = new ScaleResultNewSpot();
        newSpot.setSpotInstanceRequestId("sir-2");
        newSpot.setAvailabilityZone("us-east-1a");
        newSpot.setInstanceType(AwsInstanceTypeEnum.C4Large.getValue());
        result.setNewSpotRequests(Arrays.asList(newSpot));
        Mockito.when(SpotinstApi.getInstance().awsScaleUp(Mockito.anyString(), Mockito.anyInt())).thenReturn(result);

        spotinstCloud.provision(null, 4);

        ArgumentCaptor<String> groupCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> unitsCapture = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(SpotinstApi.getInstance(), Mockito.times(1))
               .awsScaleUp(groupCapture.capture(), unitsCapture.capture());
        assertEquals(unitsCapture.getValue().intValue(), 2);
    }

}