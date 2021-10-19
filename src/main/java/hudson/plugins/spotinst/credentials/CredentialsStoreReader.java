package hudson.plugins.spotinst.credentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.AbstractDescribableImpl;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.Nullable;
import java.util.Collections;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

/**
 * Created by Liron Arad on 07/10/2021.
 */
public class CredentialsStoreReader extends AbstractDescribableImpl<CredentialsStoreReader> {

    private final String credentialsId;

    @DataBoundConstructor
    public CredentialsStoreReader(@NonNull String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Nullable
    public SpotTokenCredentials getSpotToken() {
        SpotTokenCredentials retVal;

        retVal =  firstOrNull(
                lookupCredentials(SpotTokenCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
                withId(trimToEmpty(credentialsId)));

        return retVal;
    }
}
