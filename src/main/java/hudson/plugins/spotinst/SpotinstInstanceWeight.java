package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.spotinst.common.InstanceType;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
public class SpotinstInstanceWeight implements Describable<SpotinstInstanceWeight> {

    private InstanceType instanceType;
    private Integer executors;

    @DataBoundConstructor
    public SpotinstInstanceWeight(InstanceType instanceType, Integer executors) {
        this.instanceType = instanceType;
        this.executors = executors;
    }

    @Override
    public Descriptor<SpotinstInstanceWeight> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<SpotinstInstanceWeight> {

        @Override
        public String getDisplayName() {
            return null;
        }
    }


    public Integer getExecutors() {
        return executors;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }
}
