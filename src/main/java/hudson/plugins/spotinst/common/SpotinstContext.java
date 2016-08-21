package hudson.plugins.spotinst.common;

import java.util.*;

/**
 * Created by ohadmuchnik on 24/05/2016.
 */
public class SpotinstContext {

    //region Members
    private static SpotinstContext instance;
    private String spotinstToken;
    private Map<String, Map<String, ContextInstance>> spotRequestWaiting;
    private Map<String, Map<String, ContextInstance>> spotRequestInitiating;
    private Map<String, List<String>> offlineComputers;
    //endregion

    //region Constructor
    private SpotinstContext() {
        spotRequestWaiting = new HashMap<String, Map<String, ContextInstance>>();
        spotRequestInitiating = new HashMap<String, Map<String, ContextInstance>>();
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
    private void addToList(Map<String, Map<String, ContextInstance>> list,
                           String groupId,
                           String spotRequestId,
                           ContextInstance contextInstance) {
        if (list.containsKey(groupId) == false) {
            Map<String, ContextInstance> value = new HashMap<String, ContextInstance>();
            value.put(spotRequestId, contextInstance);
            list.put(groupId, value);
        } else {
            list.get(groupId).put(spotRequestId, contextInstance);
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

    public void addSpotRequestToWaiting(String groupId, String spotRequestId, Integer numOfExecutors, String label) {
        ContextInstance contextInstance = prepareInstanceContext(numOfExecutors, label);
        addToList(spotRequestWaiting, groupId, spotRequestId, contextInstance);
    }

    public void removeSpotRequestFromWaiting(String groupId, String spotRequestId) {
        this.spotRequestWaiting.get(groupId).remove(spotRequestId);
    }

    public Map<String, Map<String, ContextInstance>> getSpotRequestWaiting() {
        return spotRequestWaiting;
    }

    public Map<String, Map<String, ContextInstance>> getSpotRequestInitiating() {
        return spotRequestInitiating;
    }

    public void addSpotRequestToInitiating(String groupId, String instanceId, Integer numOfExecutors, String label) {
        ContextInstance contextInstance = prepareInstanceContext(numOfExecutors, label);
        addToList(spotRequestInitiating, groupId, instanceId, contextInstance);
    }

    private ContextInstance prepareInstanceContext(Integer numOfExecutors, String label) {
        ContextInstance contextInstance = new ContextInstance();
        contextInstance.setNumOfExecutors(numOfExecutors);
        contextInstance.setCreatedAt(new Date());
        if (label != null) {
            contextInstance.setLabel(label);
        }
        return contextInstance;
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

    public void cleanAll() {
        spotRequestWaiting = new HashMap<String, Map<String, ContextInstance>>();
        spotRequestInitiating = new HashMap<String, Map<String, ContextInstance>>();
    }
    //endregion
}
