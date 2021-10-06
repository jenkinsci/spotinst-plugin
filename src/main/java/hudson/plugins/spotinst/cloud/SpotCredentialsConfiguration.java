package hudson.plugins.spotinst.cloud;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.security.ACL;
import hudson.slaves.ComputerConnectorDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.*;

public class SpotCredentialsConfiguration
        extends AbstractDescribableImpl<SpotCredentialsConfiguration> implements ExtensionPoint {


    private final String credentialsId;


    @DataBoundConstructor
    public SpotCredentialsConfiguration(
            String credentialsId) {
        this.credentialsId = requireNonNull(credentialsId);

    }


    @Nullable
    public SpotSecretToken getCredentials() {
        return firstOrNull(
                lookupCredentials(
                        SpotSecretToken.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                withId(trimToEmpty(this.credentialsId)));
    }

    @Override
    public SpotCredentialsConfigurationDescriptor getDescriptor() {
        return (SpotCredentialsConfigurationDescriptor)super.getDescriptor();
    }
}
