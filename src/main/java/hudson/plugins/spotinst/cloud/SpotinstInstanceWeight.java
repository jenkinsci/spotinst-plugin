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
    private Integer             executors;
    private String              awsInstanceTypeFromAPISelect;
    private String              awsInstanceTypeFromAPISearch;
    private AwsInstanceTypeSearchMethodEnum searchMethod;
    //Deprecated
    private AwsInstanceTypeEnum             awsInstanceType;
    //endregion

    //region Constructors
    @DataBoundConstructor
    public SpotinstInstanceWeight(AwsInstanceTypeEnum awsInstanceType, Integer executors,
                                  AwsInstanceTypeSearchMethodEnum searchMethod) {
        this.awsInstanceType = awsInstanceType;
        this.executors = executors;
        if(searchMethod != null) {
            this.searchMethod = searchMethod;
        }
        else{
            this.searchMethod = AwsInstanceTypeSearchMethodEnum.SELECT;
        }
    }

    public SpotinstInstanceWeight(AwsInstanceTypeEnum awsInstanceType, Integer executors) {
        this.awsInstanceType = awsInstanceType;
        this.executors = executors;
        this.searchMethod = AwsInstanceTypeSearchMethodEnum.SELECT;
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

    //region Classes
    @Extension
    public static final class DescriptorImpl extends Descriptor<SpotinstInstanceWeight> {

        @Override
        public String getDisplayName() {
            return "Spot Instance Weight";
        }

        public ListBoxModel doFillAwsInstanceTypeFromAPISelectItems() {
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
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            List<AwsInstanceType> allAwsInstanceTypes = SpotAwsInstanceTypesHelper.getAllInstanceTypes();
            Stream<String> allTypes      = allAwsInstanceTypes.stream()
                                                              .map(awsInstanceType -> awsInstanceType.getInstanceType().toLowerCase());
            String filterValue = value.toLowerCase();
            Object[] matchingTypes = allTypes.filter(type -> type.startsWith(filterValue)).toArray();

            for (Object objectType: matchingTypes) {
                String stringType = objectType.toString();
                c.add(stringType);
            }

            return c;
        }

        public FormValidation doCheckAwsInstanceTypeFromAPISelect() {
            FormValidation retVal = doCheckAwsInstanceTypeFromAPI();

            return retVal;
        }

        public FormValidation doCheckAwsInstanceTypeFromAPISearch() {
            FormValidation retVal = doCheckAwsInstanceTypeFromAPI();

            return retVal;
        }

        private FormValidation doCheckAwsInstanceTypeFromAPI() {
            FormValidation retVal = null;

            String  accountId                 = SpotinstContext.getInstance().getAccountId();
            String  token                     = SpotinstContext.getInstance().getSpotinstToken();
            int     isValid                   = validateToken(token, accountId);
            Boolean isInstanceTypesListUpdate = SpotAwsInstanceTypesHelper.isInstanceTypesListUpdate();

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

    @DataBoundSetter
    public void setAwsInstanceTypeFromAPISelect(String awsInstanceTypeFromAPISelect) {
        this.awsInstanceTypeFromAPISelect = awsInstanceTypeFromAPISelect;
    }

    public String getAwsInstanceTypeFromAPISelect() {
        String retVal = null;

        if (this.awsInstanceTypeFromAPISelect != null) {

            /*
            If the user Previously chosen was a type that not exist in the hard coded list
             and did not configure the token right, we will present the chosen type and set the default vCPU to 1
             The descriptor of this class will show a warning message will note the user that something is wrong,
             and point to authentication fix before saving this configuration.
             */
            List<AwsInstanceType> types = SpotAwsInstanceTypesHelper.getAllInstanceTypes();
            boolean isTypeInList = types.stream().anyMatch(i -> i.getInstanceType().equals(this.awsInstanceTypeFromAPISelect));

            if (isTypeInList == false) {
                AwsInstanceType instanceType = new AwsInstanceType();
                instanceType.setInstanceType(awsInstanceTypeFromAPISelect);
                instanceType.setvCPU(1);
                SpotinstContext.getInstance().getAwsInstanceTypes().add(instanceType);
            }

            retVal = awsInstanceTypeFromAPISelect;

        }
        else {
            if(awsInstanceType != null){
                retVal = awsInstanceType.getValue();
            }
        }

        return retVal;
    }

    public String getAwsInstanceTypeFromAPISearch() {
        String retVal = null;

        if (this.awsInstanceTypeFromAPISearch != null) {

            /*
            If the user Previously chosen was a type that not exist in the hard coded list
             and did not configure the token right, we will present the chosen type and set the default vCPU to 1
             The descriptor of this class will show a warning message will note the user that something is wrong,
             and point to authentication fix before saving this configuration.
             */
            List<AwsInstanceType> types = SpotAwsInstanceTypesHelper.getAllInstanceTypes();
            boolean isTypeInList = types.stream().anyMatch(i -> i.getInstanceType().equals(this.awsInstanceTypeFromAPISearch));

            if (isTypeInList == false) {
                AwsInstanceType instanceType = new AwsInstanceType();
                instanceType.setInstanceType(awsInstanceTypeFromAPISearch);
                instanceType.setvCPU(1);
                SpotinstContext.getInstance().getAwsInstanceTypes().add(instanceType);
            }

            retVal = awsInstanceTypeFromAPISearch;

        }
        else {
            if(awsInstanceType != null){
                retVal = awsInstanceType.getValue();
            }
        }

        return retVal;
    }
    @DataBoundSetter
    public void setAwsInstanceTypeFromAPISearch(String awsInstanceTypeFromAPISearch) {
        this.awsInstanceTypeFromAPISearch = awsInstanceTypeFromAPISearch;
    }

    public AwsInstanceTypeSearchMethodEnum getSearchMethod() {

        if(searchMethod == null){
            return AwsInstanceTypeSearchMethodEnum.SELECT;
        }
        return searchMethod;
    }
    @DataBoundSetter
    public void setSearchMethod(AwsInstanceTypeSearchMethodEnum searchMethod) {
        this.searchMethod = searchMethod;
    }
    //endregion
}
