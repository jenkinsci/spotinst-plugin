package hudson.plugins.spotinst.cloud;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.trilead.ssh2.Connection;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.ItemGroup;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.logging.Level;

import static com.cloudbees.plugins.credentials.CredentialsScope.SYSTEM;

public class SpotCredentialsImpl extends BaseStandardCredentials
        implements SpotSecretToken {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private       String credentialsId;

    @DataBoundConstructor
    public SpotCredentialsImpl(
            @edu.umd.cs.findbugs.annotations.CheckForNull CredentialsScope scope, @edu.umd.cs.findbugs.annotations.CheckForNull String id,
            @edu.umd.cs.findbugs.annotations.CheckForNull String credentialsId, @edu.umd.cs.findbugs.annotations.CheckForNull String secretKey, @CheckForNull String description) {

        super(scope, id, description);
        this.credentialsId = Util.fixNull(credentialsId);
        this.secret = Secret.fromString(secretKey);
    }

    public String getCredentialsId() {return credentialsId;}

    @Override
    public Secret getSecret() {
        return secret;
    }

    public String getDisplayName() {
        return credentialsId;
    }

    public SpotCredentials getCredentials() {
        SpotCredentials initialCredentials = new BasicSpotCredentials(credentialsId, secret.getPlainText());
        return initialCredentials;
    }
    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        public DescriptorImpl() {
        }


        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String credentialsId) {
            AccessControlled
                    _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get());
            if (_context == null || !_context.hasPermission(Computer.CONFIGURE)) {
                return new StandardUsernameListBoxModel()
                        .includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            context,
                            StandardUsernameCredentials.class,
                            Collections.singletonList(SSHLauncher.SSH_SCHEME),
                            SSHAuthenticator.matcher(Connection.class)
                                      )
                    .includeCurrentValue(credentialsId);
        }

        @Override
        public String getDisplayName() {
            return "Spot token";
        }

        @Override
        public String getIconClassName() {
            return "icon-spot-credentials";
        }
    }

}