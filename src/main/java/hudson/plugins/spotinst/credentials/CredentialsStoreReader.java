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

public class CredentialsStoreReader
        extends AbstractDescribableImpl<CredentialsStoreReader> {

    private final String credentialsId;
    private final String id;

    @DataBoundConstructor
    public CredentialsStoreReader(
            String credentialsId,
            @Nullable String id) {
        this.credentialsId = requireNonNull(credentialsId);
        this.id = isBlank(id) ? UUID.randomUUID().toString() : id;
    }

    public String getId() {
        return id;
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
