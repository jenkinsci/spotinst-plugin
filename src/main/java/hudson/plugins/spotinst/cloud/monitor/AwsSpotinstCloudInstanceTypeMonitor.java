package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.AwsSpotinstCloud;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class AwsSpotinstCloudInstanceTypeMonitor extends AdministrativeMonitor {
    //region members
    Map<String, List<String>> invalidInstancesByGroupId;
    //endregion

    //region Overrides
    @Override
    public boolean isActivated() {
        boolean retVal;
        initInvalidInstances();
        retVal = hasInvalidInstanceType();
        return retVal;
    }

    @Override
    public String getDisplayName() {
        return "Aws Spotinst Cloud Instance Type Monitor";
    }
    //endregion

    //region Methods
    public boolean hasInvalidInstanceType() {
        return invalidInstancesByGroupId.isEmpty() == false;
    }
    //endregion

    //region getters & setters
    public String getInvalidInstancesByGroupId() {
        Stream<String> invalidInstancesForOutput =
                invalidInstancesByGroupId.keySet().stream().map(this::generateAlertMessage);
        String retVal = invalidInstancesForOutput.collect(Collectors.joining(", "));

        return retVal;
    }
    //endregion

    //region private Methods
    private void initInvalidInstances() {
        invalidInstancesByGroupId = new HashMap<>();
        Jenkins jenkinsInstance = Jenkins.getInstance();
        List<Cloud> clouds = jenkinsInstance != null ? jenkinsInstance.clouds : new LinkedList<>();
        List<AwsSpotinstCloud> awsClouds = clouds.stream().filter(cloud -> cloud instanceof AwsSpotinstCloud)
                                                 .map(awsCloud -> (AwsSpotinstCloud) awsCloud)
                                                 .collect(Collectors.toList());

        awsClouds.forEach(awsCloud -> {
            String       elastigroupId = awsCloud.getGroupId();
            List<String> invalidTypes  = awsCloud.getInvalidInstanceTypes();

            if (CollectionUtils.isEmpty(invalidTypes) == false) {
                invalidInstancesByGroupId.put(elastigroupId, invalidTypes);
            }
        });
    }

    private String generateAlertMessage(String group) {
        StringBuilder retVal = new StringBuilder();
        retVal.append('\'').append(group).append('\'').append(": [");

        List<String> InvalidInstancesByGroup = invalidInstancesByGroupId.get(group);
        Stream<String> InvalidInstancesForAlert =
                InvalidInstancesByGroup.stream().map(invalidInstance -> '\'' + invalidInstance + '\'');

        String instances = InvalidInstancesForAlert.collect(Collectors.joining(", "));
        retVal.append(instances).append(']');
        return retVal.toString();
    }
    //region
}
