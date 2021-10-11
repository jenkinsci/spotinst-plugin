package hudson.plugins.spotinst.credentials;


import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

/**
 * Created by Liron Arad on 07/10/2021.
 */
public class SpotTokenCredentialsImpl extends BaseStandardCredentials implements SpotTokenCredentials {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private final String id;
    private final String description;

    @DataBoundConstructor
    public SpotTokenCredentialsImpl(@CheckForNull String id, @CheckForNull String description, Secret secret,
                                    CredentialsScope scope) {

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

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    @NonNull
    @Override
    public String getDescription() {
        return description;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Spot Personal Access Token";
        }
    }
}
