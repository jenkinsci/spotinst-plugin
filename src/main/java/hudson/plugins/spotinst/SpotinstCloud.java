package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
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
    private synchronized List<SpotinstSlave> provisionSlaves(int excessWorkload, String label) {
        List<SpotinstSlave> slaves = new LinkedList<>();

        LOGGER.info("Scale up Elastigroup: " + groupId + " with " + excessWorkload + " instances");

        ScaleUpResult scaleUpResult = SpotinstGateway.scaleUp(groupId, excessWorkload);

        if (scaleUpResult != null) {
            if (scaleUpResult.getNewInstances() != null) {

                for (ScaleResultNewInstance instance : scaleUpResult.getNewInstances()) {
                    SpotinstSlave slave = buildSpotinstSlave(instance.getInstanceId(), groupId, instance.getInstanceType(), label, idleTerminationMinutes);
                    slaves.add(slave);
                    ((SpotinstComputer) slave.getComputer()).setLaunchTime(System.currentTimeMillis());
                }
            }
            if (scaleUpResult.getNewSpotRequests() != null) {

                for (ScaleResultNewSpot spot : scaleUpResult.getNewSpotRequests()) {
                    SpotinstSlave slave = buildSpotinstSlave(spot.getSpotInstanceRequestId(), groupId, spot.getInstanceType(), label, idleTerminationMinutes);
                    slaves.add(slave);
                    Integer numOfExecutors = SpotinstSlave.executorsForInstanceType(InstanceType.fromValue(spot.getInstanceType()));
                    SpotinstContext.getInstance().addSpotRequestToWaiting(label, spot.getSpotInstanceRequestId(), numOfExecutors);
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

    private int getNumOfSlavesNeeded(String label, int excessWorkload) {
        int retVal = 0;
        int currentWaitingExecutors = getCurrentWaitingExecutors(label);
        int currentInitiatingExecutors = getCurrentInitiatingExecutors(label);
        int currentExecutors = currentWaitingExecutors + currentInitiatingExecutors;


        if (excessWorkload > currentExecutors) {
            retVal = excessWorkload - currentExecutors;
        }

        return retVal;
    }

    private int getCurrentWaitingExecutors(String label) {
        int currentExecutors = 0;

        Map<String, Integer> waitingSpots = SpotinstContext.getInstance().getSpotRequestWaiting().get(label);
        if (waitingSpots != null) {
            Collection<Integer> waitingExecutors = SpotinstContext.getInstance().getSpotRequestWaiting().get(label).values();
            for (Integer numOfExecutors : waitingExecutors) {
                currentExecutors += numOfExecutors;
            }
        }

        return currentExecutors;
    }

    private int getCurrentInitiatingExecutors(String label) {
        int currentExecutors = 0;

        Map<String, Integer> initiatingSpots = SpotinstContext.getInstance().getSpotRequestInitiating().get(label);

        if (initiatingSpots != null) {
            Collection<Integer> initiatingExecutors = SpotinstContext.getInstance().getSpotRequestInitiating().get(label).values();
            for (Integer numOfExecutors : initiatingExecutors) {
                currentExecutors += numOfExecutors;
            }
        }

        return currentExecutors;
    }
    //endregion

    //region Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {

        int numOfSlavesNeeded = getNumOfSlavesNeeded(label.getName(), excessWorkload);

        if (numOfSlavesNeeded > 0) {
            List<SpotinstSlave> slaves = provisionSlaves(numOfSlavesNeeded, label.getName());

            if (slaves.size() > 0) {
                for (SpotinstSlave slave : slaves) {
                    try {
                        Jenkins.getInstance().addNode(slave);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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
