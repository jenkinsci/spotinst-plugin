package hudson.plugins.spotinst.credentials;



import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.CheckForNull;

public class SpotTokenCredentialsImpl extends BaseStandardCredentials
        implements SpotTokenCredentials {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private final String id;

    @DataBoundConstructor
    public SpotTokenCredentialsImpl(
            @CheckForNull String id,
            @CheckForNull String description,
            Secret secret, CredentialsScope scope) {

        super(scope, id, description);
        this.secret = secret;
        this.id = id;
    }

    @Override
    public Secret getSecret() {
        return secret;
    }

    public String getDisplayName() {
        return id;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        static { }

        @Override
        public String getDisplayName() {
            return "Spot Personal Access Token";
        }

        @Override
        public String getIconClassName() {
            return "icon-spot-credentials";
        }
    }
}
