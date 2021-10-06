package hudson.plugins.spotinst.cloud;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class SpotCredentialsConfigurationDescriptor extends Descriptor<SpotCredentialsConfiguration> {
    public static DescriptorExtensionList<SpotCredentialsConfiguration, SpotCredentialsConfigurationDescriptor> all() {
        return Jenkins.get().getDescriptorList(SpotCredentialsConfiguration.class);
    }
}