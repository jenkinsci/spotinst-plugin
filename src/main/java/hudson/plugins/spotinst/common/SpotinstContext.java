package hudson.plugins.spotinst.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.plugins.spotinst.cloud.BaseSpotinstCloud;
import hudson.plugins.spotinst.model.aws.AwsInstanceType;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang.RandomStringUtils;

import java.util.*;

/**
 * Created by ohadmuchnik on 24/05/2016.
 */
public class SpotinstContext {

    //region Members
    private static SpotinstContext instance;
    private String spotinstToken;
    private String accountId;
    private List<AwsInstanceType> awsInstanceTypes;
    private Date awsInstanceTypesLastUpdate;
    private String controllerIdentifier;
    private Map<String,String> groupsManageByController;
    private List<String> groupsDoesNotManageByController;
    private PassiveExpiringMap<String, String> candidateGroupsForControllerOwnership;
    private Map<BaseSpotinstCloud, SpotinstCloudCommunicationState> cloudsInitializationState;

    private static final Integer SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS = 1000 * 60 * 4;
    //endregion

    public static SpotinstContext getInstance() {
        if (instance == null) {
            instance = new SpotinstContext();
        }
        return instance;
    }

    //region Public Methods
    public String getSpotinstToken() {
        return spotinstToken;
    }

    public void setSpotinstToken(String spotinstToken) {
        this.spotinstToken = spotinstToken;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /*
    To get most up-to-date instance types DO NOT use this getter, instead use SpotAwsInstanceTypesHelper.getAllInstanceTypes().
     */
    public List<AwsInstanceType> getAwsInstanceTypes() {
        return awsInstanceTypes;
    }

    public void setAwsInstanceTypes(List<AwsInstanceType> awsInstanceTypes) {
        this.awsInstanceTypes = awsInstanceTypes;
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
    public Date getAwsInstanceTypesLastUpdate() {
        return awsInstanceTypesLastUpdate;
    }

    @SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
    public void setAwsInstanceTypesLastUpdate(Date awsInstanceTypesLastUpdate) {
        this.awsInstanceTypesLastUpdate = awsInstanceTypesLastUpdate;
    }

    public String getControllerIdentifier() {
        if(controllerIdentifier == null){
            controllerIdentifier = RandomStringUtils.randomAlphanumeric(10);
        }

        return controllerIdentifier;
    }

    public  PassiveExpiringMap<String,String> getCandidateGroupsForControllerOwnership() {
        if (candidateGroupsForControllerOwnership == null) {
            candidateGroupsForControllerOwnership = new PassiveExpiringMap<>(SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS);
        }

        return candidateGroupsForControllerOwnership;
    }

    public Map<BaseSpotinstCloud, SpotinstCloudCommunicationState> getCloudsInitializationState() {
        if (cloudsInitializationState == null) {
            cloudsInitializationState = new HashMap<>();
        }

        return cloudsInitializationState;
    }

    //endregion

}
