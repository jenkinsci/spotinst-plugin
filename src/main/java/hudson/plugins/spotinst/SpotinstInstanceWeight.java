package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.spotinst.common.AwsInstanceType;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
public class SpotinstInstanceWeight implements Describable<SpotinstInstanceWeight> {

    private AwsInstanceType awsInstanceType;
    private Integer executors;

    @DataBoundConstructor
    public SpotinstInstanceWeight(AwsInstanceType awsInstanceType, Integer executors) {
        this.awsInstanceType = awsInstanceType;
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

    public AwsInstanceType getAwsInstanceType() {
        return awsInstanceType;
    }
}
