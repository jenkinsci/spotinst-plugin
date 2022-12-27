package hudson.plugins.spotinst.cloud;

import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.spotinst.common.AwsInstanceTypeEnum;
import hudson.plugins.spotinst.common.AwsInstanceTypeSearchMethodEnum;
import hudson.plugins.spotinst.common.SpotAwsInstanceTypesHelper;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;
import java.util.stream.Stream;

import static hudson.plugins.spotinst.api.SpotinstApi.validateToken;

/**
 * Created by ohadmuchnik on 18/07/2016.
 */
public class SpotinstInstanceWeight implements Describable<SpotinstInstanceWeight> {
    //region Members
    private Integer                         executors;
    private String                          awsInstanceTypeFromAPI;
    private String                          awsInstanceTypeFromAPISearch;
    private AwsInstanceTypeSearchMethodEnum searchMethod;
    //Deprecated
    private AwsInstanceTypeEnum             awsInstanceType;
    //endregion

    //region Constructors
    @DataBoundConstructor
    public SpotinstInstanceWeight(AwsInstanceTypeEnum awsInstanceType, Integer executors) {
        this.awsInstanceType = awsInstanceType;
        this.executors = executors;
    }
    //endregion

    //region Overrides
    @Override
    public Descriptor<SpotinstInstanceWeight> getDescriptor() {
        Descriptor<SpotinstInstanceWeight> retVal = Jenkins.getInstance().getDescriptor(getClass());

        if (retVal == null) {
            throw new RuntimeException("Descriptor of type SpotinstInstanceWeight cannot be null");
        }

        return retVal;
    }
    //endregion

    //region Methods
    public String getAwsInstanceTypeFromAPIInput() {
        String                          type;
        AwsInstanceTypeSearchMethodEnum searchMethod = getSearchMethod();

        if (searchMethod == AwsInstanceTypeSearchMethodEnum.SEARCH) {
            type = getAwsInstanceTypeFromAPISearch();
        }
        else {
            type = getAwsInstanceTypeFromAPI();
        }

        return type;
    }
    //endregion

    //region Private Methods
    private String getAwsInstanceTypeByName(String awsInstanceTypeFromAPIName) {
        String retVal = null;

        if (awsInstanceTypeFromAPIName != null) {

            /*
            If the user Previously chosen was a type that not exist in the hard coded list
             and did not configure the token right, we will present the chosen type and set the default vCPU to 1
             The descriptor of this class will show a warning message will note the user that something is wrong,
             and point to authentication fix before saving this configuration.
             */
            List<AwsInstanceType> types        = SpotAwsInstanceTypesHelper.getAllInstanceTypes();
            boolean               isTypeInList =
                    types.stream().anyMatch(i -> i.getInstanceType().equals(awsInstanceTypeFromAPIName));

            if (isTypeInList == false && getSearchMethod() != AwsInstanceTypeSearchMethodEnum.SEARCH) {
                AwsInstanceType instanceType = new AwsInstanceType();
                instanceType.setInstanceType(awsInstanceTypeFromAPIName);
                instanceType.setvCPU(1);
                SpotinstContext.getInstance().getAwsInstanceTypes().add(instanceType);
            }

            retVal = awsInstanceTypeFromAPIName;

        }
        else {
            if (awsInstanceType != null) {
                retVal = awsInstanceType.getValue();
            }
        }

        return retVal;
    }
    //endregion

    //region Classes
    @Extension
    public static final class DescriptorImpl extends Descriptor<SpotinstInstanceWeight> {

        @Override
        public String getDisplayName() {
            return "Spot Instance Weight";
        }

        public ListBoxModel doFillAwsInstanceTypeFromAPIItems() {
            ListBoxModel          retVal           = new ListBoxModel();
            List<AwsInstanceType> allInstanceTypes = SpotAwsInstanceTypesHelper.getAllInstanceTypes();

            if (allInstanceTypes != null) {
                for (AwsInstanceType instanceType : allInstanceTypes) {
                    retVal.add(instanceType.getInstanceType());
                }
            }

            return retVal;
        }

