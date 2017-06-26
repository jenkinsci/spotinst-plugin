package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.util.FormValidation;
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
    public String spotinstToken;
    public String accountId;
    //endregion

    public SpotinstTokenConfig() {
        load();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        SpotinstContext.getInstance().setAccountId(accountId);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        spotinstToken = json.getString("spotinstToken");
        accountId = json.getString("accountId");
        save();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        SpotinstContext.getInstance().setAccountId(accountId);
        return true;
    }

    private static int validateToken(String token) {
        int retVal = SpotinstApi.getInstance().validateToken(token);
        return retVal;
    }

    public FormValidation doValidateToken(@QueryParameter("spotinstToken") String spotinstToken) {
        int isValid = validateToken(spotinstToken);

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
}