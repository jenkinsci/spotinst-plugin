package hudson.plugins.spotinst.cloud;

import hudson.model.AbstractDescribableImpl;
import hudson.plugins.spotinst.credentials.SpotTokenCredentialsLoader;
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

public class SpotTokenLoader
        extends AbstractDescribableImpl<SpotTokenLoader> {

    private final String adminCredentialsId;
    private final String id;

    @DataBoundConstructor
    public SpotTokenLoader(
            String adminCredentialsId,
            @Nullable String id) {
        this.adminCredentialsId = requireNonNull(adminCredentialsId);
        this.id = isBlank(id) ? UUID.randomUUID().toString() : id;
    }

    public String getAdminCredentialsId() {
        return adminCredentialsId;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public SpotTokenCredentialsLoader getAdminCredentials() {
        return firstOrNull(
                lookupCredentials(
                        SpotTokenCredentialsLoader.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                withId(trimToEmpty("06.10|18:45")));
    }
}