        public AutoCompletionCandidates doAutoCompleteAwsInstanceTypeFromAPISearch(@QueryParameter String value) {
            AutoCompletionCandidates retVal              = new AutoCompletionCandidates();
            List<AwsInstanceType>    allAwsInstanceTypes = SpotAwsInstanceTypesHelper.getAllInstanceTypes();
            Stream<String> allTypes =
                    allAwsInstanceTypes.stream().map(awsInstanceType -> awsInstanceType.getInstanceType());
            Stream<String> matchingTypes = allTypes.filter(type -> type.startsWith(value));
            matchingTypes.forEach(retVal::add);

            return retVal;
        }

        public FormValidation doCheckAwsInstanceTypeFromAPI() {
            FormValidation retVal = CheckAccountIdAndToken();

            return retVal;
        }

        public FormValidation doCheckAwsInstanceTypeFromAPISearch() {
            FormValidation retVal = CheckAccountIdAndToken();

            return retVal;
        }

        private FormValidation CheckAccountIdAndToken() {
            FormValidation retVal = null;

            String  accountId                 = SpotinstContext.getInstance().getAccountId();
            String  token                     = SpotinstContext.getInstance().getSpotinstToken();
            int     isValid                   = validateToken(token, accountId);
            boolean isInstanceTypesListUpdate = SpotAwsInstanceTypesHelper.isInstanceTypesListUpdate();

            if (isValid != 0 || isInstanceTypesListUpdate == false) {
                retVal = FormValidation.error(
                        "Usage of this configuration might not work as expected. In order to get the up-to-date instance types please check the Spot token on the “Configure System” page.");
            }

            return retVal;
        }
    }
    //endregion

    //region Getters / Setters
    public Integer getExecutors() {
        return executors;
    }

    //Deprecated
    public AwsInstanceTypeEnum getAwsInstanceType() {
        return awsInstanceType;
    }

    public String getAwsInstanceTypeFromAPI() {
        String retVal = getAwsInstanceTypeByName(this.awsInstanceTypeFromAPI);

        return retVal;
    }

    @DataBoundSetter
    public void setAwsInstanceTypeFromAPI(String awsInstanceTypeFromAPI) {
        this.awsInstanceTypeFromAPI = awsInstanceTypeFromAPI;

        if(searchMethod != AwsInstanceTypeSearchMethodEnum.SEARCH){
            this.awsInstanceTypeFromAPISearch = awsInstanceTypeFromAPI;
        }
    }

    public String getAwsInstanceTypeFromAPISearch() {
        String retVal;

        if (searchMethod == AwsInstanceTypeSearchMethodEnum.SEARCH) {
            retVal = getAwsInstanceTypeByName(this.awsInstanceTypeFromAPISearch);
        }
        else {
            retVal = this.awsInstanceTypeFromAPISearch;
        }

        return retVal;
    }

    @DataBoundSetter
    public void setAwsInstanceTypeFromAPISearch(String awsInstanceTypeFromAPISearch) {
        this.awsInstanceTypeFromAPISearch = awsInstanceTypeFromAPISearch;

        if(searchMethod == AwsInstanceTypeSearchMethodEnum.SEARCH){
            this.awsInstanceTypeFromAPI = awsInstanceTypeFromAPISearch;
        }
    }

    public AwsInstanceTypeSearchMethodEnum getSearchMethod() {
        AwsInstanceTypeSearchMethodEnum retVal = AwsInstanceTypeSearchMethodEnum.SELECT;

        if (searchMethod != null) {
            retVal = searchMethod;
        }

        return retVal;
    }

    @DataBoundSetter
    public void setSearchMethod(AwsInstanceTypeSearchMethodEnum searchMethod) {

        if (searchMethod == null) {
            this.searchMethod = AwsInstanceTypeSearchMethodEnum.SELECT;
        }
        else {
            this.searchMethod = searchMethod;
        }
    }
    //endregion
}
