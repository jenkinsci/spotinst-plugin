package hudson.plugins.spotinst.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ohadmuchnik on 24/05/2016.
 */
public class SpotinstContext {

    //region Members
    private static SpotinstContext instance;
    private String spotinstToken;
    private Map<String, Map<String, Integer>> spotRequestWaiting;
    private Map<String, Map<String, Integer>> spotRequestInitiating;
    private Map<String, List<String>> offlineComputers;
    //endregion

    //region Constructor
    private SpotinstContext() {
        spotRequestWaiting = new HashMap<String, Map<String, Integer>>();
        spotRequestInitiating = new HashMap<String, Map<String, Integer>>();
        offlineComputers = new HashMap<String, List<String>>();
    }

    public static SpotinstContext getInstance() {
        if (instance == null) {
            instance = new SpotinstContext();
        }
        return instance;
    }
    //endregion

    //region Private Methods
    private void addToList(Map<String, Map<String, Integer>> list,
                           String label,
                           String spotRequestId,
                           Integer numOfExecutors) {
        if (list.containsKey(label) == false) {
            Map<String, Integer> value = new HashMap<String, Integer>();
            value.put(spotRequestId, numOfExecutors);
            list.put(label, value);
        } else {
            list.get(label).put(spotRequestId, numOfExecutors);
        }
    }

    //endregion

    //region Public Methods
    public String getSpotinstToken() {
        return spotinstToken;
    }

    public void setSpotinstToken(String spotinstToken) {
        this.spotinstToken = spotinstToken;
    }

    public void addSpotRequestToWaiting(String label, String spotRequestId, Integer numOfExecutors) {
        addToList(spotRequestWaiting, label, spotRequestId, numOfExecutors);
    }

    public void removeSpotRequestFromWaiting(String label, String spotRequestId) {
        this.spotRequestWaiting.get(label).remove(spotRequestId);
    }

    public Map<String, Map<String, Integer>> getSpotRequestWaiting() {
        return spotRequestWaiting;
    }

    public Map<String, Map<String, Integer>> getSpotRequestInitiating() {
        return spotRequestInitiating;
    }

    public void addSpotRequestToInitiating(String label, String spotRequestId, Integer numOfExecutors) {
        addToList(spotRequestInitiating, label, spotRequestId, numOfExecutors);
    }

    public void removeSpotRequestFromInitiating(String label, String spotRequestId) {
        this.spotRequestInitiating.get(label).remove(spotRequestId);
    }

    public void addToOfflineComputers(String elastigroupId, String instanceId) {
        if (offlineComputers.containsKey(elastigroupId) == false) {
            List<String> instances = new LinkedList<String>();
            instances.add(instanceId);
            offlineComputers.put(elastigroupId, instances);
        } else {
            offlineComputers.get(elastigroupId).add(instanceId);
        }
    }

    public void removeFromOfflineComputers(String elastigroupId, String instanceId) {
        this.offlineComputers.get(elastigroupId).remove(instanceId);
    }

    public Map<String, List<String>> getOfflineComputers() {
        return offlineComputers;
    }
    //endregion
}
