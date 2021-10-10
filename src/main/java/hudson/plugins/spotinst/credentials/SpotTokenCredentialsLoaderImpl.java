package hudson.plugins.spotinst.credentials;



import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.CheckForNull;

public class SpotTokenCredentialsLoaderImpl extends BaseStandardCredentials
        implements SpotTokenCredentialsLoader {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private final String id;
    private final String description;

    @DataBoundConstructor
    public SpotTokenCredentialsLoaderImpl(
            @CheckForNull String id,
            @CheckForNull String description,
            Secret secret, CredentialsScope scope) {

        super(scope, id, description);
        this.secret = secret;
        this.id = id;
        this.description = description;
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
            return "Spot secret Loader";
        }

        @Override
        public String getIconClassName() {
            return "icon-spot-credentials";
        }
    }
}
