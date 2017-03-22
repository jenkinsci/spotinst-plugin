package hudson.plugins.spotinst.common;

/**
 * Created by ohadmuchnik on 29/08/2016.
 */
public enum GcpMachineType {

    N1Standard1("n1-standard-1", 1),
    N1Standard2("n1-standard-2", 2),
    N1Standard4("n1-standard-4", 4),
    N1Standard8("n1-standard-8", 8),
    N1Standard16("n1-standard-16", 16),
    N1Standard32("n1-standard-32", 32),
    N1Highmem2("n1-highmem-2", 2),
    N1Highmem4("n1-highmem-4", 4),
    N1Highmem8("n1-highmem-8", 8),
    N1Highmem16("n1-highmem-16", 16),
    N1Highmem32("n1-highmem-32", 32),
    N1Highcpu2("n1-highcpu-2", 2),
    N1Highcpu4("n1-highcpu-4", 4),
    N1Highcpu8("n1-highcpu-8", 8),
    N1Highcpu16("n1-highcpu-16", 16),
    N1Highcpu32("n1-highcpu-32", 32),
    F1Micro("f1-micro", 1),
    G1Small("g1-small", 1);

    private String  name;
    private Integer executors;

    private GcpMachineType(String value, Integer executors) {
        this.name = value;
        this.executors = executors;
    }

    public String toString() {
        return this.name;
    }

    public String getName() {
        return name;
    }

    public Integer getExecutors() {
        return executors;
    }

    public static GcpMachineType fromValue(String value) {
        if (value != null && !"".equals(value)) {
            if ("n1-standard-1".equals(value)) {
                return N1Standard1;
            }
            else if ("n1-standard-2".equals(value)) {
                return N1Standard2;
            }
            else if ("n1-standard-4".equals(value)) {
                return N1Standard4;
            }
            else if ("n1-standard-8".equals(value)) {
                return N1Standard8;
            }
            else if ("n1-standard-16".equals(value)) {
                return N1Standard16;
            }
            else if ("n1-standard-32".equals(value)) {
                return N1Standard32;
            }
            else if ("n1-highmem-2".equals(value)) {
                return N1Highmem2;
            }
            else if ("n1-highmem-4".equals(value)) {
                return N1Highmem4;
            }
            else if ("n1-highmem-8".equals(value)) {
                return N1Highmem8;
            }
            else if ("n1-highmem-16".equals(value)) {
                return N1Highmem16;
            }
            else if ("n1-highmem-32".equals(value)) {
                return N1Highmem32;
            }
            else if ("n1-highcpu-2".equals(value)) {
                return N1Highcpu2;
            }
            else if ("n1-highcpu-4".equals(value)) {
                return N1Highcpu4;
            }
            else if ("n1-highcpu-8".equals(value)) {
                return N1Highcpu8;
            }
            else if ("n1-highcpu-16".equals(value)) {
                return N1Highcpu16;
            }
            else if ("n1-highcpu-32".equals(value)) {
                return N1Highcpu32;
            }
            else if ("f1-micro".equals(value)) {
                return F1Micro;
            }
            else if ("g1-small".equals(value)) {
                return G1Small;
            }
            else {
                return null;
            }
        }
        else {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }
    }
}
