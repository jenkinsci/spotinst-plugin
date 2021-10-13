package hudson.plugins.spotinst.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

/**
 * Created by Liron Arad on 07/10/2021.
 */
@NameWith(SpotTokenCredentials.NameProvider.class)
public interface SpotTokenCredentials extends StandardCredentials {

    Secret getSecret();

    String getDisplayName();

    class NameProvider extends CredentialsNameProvider<SpotTokenCredentialsImpl> {

        @Override
        public String getName(SpotTokenCredentialsImpl spotTokenCredentials) {
            return spotTokenCredentials.getDisplayName();
        }
    }
}
