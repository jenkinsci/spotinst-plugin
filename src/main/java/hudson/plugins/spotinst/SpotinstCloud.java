package hudson.plugins.spotinst;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.plugins.spotinst.common.*;
import hudson.plugins.spotinst.scale.aws.ScaleResultNewInstance;
import hudson.plugins.spotinst.scale.aws.ScaleResultNewSpot;
import hudson.plugins.spotinst.scale.aws.ScaleUpResult;
import hudson.plugins.spotinst.scale.gcp.GcpResultNewInstance;
import hudson.plugins.spotinst.scale.gcp.GcpScaleUpResult;
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

/**
 * Created by ohadmuchnik on 25/05/2016.
 */
public class SpotinstCloud extends Cloud {

    //region Members
    private static final Logger LOGGER = LoggerFactory.getLogger(SpotinstCloud.class);
    private String groupId;
    private String labelString;
    private String idleTerminationMinutes;
    private String workspaceDir;
    private Map<AwsInstanceType, Integer> executorsForInstanceType;
    private List<? extends SpotinstInstanceWeight> executorsForTypes;
    private Set<LabelAtom> labelSet;
    //endregion

    //region Constructor
    @DataBoundConstructor
    public SpotinstCloud(String groupId,
                         String labelString,
                         String idleTerminationMinutes,
                         String workspaceDir,
                         List<? extends SpotinstInstanceWeight> executorsForTypes) {
        super(groupId);
        this.groupId = groupId;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.workspaceDir = workspaceDir;
        labelSet = Label.parse(labelString);
        executorsForInstanceType = new HashMap<>();
        if (executorsForTypes != null) {
            this.executorsForTypes = executorsForTypes;
            for (SpotinstInstanceWeight executors : executorsForTypes) {
                if (executors.getExecutors() != null) {
                    executorsForInstanceType.put(executors.getAwsInstanceType(), executors.getExecutors());
                }
            }
        }
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

        if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
            gcpScaleUp(excessWorkload, slaves, labelString);
        } else {
            awsScaleUp(excessWorkload, slaves, labelString);
        }

