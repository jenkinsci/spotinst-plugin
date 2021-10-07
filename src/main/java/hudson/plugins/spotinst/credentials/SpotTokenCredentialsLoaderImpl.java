package hudson.plugins.spotinst.credentials;



import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.CredentialsScope.SYSTEM;

public class SpotTokenCredentialsLoaderImpl extends BaseStandardCredentials
        implements SpotTokenCredentialsLoader {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private final String id;

    @DataBoundConstructor
    public SpotTokenCredentialsLoaderImpl(
            @CheckForNull String id,
            @CheckForNull String description,
            Secret secret) {

        super(SYSTEM, id, description);
        this.secret = secret;
        this.id = id;
    }

    @Override
    public Secret getSecret() {
        return secret;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        static { }

        @Override
        public String getDisplayName() {
            return "Spot token Loader";
        }

        @Override
        public String getIconClassName() {
            return "icon-spot-credentials";
        }
    }
}
