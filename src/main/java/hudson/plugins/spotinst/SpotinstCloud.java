package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.plugins.spotinst.common.ContextInstanceData;
import hudson.plugins.spotinst.common.InstanceType;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.common.SpotinstGateway;
import hudson.plugins.spotinst.scale.ScaleResultNewInstance;
import hudson.plugins.spotinst.scale.ScaleResultNewSpot;
import hudson.plugins.spotinst.scale.ScaleUpResult;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public class SpotinstCloud extends Cloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstCloud.class);
    private String groupId;
    private String labelString;
    private String idleTerminationMinutes;
    private Set<LabelAtom> labelSet;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public SpotinstCloud(String groupId, String labelString, String idleTerminationMinutes) {
        super(groupId);
        this.groupId = groupId;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        labelSet = Label.parse(labelString);

    }

    //endregion

    //region Private Methods
    private synchronized List<SpotinstSlave> provisionSlaves(int excessWorkload, Label label) {
        List<SpotinstSlave> slaves = new LinkedList<SpotinstSlave>();
        String labelString = null;
        if (label != null) {
            labelString = label.getName();
        }
        LOGGER.info("Scale up Elastigroup: " + groupId + " with " + excessWorkload + " workload units");

        ScaleUpResult scaleUpResult = SpotinstGateway.scaleUp(groupId, excessWorkload);

        if (scaleUpResult != null) {
            if (scaleUpResult.getNewInstances() != null) {
                LOGGER.info(scaleUpResult.getNewInstances().size() + " new instances launched");

                for (ScaleResultNewInstance instance : scaleUpResult.getNewInstances()) {
                    Integer numOfExecutors = SpotinstSlave.executorsForInstanceType(InstanceType.fromValue(instance.getInstanceType()));
                    SpotinstContext.getInstance().addSpotRequestToInitiating(groupId, instance.getInstanceId(), numOfExecutors);
                    SpotinstSlave slave = buildSpotinstSlave(instance.getInstanceId(), groupId, instance.getInstanceType(), labelString, idleTerminationMinutes);
                    slaves.add(slave);
                }
            }
            if (scaleUpResult.getNewSpotRequests() != null) {
                LOGGER.info(scaleUpResult.getNewSpotRequests().size() + " new spot requests created");
                for (ScaleResultNewSpot spot : scaleUpResult.getNewSpotRequests()) {
                    SpotinstSlave slave = buildSpotinstSlave(spot.getSpotInstanceRequestId(), groupId, spot.getInstanceType(), labelString, idleTerminationMinutes);
                    slaves.add(slave);
                    Integer numOfExecutors = SpotinstSlave.executorsForInstanceType(InstanceType.fromValue(spot.getInstanceType()));
                    SpotinstContext.getInstance().addSpotRequestToWaiting(groupId, spot.getSpotInstanceRequestId(), numOfExecutors);
                }
            }
        } else {
            LOGGER.error("Failed to scale up Elastigroup: " + groupId);
        }

        return slaves;
    }

    private SpotinstSlave buildSpotinstSlave(String newInstanceId, String elastigroupId, String instanceType, String label, String idleTerminationMinutes) {
        SpotinstSlave slave = null;
        try {
            slave = new SpotinstSlave(
                    newInstanceId,
                    elastigroupId,
                    newInstanceId,
                    instanceType,
                    label,
                    idleTerminationMinutes,
                    120);

        } catch (Descriptor.FormException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slave;
    }

    private SpotinstSlave callSlave(SpotinstSlave slave) {

        try {
            slave.toComputer().connect(false).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return slave;
    }

    private int getNumOfSlavesNeeded(int excessWorkload) {
        int retVal = 0;
        int currentWaitingExecutors = getCurrentWaitingExecutors();
        int currentInitiatingExecutors = getCurrentInitiatingExecutors();

        LOGGER.info("Weh have " + currentWaitingExecutors + " spot executors waiting to be launch and " + currentInitiatingExecutors + " instances executors that are initiating");
        int currentExecutors = currentWaitingExecutors + currentInitiatingExecutors;

        if (excessWorkload > currentExecutors) {
            retVal = excessWorkload - currentExecutors;
        }

        return retVal;
    }

    private int getCurrentWaitingExecutors() {
        int currentExecutors = 0;

        Map<String, ContextInstanceData> waitingSpots = SpotinstContext.getInstance().getSpotRequestWaiting().get(groupId);
        if (waitingSpots != null) {
            Collection<ContextInstanceData> waitingExecutors = waitingSpots.values();
            for (ContextInstanceData contextInstanceData : waitingExecutors) {
                currentExecutors += contextInstanceData.getNumOfExecutors();
            }
        }
        return currentExecutors;
    }

    private int getCurrentInitiatingExecutors() {
        int currentExecutors = 0;

        Map<String, ContextInstanceData> initiatingSpots = SpotinstContext.getInstance().getSpotRequestInitiating().get(groupId);

        if (initiatingSpots != null) {
            Collection<ContextInstanceData> initiatingExecutors = initiatingSpots.values();
            for (ContextInstanceData contextInstanceData : initiatingExecutors) {
                currentExecutors += contextInstanceData.getNumOfExecutors();
            }
        }
        return currentExecutors;
    }
    //endregion

    //region Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        LOGGER.info("Got provision slave request for workload: " + excessWorkload);
        int numOfSlavesNeeded = getNumOfSlavesNeeded(excessWorkload);

        if (numOfSlavesNeeded > 0) {
            LOGGER.info("Need to scale up " + numOfSlavesNeeded);

            List<SpotinstSlave> slaves = provisionSlaves(numOfSlavesNeeded, label);
            if (slaves.size() > 0) {
                for (SpotinstSlave slave : slaves) {
                    try {
                        Jenkins.getInstance().addNode(slave);
                    } catch (IOException e) {
                        LOGGER.error("Failed to create slave node");
                        e.printStackTrace();
                    }
                }
            }
        } else {
            LOGGER.info("No need to scale up new slaves, there are some that are initiating");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canProvision(Label label) {
        boolean canProvision = false;
        if (label == null ||
                label.matches(labelSet)) {
            canProvision = true;
        }

        return canProvision;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public String spotinstToken;

        public DescriptorImpl() {
            load();
            SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
        }

        @Override
        public String getDisplayName() {
            return "Spotinst";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            spotinstToken = json.getString("spotinstToken");
            save();
            SpotinstContext.getInstance().setSpotinstToken(spotinstToken);

            return true;
        }

        public FormValidation doValidateToken(@QueryParameter String spotinstToken) {
            FormValidation result;
            int isValid = SpotinstGateway.validateToken(spotinstToken);
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
    }

    public String getGroupId() {
        return groupId;
    }

    public String getLabelString() {
        return labelString;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getSpotinstToken() {
        return getDescriptor().spotinstToken;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }


    //endregion
}