        return slaves;
    }

    private void awsScaleUp(int excessWorkload, List<SpotinstSlave> slaves, String labelString) {

        ScaleUpResult scaleUpResult = SpotinstGateway.awsScaleUp(groupId, excessWorkload);

        if (scaleUpResult != null) {
            if (scaleUpResult.getNewInstances() != null) {
                handleOd(slaves, labelString, scaleUpResult);
            }
            if (scaleUpResult.getNewSpotRequests() != null) {
                handleSpot(slaves, labelString, scaleUpResult);
            }
        } else {
            LOGGER.error("Failed to scale up Elastigroup: " + groupId);
        }
    }

    private void gcpScaleUp(int excessWorkload, List<SpotinstSlave> slaves, String labelString) {

        GcpScaleUpResult scaleUpResult = SpotinstGateway.gcpScaleUp(groupId, excessWorkload);

        if (scaleUpResult != null) {
            if (scaleUpResult.getNewInstances() != null) {
                for (GcpResultNewInstance newInstance : scaleUpResult.getNewInstances()) {
                    handleNewGcpInstance(slaves, labelString, newInstance);
                }
            }

            if (scaleUpResult.getNewPreemptibles() != null) {
                for (GcpResultNewInstance newInstance : scaleUpResult.getNewPreemptibles()) {
                    handleNewGcpInstance(slaves, labelString, newInstance);
                }
            }
        } else {
            LOGGER.error("Failed to scale up Elastigroup: " + groupId);
        }
    }

    private void handleNewGcpInstance(List<SpotinstSlave> slaves, String labelString, GcpResultNewInstance newInstance) {
        Integer executors = GcpMachineType.fromValue(newInstance.getMachineType()).getExecutors();
        SpotinstContext.getInstance().addSpotRequestToInitiating(groupId, newInstance.getInstanceName(), executors, labelString);
        SpotinstSlave slave = buildSpotinstSlave(newInstance.getInstanceName(), groupId, newInstance.getMachineType(), labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
        slaves.add(slave);
    }

    private void handleSpot(List<SpotinstSlave> slaves, String labelString, ScaleUpResult scaleUpResult) {
        LOGGER.info(scaleUpResult.getNewSpotRequests().size() + " new spot requests created");
        for (ScaleResultNewSpot spot : scaleUpResult.getNewSpotRequests()) {
            Integer executors = getNumOfExecutors(spot.getInstanceType());
            SpotinstContext.getInstance().addSpotRequestToWaiting(groupId, spot.getSpotInstanceRequestId(), executors, labelString);
            SpotinstSlave slave = buildSpotinstSlave(spot.getSpotInstanceRequestId(), groupId, spot.getInstanceType(), labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
            slaves.add(slave);
        }
    }

    private void handleOd(List<SpotinstSlave> slaves, String labelString, ScaleUpResult scaleUpResult) {
        LOGGER.info(scaleUpResult.getNewInstances().size() + " new instances launched");
        for (ScaleResultNewInstance instance : scaleUpResult.getNewInstances()) {
            Integer executors = getNumOfExecutors(instance.getInstanceType());
            SpotinstContext.getInstance().addSpotRequestToInitiating(groupId, instance.getInstanceId(), executors, labelString);
            SpotinstSlave slave = buildSpotinstSlave(instance.getInstanceId(), groupId, instance.getInstanceType(), labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
            slaves.add(slave);
        }
    }

    public SpotinstSlave buildSpotSlave(String instanceType, String spotRequestId) {
        Integer executors = getNumOfExecutors(instanceType);
        SpotinstContext.getInstance().addSpotRequestToWaiting(groupId, spotRequestId, executors, labelString);
        SpotinstSlave slave = buildSpotinstSlave(spotRequestId, groupId, instanceType, labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
        return slave;
    }

    public SpotinstSlave buildInstanceSlave(String instanceType, String instanceId) {
        Integer executors = getNumOfExecutors(instanceType);
        SpotinstContext.getInstance().addSpotRequestToInitiating(groupId, instanceId, executors, labelString);
        SpotinstSlave slave = buildSpotinstSlave(instanceId, groupId, instanceType, labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
        return slave;
    }

    public SpotinstSlave buildGcpInstanceSlave(String machineType, String instanceName) {
        Integer executors = GcpMachineType.fromValue(machineType).getExecutors();
        SpotinstContext.getInstance().addSpotRequestToInitiating(groupId, instanceName, executors, labelString);
        SpotinstSlave slave = buildSpotinstSlave(instanceName, groupId, machineType, labelString, idleTerminationMinutes, workspaceDir, String.valueOf(executors));
        return slave;
    }


    private Integer getNumOfExecutors(String instanceType) {
        LOGGER.info("Determining the # of executors for instance type: " + instanceType);
        Integer retVal;
        AwsInstanceType type = AwsInstanceType.fromValue(instanceType);
        if (executorsForInstanceType.containsKey(type)) {
            retVal = executorsForInstanceType.get(type);
            LOGGER.info("We have a weight definition for this type of " + retVal);
        } else {
            retVal = SpotinstSlave.executorsForInstanceType(AwsInstanceType.fromValue(instanceType));
            LOGGER.info("Using the default value of " + retVal);
        }
        return retVal;
    }

    private SpotinstSlave buildSpotinstSlave(String newInstanceId,
                                             String elastigroupId,
                                             String instanceType,
                                             String label,
                                             String idleTerminationMinutes,
                                             String workspaceDir,
                                             String numOfExecutors) {
        SpotinstSlave slave = null;
        try {
            slave = new SpotinstSlave(
                    newInstanceId,
                    elastigroupId,
                    newInstanceId,
                    instanceType,
                    label,
                    idleTerminationMinutes,
                    workspaceDir,
                    numOfExecutors);

        } catch (Descriptor.FormException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slave;
    }

    private int getNumOfSlavesNeeded(int excessWorkload, Label label) {
        int retVal = 0;
        int currentWaitingExecutors = getCurrentWaitingExecutors(label);
        int currentInitiatingExecutors = getCurrentInitiatingExecutors(label);

        LOGGER.info("We have " + currentWaitingExecutors + " spot executors waiting to be launch and " + currentInitiatingExecutors + " instances executors that are initiating for group: " + groupId);
        int currentExecutors = currentWaitingExecutors + currentInitiatingExecutors;

        if (excessWorkload > currentExecutors) {
            retVal = excessWorkload - currentExecutors;
        }

        return retVal;
    }

    private int getCurrentWaitingExecutors(Label label) {
        Map<String, ContextInstance> waitingSpots = SpotinstContext.getInstance().getSpotRequestWaiting().get(groupId);
        int currentExecutors = getRelevantExecutors(label, waitingSpots);
        return currentExecutors;
    }

    private int getCurrentInitiatingExecutors(Label label) {
        Map<String, ContextInstance> initiatingSpots = SpotinstContext.getInstance().getSpotRequestInitiating().get(groupId);
        int currentExecutors = getRelevantExecutors(label, initiatingSpots);
        return currentExecutors;
    }

    private int getRelevantExecutors(Label label, Map<String, ContextInstance> contextInstances) {
        int currentExecutors = 0;
        if (contextInstances != null) {
            Collection<ContextInstance> initiatingExecutors = contextInstances.values();
            for (ContextInstance contextInstance : initiatingExecutors) {
                if ((label != null &&
                        label.getName().equals(contextInstance.getLabel())) ||
                        label == null) {
                    currentExecutors += contextInstance.getNumOfExecutors();
                }

            }
        }
        return currentExecutors;
    }
    //endregion

    //region Public Methods
    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        if (label != null) {
            LOGGER.info("Got provision slave request for workload: " + excessWorkload + " with label: " + label.getName());
        } else {
            LOGGER.info("Got provision slave request for workload: " + excessWorkload);
        }

        int numOfSlavesNeeded = getNumOfSlavesNeeded(excessWorkload, label);

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

    public Map<AwsInstanceType, Integer> getExecutorsForInstanceType() {
        return executorsForInstanceType;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public String spotinstToken;
        public CloudProviderEnum cloudProvider;

        public DescriptorImpl() {
            load();
            SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
            setCloudProvider();
        }

        @Override
        public String getDisplayName() {
            return "Spotinst";
        }

        private void setCloudProvider() {
            if (cloudProvider != null) {
                SpotinstContext.getInstance().setCloudProvider(cloudProvider);
            } else {
                SpotinstContext.getInstance().setCloudProvider(CloudProviderEnum.AWS);
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            spotinstToken = json.getString("spotinstToken");
            cloudProvider = CloudProviderEnum.fromValue(json.getString("cloudProvider"));
            save();
            SpotinstContext.getInstance().setSpotinstToken(spotinstToken);
            setCloudProvider();
            return true;
        }

        public FormValidation doValidateToken(@QueryParameter String spotinstToken) {
            int isValid;
            if (SpotinstContext.getInstance().getCloudProvider().equals(CloudProviderEnum.GCP)) {
                isValid = SpotinstGateway.gcpValidateToken(spotinstToken);
            } else {
                isValid = SpotinstGateway.awsValidateToken(spotinstToken);
            }

            FormValidation result;
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

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public List<? extends SpotinstInstanceWeight> getExecutorsForTypes() {
        return executorsForTypes;
    }

    //endregion
}
