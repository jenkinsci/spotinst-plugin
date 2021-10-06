package hudson.plugins.spotinst.cloud;



import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.util.Secret;

@NameWith(SpotSecretToken.NameProvider.class)
public interface SpotSecretToken extends StandardCredentials, SpotCredentialsProvider {

    Secret getSecret();
    String getDisplayName();
    class NameProvider extends CredentialsNameProvider<SpotSecretToken> {

        @NonNull
        @Override
        public String getName(@NonNull SpotSecretToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return c.getDisplayName() + (description != null ? " (" + description + ")" : "");
        }
    }
}
