package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.spotinst.api.SpotinstApi;
import hudson.plugins.spotinst.common.CloudProviderEnum;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.slaves.Cloud;
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
    public String            spotinstToken;
    public CloudProviderEnum cloudProvider;
    //endregion

    public SpotinstTokenConfig() {
        load();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        spotinstToken = json.getString("spotinstToken");
        cloudProvider = CloudProviderEnum.fromValue(json.getString("cloudProvider"));
        save();
        SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        return true;
    }

    private static int validateToken(CloudProviderEnum cloudProvider, String token) {
        int retVal = SpotinstApi.getInstance().validateToken(cloudProvider, token);
        return retVal;
    }
    public FormValidation doValidateToken(@QueryParameter("spotinstToken") String spotinstToken, @QueryParameter("cloudProvider") CloudProviderEnum cloudProvider) {
        int isValid = validateToken(cloudProvider, spotinstToken);

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

    public CloudProviderEnum getCloudProvider() {
        return cloudProvider;
    }
}