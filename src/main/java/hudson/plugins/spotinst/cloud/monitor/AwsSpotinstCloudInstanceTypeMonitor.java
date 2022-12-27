package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.AwsSpotinstCloud;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class AwsSpotinstCloudInstanceTypeMonitor extends AdministrativeMonitor {
    //region members
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSpotinstCloud.class);
    Map<String, List<String>> invalidInstances;
    //endregion

    //region Overrides
    @Override
    public boolean isActivated() {
        boolean retVal;
        invalidInstances = new HashMap<>();
        List<Cloud> clouds = Jenkins.getInstance().clouds;
        List<AwsSpotinstCloud> awsClouds = clouds.stream().filter(cloud -> cloud instanceof AwsSpotinstCloud)
                                                 .map(awsCloud -> (AwsSpotinstCloud) awsCloud)
                                                 .collect(Collectors.toList());

        awsClouds.forEach(awsCloud -> {
            String       elastigroupId = awsCloud.getGroupId();
            List<String> invalidTypes  = awsCloud.getInvalidInstanceTypes();

            if (CollectionUtils.isEmpty(invalidTypes) == false) {
                invalidInstances.put(elastigroupId, invalidTypes);
            }
        });

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
        return invalidInstances.isEmpty() == false;
    }
    //endregion

    //region getters & setters
    public String getInvalidInstances() {
        Stream<String> invalidInstancesForOutput = invalidInstances.keySet().stream().map(this::generateAlertMessage);
        String retVal = invalidInstancesForOutput.collect(Collectors.joining(", "));

        return retVal;
    }
    //endregion

    //region private Methods
    private String generateAlertMessage(String group) {
        StringBuilder retVal = new StringBuilder();
        retVal.append('\'').append(group).append('\'').append(": [");

        List<String> InvalidInstancesByGroup = invalidInstances.get(group);
        Stream<String> InvalidInstancesForAlert =
                InvalidInstancesByGroup.stream().map(invalidInstance -> '\'' + invalidInstance + '\'');

        String instances = InvalidInstancesForAlert.collect(Collectors.joining(", "));
        retVal.append(instances).append(']');
        return retVal.toString();
    }
    //region
}
