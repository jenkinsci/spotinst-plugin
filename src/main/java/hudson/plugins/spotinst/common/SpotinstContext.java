package hudson.plugins.spotinst.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private String orchestratorIdentifier;
    private Map<String,String> groupsInUse;
    private PassiveExpiringMap<String, String> suspendedGroupFetching;
    private static final Integer SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS = 1000 * 60 * 2;
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

    public String getOrchestratorIdentifier() {
        if(orchestratorIdentifier == null){
            orchestratorIdentifier = RandomStringUtils.randomAlphanumeric(10);
        }
        return orchestratorIdentifier;
    }

    public  PassiveExpiringMap<String,String> getSuspendedGroupFetching() {
        if (suspendedGroupFetching == null) {
            suspendedGroupFetching = new PassiveExpiringMap<>(SUSPENDED_GROUP_FETCHING_TIME_TO_LIVE_IN_MILLIS);
        }
        return suspendedGroupFetching;
    }

    public Map<String,String> getGroupsInUse() {
        if (groupsInUse == null) {
            groupsInUse = new HashMap<>();
        }
        return groupsInUse;
    }
    //endregion

}
