package hudson.plugins.spotinst.credentials;



import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.plugins.spotinst.credentials.SpotTokenCredentials;
import hudson.security.ACL;
import hudson.util.Secret;
import io.jenkins.cli.shaded.org.apache.sshd.common.util.GenericUtils;
import jenkins.model.Jenkins;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.CredentialsScope.SYSTEM;

public class SpotTokenCredentialsImpl extends BaseStandardCredentials
        implements SpotTokenCredentials {

    private static final long serialVersionUID = 1L;

    private final Secret secret;
    private final String id;

    @DataBoundConstructor
    public SpotTokenCredentialsImpl(
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

        static {
            IconSet.icons.addIcon(
                    new Icon(
                            "icon-bitbucket-credentials icon-sm",
                            "atlassian-bitbucket-server-integration/images/16x16/credentials.png",
                            Icon.ICON_SMALL_STYLE,
                            IconType.PLUGIN));
            IconSet.icons.addIcon(
                    new Icon(
                            "icon-bitbucket-credentials icon-md",
                            "atlassian-bitbucket-server-integration/images/24x24/credentials.png",
                            Icon.ICON_MEDIUM_STYLE,
                            IconType.PLUGIN));
            IconSet.icons.addIcon(
                    new Icon(
                            "icon-bitbucket-credentials icon-lg",
                            "atlassian-bitbucket-server-integration/images/32x32/credentials.png",
                            Icon.ICON_LARGE_STYLE,
                            IconType.PLUGIN));
            IconSet.icons.addIcon(
                    new Icon(
                            "icon-bitbucket-credentials icon-xlg",
                            "atlassian-bitbucket-server-integration/images/48x48/credentials.png",
                            Icon.ICON_XLARGE_STYLE,
                            IconType.PLUGIN));
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

//        @Nullable
//        public SpotTokenCredentials getCredentialsId() {
//            return firstOrNull(
//                    lookupCredentials(
//                            SpotTokenCredentials.class,
//                            Jenkins.get(),
//                            ACL.SYSTEM,
//                            Collections.emptyList()),
//                    withId(GenericUtils.trimToEmpty("liron-token")));
//        }
}
