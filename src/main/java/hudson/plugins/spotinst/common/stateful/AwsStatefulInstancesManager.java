package hudson.plugins.spotinst.common.stateful;

import hudson.plugins.spotinst.model.aws.stateful.AwsStatefulInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sitay on 30/08/2013.
 */
public class AwsStatefulInstancesManager {
    //region members
    private static final Map<String, Map<String, AwsStatefulInstance>> awsStatefulInstanceBySsiByGroupId =
            new ConcurrentHashMap<>();
    //endregion

    //region getters & setters
    public static Map<String, Map<String, AwsStatefulInstance>> getAwsStatefulInstanceBySsiByGroupId() {
        return awsStatefulInstanceBySsiByGroupId;
    }
    //endregion
}
