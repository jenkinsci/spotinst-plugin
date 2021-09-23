package hudson.plugins.spotinst.cloud;



import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

@NameWith(SpotTokenCredentials.NameProvider.class)
public interface SpotTokenCredentials extends StandardCredentials {

    Secret getSecret();

    class NameProvider extends CredentialsNameProvider<SpotTokenCredentialsImpl> {

        @Override
        public String getName(SpotTokenCredentialsImpl spotTokenCredentials) {
            return spotTokenCredentials.getDescription() + " - Spot token";
        }
    }
}
