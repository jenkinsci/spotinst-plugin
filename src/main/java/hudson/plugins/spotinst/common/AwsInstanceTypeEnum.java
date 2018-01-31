package hudson.plugins.spotinst.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ohadmuchnik on 26/05/2016.
 */
public enum AwsInstanceTypeEnum {

    T1Micro("t1.micro", 1),

    M1Small("m1.small", 1),

    M1Medium("m1.medium", 2),

    M1Large("m1.large", 4),

    M1Xlarge("m1.xlarge", 8),

    M3Medium("m3.medium", 1),

    M3Large("m3.large", 2),

    M3Xlarge("m3.xlarge", 4),

    M32xlarge("m3.2xlarge", 8),

    M4Large("m4.large", 2),

    M4Xlarge("m4.xlarge", 4),

    M42xlarge("m4.2xlarge", 8),

    M44xlarge("m4.4xlarge", 16),

    M410xlarge("m4.10xlarge", 40),

    T2Nano("t2.nano", 1),

    T2Micro("t2.micro", 1),

    T2Small("t2.small", 1),

    T2Medium("t2.medium", 2),

    T2Large("t2.large", 2),

    M2Xlarge("m2.xlarge", 6),

    M22xlarge("m2.2xlarge", 13),

    M24xlarge("m2.4xlarge", 26),

    Cr18xlarge("cr1.8xlarge", 88),

    I2Xlarge("i2.xlarge", 4),

    I22xlarge("i2.2xlarge", 8),

    I24xlarge("i2.4xlarge", 16),

    I28xlarge("i2.8xlarge", 32),

    Hi14xlarge("hi1.4xlarge", 35),

    Hs18xlarge("hs1.8xlarge", 35),

    C1Medium("c1.medium", 5),

    C1Xlarge("c1.xlarge", 20),

    C3Large("c3.large", 2),

    C3Xlarge("c3.xlarge", 4),

    C32xlarge("c3.2xlarge", 8),

    C34xlarge("c3.4xlarge", 16),

    C38xlarge("c3.8xlarge", 32),

    C4Large("c4.large", 2),

    C4Xlarge("c4.xlarge", 4),

    C42xlarge("c4.2xlarge", 8),

    C44xlarge("c4.4xlarge", 16),

    C48xlarge("c4.8xlarge", 32),

    Cc14xlarge("cc1.4xlarge", 1),

    Cc28xlarge("cc2.8xlarge", 1),

    G22xlarge("g2.2xlarge", 8),

    Cg14xlarge("cg1.4xlarge", 33),

    R3Large("r3.large", 2),

    R3Xlarge("r3.xlarge", 4),

    R32xlarge("r3.2xlarge", 8),

    R34xlarge("r3.4xlarge", 16),

    R38xlarge("r3.8xlarge", 32),

    D2Xlarge("d2.xlarge", 4),

    D22xlarge("d2.2xlarge", 8),

    D24xlarge("d2.4xlarge", 16),

    D28xlarge("d2.8xlarge", 32),

    G28xlarge("g2.8xlarge", 32),

    M416xlarge("m4.16xlarge", 64),

    P216xlarge("p2.16xlarge", 64),

    P28xlarge("p2.8xlarge", 32),

    P2xlarge("p2.xlarge", 4),

    R416xlarge("r4.16xlarge", 64),

    R42xlarge("r4.2xlarge", 8),

    R44xlarge("r4.4xlarge", 16),

    R48xlarge("r4.8xlarge", 32),

    R4large("r4.large", 2),

    R4xlarge("r4.xlarge", 4),

    T22xlarge("t2.2xlarge", 8),

    T2xlarge("t2.xlarge", 4),

    X116xlarge("x1.16xlarge", 64),

    X132xlarge("x1.32xlarge", 128),

    G34xlarge("g3.4xlarge", 16),
    G38xlarge("g3.8xlarge", 32),
    G316xlarge("g3.16xlarge", 64),
    F12xlarge("f1.2xlarge", 8),
    F116xlarge("f1.16xlarge", 64),
    I3large("i3.large", 2),
    I3xlarge("i3.xlarge", 4),
    I32xlarge("i3.2xlarge", 8),
    I34xlarge("i3.4xlarge", 16),
    I38xlarge("i3.8xlarge", 32),
    I316xlarge("i3.16xlarge", 64),
    P32xlarge("p3.2xlarge", 8),
    P38xlarge("p3.8xlarge", 32),
    P316xlarge("p3.16xlarge", 64),
    C5large("c5.large", 2),
    C5xlarge("c5.xlarge", 4),
    C52xlarge("c5.2xlarge", 8),
    C54xlarge("c5.4xlarge", 16),
    C59xlarge("c5.9xlarge", 36),
    C518xlarge("c5.18xlarge", 72),
    X1Exlarge("x1e.xlarge", 4),
    X1E2xlarge("x1e.2xlarge", 8),
    X1E4xlarge("x1e.4xlarge", 16),
    X1E8xlarge("x1e.8xlarge", 32),
    X1E16xlarge("x1e.16xlarge", 64),
    X1E32xlarge("x1e.32xlarge", 128),
    M5large("m5.large", 2),
    M5xlarge("m5.xlarge", 4),
    M52xlarge("m5.2xlarge", 8),
    M54xlarge("m5.4xlarge", 16),
    M512xlarge("m5.12xlarge", 48),
    M524xlarge("m5.24xlarge", 96),
    H12xlarge("h1.2xlarge", 8),
    H14xlarge("h1.4xlarge", 16),
    H18xlarge("h1.8xlarge", 32),
    H116xlarge("h1.16xlarge", 64);


    private String  value;
    private Integer executors;
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceTypeEnum.class);

    private AwsInstanceTypeEnum(String value, Integer executors) {
        this.value = value;
        this.executors = executors;
    }

    public String toString() {
        return this.value;
    }

    public Integer getExecutors() {
        return executors;
    }

    public static AwsInstanceTypeEnum fromValue(String value) {
        AwsInstanceTypeEnum retVal = null;
        for (AwsInstanceTypeEnum instanceType : AwsInstanceTypeEnum.values()) {
            if (instanceType.value.equals(value)) {
                retVal = instanceType;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.error("Tried to create instance type enum for: " + value + ", but we don't support such type ");
        }

        return retVal;
    }

    public String getValue() {
        return value;
    }
}
