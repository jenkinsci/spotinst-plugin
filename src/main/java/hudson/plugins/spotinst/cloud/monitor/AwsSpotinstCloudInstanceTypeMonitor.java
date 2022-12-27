package hudson.plugins.spotinst.cloud.monitor;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.plugins.spotinst.cloud.AwsSpotinstCloud;
import hudson.plugins.spotinst.common.SpotinstContext;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class AwsSpotinstCloudInstanceTypeMonitor extends AdministrativeMonitor {
    //region members
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSpotinstCloud.class);
    List<String> invalidInstances;
    //endregion

    //region Overrides
    @Override
    public boolean isActivated() {
        boolean     retVal                = false;
        Set<String> allCloudsInvalidTypes = findInvalidInstancesFromClouds();

        SpotinstContext.getInstance().setInvalidInstanceTypes(new ArrayList<>(allCloudsInvalidTypes));
        retVal = hasInvalidInstanceType();
        return retVal;
    }

    @Override
    public String getDisplayName() {
        return "Aws Spotinst Cloud Instance Type Monitor";
    }
    //endregion

    //region getters & setters
    public boolean hasInvalidInstanceType() {
        List<String> cachedInvalidInstances = getCachedInvalidInstance();
        boolean retVal = cachedInvalidInstances != null && cachedInvalidInstances.isEmpty() == false;
        return retVal;
    }

    public String getInvalidInstances() {
        invalidInstances = getCachedInvalidInstance();
        Stream<String> invalidInstancesForOutput = invalidInstances.stream().map(instance -> '\'' + instance + '\'');
        String         retVal                    = invalidInstancesForOutput.collect(Collectors.joining(", "));

        return retVal;
    }
    //endregion

    //region private Methods
    private Set<String> findInvalidInstancesFromClouds(){
        Set<String> retVal = new HashSet<>();
        Stream<AwsSpotinstCloud> awsClouds =
                Jenkins.getInstance().clouds.stream().filter(cloud -> cloud instanceof AwsSpotinstCloud)
                                            .map(awsCloud -> (AwsSpotinstCloud) awsCloud);

        awsClouds.forEach(awsCloud -> {
            List<String> invalidTypes = awsCloud.getInvalidInstanceTypes();
            retVal.addAll(invalidTypes);
        });

        return retVal;
    }

    private List<String> getCachedInvalidInstance() {
        List<String> retVal = SpotinstContext.getInstance().getInvalidInstanceTypes();

        return retVal;
    }
    //endregion
}
