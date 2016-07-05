package hudson.plugins.spotinst.common;

import java.util.*;

/**
 * Created by ohadmuchnik on 24/05/2016.
 */
public class SpotinstContext {

    //region Members
    private static SpotinstContext instance;
    private String spotinstToken;
    private Map<String, Map<String, ContextInstanceData>> spotRequestWaiting;
    private Map<String, Map<String, ContextInstanceData>> spotRequestInitiating;
    private Map<String, List<String>> offlineComputers;
    //endregion

    //region Constructor
    private SpotinstContext() {
        spotRequestWaiting = new HashMap<String, Map<String, ContextInstanceData>>();
        spotRequestInitiating = new HashMap<String, Map<String, ContextInstanceData>>();
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
    private void addToList(Map<String, Map<String, ContextInstanceData>> list,
                           String groupId,
                           String spotRequestId,
                           ContextInstanceData contextInstanceData) {
        if (list.containsKey(groupId) == false) {
            Map<String, ContextInstanceData> value = new HashMap<String, ContextInstanceData>();
            value.put(spotRequestId, contextInstanceData);
            list.put(groupId, value);
        } else {
            list.get(groupId).put(spotRequestId, contextInstanceData);
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

    public void addSpotRequestToWaiting(String groupId, String spotRequestId, Integer numOfExecutors) {
        ContextInstanceData contextInstanceData = new ContextInstanceData();
        contextInstanceData.setNumOfExecutors(numOfExecutors);
        contextInstanceData.setCreatedAt(new Date());
        addToList(spotRequestWaiting, groupId, spotRequestId, contextInstanceData);
    }

    public void removeSpotRequestFromWaiting(String groupId, String spotRequestId) {
        this.spotRequestWaiting.get(groupId).remove(spotRequestId);
    }

    public Map<String, Map<String, ContextInstanceData>> getSpotRequestWaiting() {
        return spotRequestWaiting;
    }

    public Map<String, Map<String, ContextInstanceData>> getSpotRequestInitiating() {
        return spotRequestInitiating;
    }

    public void addSpotRequestToInitiating(String groupId, String instanceId, Integer numOfExecutors) {
        ContextInstanceData contextInstanceData = new ContextInstanceData();
        contextInstanceData.setNumOfExecutors(numOfExecutors);
        contextInstanceData.setCreatedAt(new Date());
        addToList(spotRequestInitiating, groupId, instanceId, contextInstanceData);
    }

    public void removeSpotRequestFromInitiating(String groupId, String instanceId) {
        this.spotRequestInitiating.get(groupId).remove(instanceId);
    }

    public void addToOfflineComputers(String groupId, String instanceId) {
        if (offlineComputers.containsKey(groupId) == false) {
            List<String> instances = new LinkedList<String>();
            instances.add(instanceId);
            offlineComputers.put(groupId, instances);
        } else {
            offlineComputers.get(groupId).add(instanceId);
        }
    }

    public void removeFromOfflineComputers(String groupId, String instanceId) {
        this.offlineComputers.get(groupId).remove(instanceId);
    }

    public Map<String, List<String>> getOfflineComputers() {
        return offlineComputers;
    }
    //endregion
}
