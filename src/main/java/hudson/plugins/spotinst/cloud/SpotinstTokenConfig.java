package hudson.plugins.spotinst.cloud;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.common.CredentialsMethodEnum;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.credentials.SpotTokenCredentialsLoader;
import hudson.plugins.spotinst.credentials.SpotTokenCredentialsLoaderImpl;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
@Extension
public class SpotinstTokenConfig extends GlobalConfiguration {
    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstTokenConfig.class);

    private String spotinstToken;
    private String accountId;
    private CredentialsMethodEnum credentialsMethod;
    private String                credentialsId;
    //endregion

    public SpotinstTokenConfig() {
        load();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    public SpotinstTokenConfig(String credentialsId) {
        load();
        this.credentialsId = credentialsId;
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        spotinstToken = json.getString("spotinstToken");
        accountId = json.getString("accountId");
        credentialsId = json.getString("credentialsId");
        String credentialsStoreSpotToken = null;

        try {
        SpotTokenLoader            spotTokenLoader            = new SpotTokenLoader(credentialsId, credentialsId);
        SpotTokenCredentialsLoader spotTokenCredentialsLoader = spotTokenLoader.getAdminCredentials();
        Secret secret = spotTokenCredentialsLoader.getSecret();
        credentialsStoreSpotToken = secret.getPlainText();
        }
        catch (Exception e) {
            LOGGER.info(String.format("token was not loaded from credentials store."));
        }

        save();

        if(credentialsStoreSpotToken != null){
            SpotinstContext.getInstance().setSpotinstToken(credentialsStoreSpotToken);

        }
        else{
            SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        }

        SpotinstContext.getInstance().setAccountId(accountId);


        return true;
    }

    private static int validateToken(String token, String accountId) {
        int retVal = SpotinstApi.validateToken(token, accountId);
        return retVal;
    }

    public FormValidation doValidateToken(@QueryParameter("spotinstToken") String spotinstToken,
                                          @QueryParameter("accountId") String accountId) {
        int isValid = validateToken(spotinstToken, accountId);

        FormValidation result;
        switch (isValid) {
            case 0: {
                result = FormValidation.okWithMarkup("<div style=\"color:green\">The token is valid</div>");
                break;
            }
            case 1: {
                result = FormValidation.error("Invalid token");
                break;
            }
            default: {
                result = FormValidation.warning("Failed to process the validation, please try again");
                break;
            }
        }
        return result;
    }

    public String getSpotinstToken() {
        return spotinstToken;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setSpotinstToken(String spotinstToken) {
        //Secret secret = getSecret();

        this.spotinstToken = spotinstToken;
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    @DataBoundSetter
    public void setCredentialsMethod(CredentialsMethodEnum credentialsMethod) {
        this.credentialsMethod = credentialsMethod;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public CredentialsMethodEnum getCredentialsMethod() {
        return credentialsMethod;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @RequirePOST
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM, context, SpotTokenCredentialsLoader.class, Collections.emptyList(), CredentialsMatchers
                        .always());
    }

    public List getCredentialsDescriptors() {
        return Jenkins.get().getDescriptorList(Credentials.class).stream()
                      .filter(x -> x.isSubTypeOf(SpotTokenCredentialsLoaderImpl.class)).collect(Collectors.toList());
    }
}