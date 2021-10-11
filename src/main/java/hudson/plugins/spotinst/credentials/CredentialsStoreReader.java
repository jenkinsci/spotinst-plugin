package hudson.plugins.spotinst.credentials;

import hudson.model.AbstractDescribableImpl;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.UUID;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

/**
 * Created by Liron Arad on 07/10/2021.
 */
public class CredentialsStoreReader
        extends AbstractDescribableImpl<CredentialsStoreReader> {

    private final String credentialsId;
    private final String credentialsName;

    @DataBoundConstructor
    public CredentialsStoreReader(
            String credentialsId,
            @Nullable String credentialsName) {
        this.credentialsId = isBlank(credentialsId) ? UUID.randomUUID().toString() : credentialsId;
        this.credentialsName = isBlank(credentialsName) ? credentialsId : credentialsName;
    }

    public String getCredentialsName() {
        return credentialsName;
    }

    @Nullable
    public SpotTokenCredentials getSpotToken() {
        return firstOrNull(
                lookupCredentials(
                        SpotTokenCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                withId(trimToEmpty(credentialsId)));
    }
}
