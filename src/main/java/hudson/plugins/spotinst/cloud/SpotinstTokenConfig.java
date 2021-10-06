package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
@Extension
public class SpotinstTokenConfig extends GlobalConfiguration {
    //region Members
    private String spotinstToken;
    private String accountId;
    //endregion

    public SpotinstTokenConfig() {
        load();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        spotinstToken = json.getString("spotinstToken");
        accountId = json.getString("accountId");
        save();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
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
        SpotCredentialsConfiguration s = new SpotCredentialsConfiguration("liron-token-10");
        SpotSecretToken sp = s.getCredentials();
        Secret sec = sp.getSecret();
        String tok = sec.getPlainText();
        switch (isValid) {
            case 0: {
                result = FormValidation.okWithMarkup("<div style=\"color:green\">The token is valid " + tok + "</div>");
                break;
            }
            case 1: {
                result = FormValidation.error("Invalid token " + tok);
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
}