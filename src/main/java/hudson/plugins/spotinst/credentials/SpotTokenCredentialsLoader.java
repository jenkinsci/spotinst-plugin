package hudson.plugins.spotinst.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

@NameWith(SpotTokenCredentialsLoader.NameProvider.class)
public interface SpotTokenCredentialsLoader extends StandardCredentials {

    Secret getSecret();

    class NameProvider extends CredentialsNameProvider<SpotTokenCredentialsLoaderImpl> {

        @Override
        public String getName(SpotTokenCredentialsLoaderImpl spotTokenCredentials) {
            return spotTokenCredentials.getDescription() + " - Spot token Loader";
        }
    }
}
