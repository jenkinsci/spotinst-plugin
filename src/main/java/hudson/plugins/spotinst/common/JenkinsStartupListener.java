package hudson.plugins.spotinst.common;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Extension
public class JenkinsStartupListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsStartupListener.class);

    @Initializer(after = InitMilestone.SYSTEM_CONFIG_LOADED)
    public static void onJenkinsStart() {
        LOGGER.info(
                "Jenkins has started. Handle Backward Compatibility for changing Jenkins version s.a Generating elastigroup names for spotinst clouds without elastigroup names...");
        Jenkins.get().clouds.stream().filter(cloud -> cloud instanceof BaseSpotinstCloud)
                            .map(cloud -> (BaseSpotinstCloud) cloud)
                            .forEach(BaseSpotinstCloud::handleBackwardCompatibility);

        try {
            Jenkins.get().save();
        }
        catch (IOException exception) {
            LOGGER.error("failed to update", exception);
        }
    }
}
