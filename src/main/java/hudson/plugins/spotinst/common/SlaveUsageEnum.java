package hudson.plugins.spotinst.common;

import hudson.model.Node;

/**
 * Created by ohadmuchnik on 29/08/2016.
 */
public enum SlaveUsageEnum {
    NORMAL("NORMAL"),
    EXCLUSIVE("EXCLUSIVE");

    private String name;

    SlaveUsageEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SlaveUsageEnum fromName(String name) {
        SlaveUsageEnum retVal = null;
        for (SlaveUsageEnum usageEnum : SlaveUsageEnum.values()) {
            if (usageEnum.name.equals(name)) {
                retVal = usageEnum;
                break;
            }
        }

        return retVal;
    }

    public Node.Mode toMode() {
        Node.Mode retVal;
        if (this.equals(EXCLUSIVE)) {
            retVal = Node.Mode.EXCLUSIVE;
        }
        else {
            retVal = Node.Mode.NORMAL;
        }
        return retVal;
    }

    public static SlaveUsageEnum fromMode(Node.Mode mode) {
        SlaveUsageEnum retVal;

        if (mode.equals(Node.Mode.EXCLUSIVE)) {
            retVal = SlaveUsageEnum.EXCLUSIVE;
        }
        else {
            retVal = SlaveUsageEnum.NORMAL;
        }

        return retVal;
    }
}
