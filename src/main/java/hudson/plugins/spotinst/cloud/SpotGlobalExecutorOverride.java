package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Shibel Karmi Mansour on 10/07/2021.
 */
public class SpotGlobalExecutorOverride implements Describable<SpotGlobalExecutorOverride> {
    //region Members
    private boolean             isEnabled;
    private Integer             executors;
    //endregion

    //region Constructors
    @DataBoundConstructor
    public SpotGlobalExecutorOverride(boolean isEnabled, Integer executors) {
        this.isEnabled = isEnabled;
        this.executors = executors;
    }
    //endregion

    //region Overrides
    @Override
    public Descriptor<SpotGlobalExecutorOverride> getDescriptor() {
        Descriptor<SpotGlobalExecutorOverride> retVal = Jenkins.get().getDescriptor(getClass());

        if (retVal == null) {
            throw new RuntimeException("Descriptor of type SpotGlobalExecutorOverride cannot be null");
        }

        return retVal;
    }
    //endregion


    //region Classes
    @Extension
    public static final class DescriptorImpl extends Descriptor<SpotGlobalExecutorOverride> {

        @Override
        public String getDisplayName() {
            return "Spot Global Executor Override";
        }
    }
    //endregion

    //region Getters / Setters
    public Integer getExecutors() {
        return executors;
    }

    public boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    //endregion
}
