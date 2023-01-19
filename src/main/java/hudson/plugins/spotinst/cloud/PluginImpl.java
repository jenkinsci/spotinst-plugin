package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class PluginImpl extends Plugin implements Describable<PluginImpl> {
    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    // Whether the SshHostKeyVerificationAdministrativeMonitor should show messages when we have templates using
    // accept-new or check-new-soft strategies
    private long dismissInsecureMessages;

    public void saveDismissInsecureMessages(long dismissInsecureMessages) {
        this.dismissInsecureMessages = dismissInsecureMessages;
        try {
            save();
        } catch (IOException io) {
            LOGGER.warning("There was a problem saving that you want to dismiss all messages related to insecure EC2 templates");
        }
    }

    public long getDismissInsecureMessages() {
        return dismissInsecureMessages;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    public static PluginImpl get() {
        return Jenkins.get().getPlugin(PluginImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PluginImpl> {
        @Override
        public String getDisplayName() {
            return "Spotinst PluginImpl";
        }
    }

    @Override
    public void postInitialize() throws IOException {
        // backward compatibility with the legacy class name
//        Jenkins.XSTREAM.alias("hudson.plugins.ec2.EC2Cloud", AmazonEC2Cloud.class);
//        Jenkins.XSTREAM.alias("hudson.plugins.ec2.EC2Slave", EC2OndemandSlave.class);
//        // backward compatibility with the legacy instance type
//        Jenkins.XSTREAM.registerConverter(new InstanceTypeConverter());

        load();
    }
}
