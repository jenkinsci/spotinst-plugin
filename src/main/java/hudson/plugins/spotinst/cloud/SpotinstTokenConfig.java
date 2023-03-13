package hudson.plugins.spotinst.cloud;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.common.CredentialsMethodEnum;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.credentials.SpotTokenCredentials;
import hudson.plugins.spotinst.credentials.CredentialsStoreReader;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
@Extension
public class SpotinstTokenConfig extends GlobalConfiguration {
    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstTokenConfig.class);

    private String spotinstToken;
    private String accountId;
    private String credentialsMethod;
    private String credentialsId;
    //endregion

    public SpotinstTokenConfig() {
        load();
        String tokenToUse;
        boolean isPlanTextToken = credentialsMethod == null || credentialsMethod.equals(CredentialsMethodEnum.PlainText.getName());

        if (isPlanTextToken) {
            tokenToUse = spotinstToken;
        }

        else {
            tokenToUse = getCredentialsStoreSpotToken();
        }

        SpotinstContext.getInstance().setAccountId(accountId);
        SpotinstContext.getInstance().setSpotinstToken(tokenToUse);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        String tokenToUse;
        accountId = json.getString("accountId");
        boolean isSpecifiedCredentialMethod = json.has("credentialsMethod");

        if (isSpecifiedCredentialMethod) {
            credentialsMethod = json.getString("credentialsMethod");
        }
        else {
            credentialsMethod = CredentialsMethodEnum.PlainText.getName();
        }


        if (credentialsMethod.equals(CredentialsMethodEnum.CredentialsStore.getName())) {
            credentialsId = json.getString("credentialsId");
            tokenToUse = getCredentialsStoreSpotToken();
        }
        else {
            spotinstToken = json.getString("spotinstToken");
            tokenToUse = spotinstToken;
        }

        save();

        SpotinstContext.getInstance().setSpotinstToken(tokenToUse);
        SpotinstContext.getInstance().setAccountId(accountId);

        return true;
    }

    private String getCredentialsStoreSpotToken(){
        String retVal = null;
        CredentialsStoreReader credentialsStoreReader = new CredentialsStoreReader(credentialsId);
        SpotTokenCredentials   spotTokenCredentials   = credentialsStoreReader.getSpotToken();

        if (spotTokenCredentials != null) {
            Secret secret = spotTokenCredentials.getSecret();
            retVal = secret.getPlainText();
        }
        else {
            String failureMassage = "Failed to load token match to credentials ID: %s";
            LOGGER.error(String.format(failureMassage, credentialsId));
        }

        return retVal;
    }

    private static int validateToken(String token, String accountId) {
        int retVal = SpotinstApi.validateToken(token, accountId);
        return retVal;
    }

    public FormValidation doValidateCredentialsStoreToken(@QueryParameter("credentialsId") String credentialsId,
                                                          @QueryParameter("accountId") String accountId) {
        FormValidation result;

        if (credentialsId.equals("")) {
            result = FormValidation.error("Please choose credentials ID.");
        }
        else {
            CredentialsStoreReader credentialsStoreReader = new CredentialsStoreReader(credentialsId);
            SpotTokenCredentials   spotTokenCredentials   = credentialsStoreReader.getSpotToken();

            if (spotTokenCredentials != null) {
                Secret secret = spotTokenCredentials.getSecret();
                String token  = secret.getPlainText();
                result = doValidateToken(token, accountId);
            }
            else {
                result = FormValidation.error("Failed to load token match to credentials ID.");
            }
        }
        return result;
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
        String retToken;

        if(credentialsMethod == null || credentialsMethod.equals(CredentialsMethodEnum.PlainText.getName())){
            retToken = spotinstToken;
        }
        else{
            retToken = getCredentialsStoreSpotToken();
        }

        return retToken;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setSpotinstToken(String spotinstToken) {
        this.spotinstToken = spotinstToken;
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    public void setCredentialsMethod(String credentialsMethod) {
        this.credentialsMethod = credentialsMethod;
    }

    public String getCredentialsMethod() {
        return credentialsMethod;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @RequirePOST
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
        ListBoxModel retVal;

        retVal = new StandardListBoxModel().includeEmptyValue()
                                           .includeMatchingAs(ACL.SYSTEM, context, SpotTokenCredentials.class,
                                                              Collections.emptyList(), CredentialsMatchers.always());

        return retVal;
    }
}