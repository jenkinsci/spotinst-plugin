package hudson.plugins.spotinst.model.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ohadmuchnik on 21/06/2017.
 */
public enum AzureScaleSetSizeEnum {

    BASIC_A0("basic_a0", 1),
    BASIC_A1("basic_a1", 1),
    BASIC_A2("basic_a2", 2),
    BASIC_A3("basic_a3", 4),
    BASIC_A4("basic_a4", 8),
    STANDARD_A1_V2("standard_a1_v2", 1),
    STANDARD_A2_V2("standard_a2_v2", 2),
    STANDARD_A4_V2("standard_a4_v2", 4),
    STANDARD_A8_V2("standard_a8_v2", 8),
    STANDARD_A2M_V2("standard_a2m_v2", 2),
    STANDARD_A4M_V2("standard_a4m_v2", 4),
    STANDARD_A8M_V2("standard_a8m_v2", 8),
    STANDARD_D1_V2("standard_d1_v2", 1),
    STANDARD_D2_V2("standard_d2_v2", 2),
    STANDARD_D3_V2("standard_d3_v2", 4),
    STANDARD_D4_V2("standard_d4_v2", 8),
    STANDARD_D5_V2("standard_d5_v2", 16),
    STANDARD_D1("standard_d1", 1),
    STANDARD_D2("standard_d2", 2),
    STANDARD_D3("standard_d3", 4),
    STANDARD_D4("standard_d4", 8),
    STANDARD_A0("standard_a0", 1),
    STANDARD_A1("standard_a1", 1),
    STANDARD_A2("standard_a2", 2),
    STANDARD_A3("standard_a3", 4),
    STANDARD_A4("standard_a4", 8),
    STANDARD_A5("standard_a5", 2),
    STANDARD_A6("standard_a6", 4),
    STANDARD_A7("standard_a7", 8),
    STANDARD_F1("standard_f1", 1),
    STANDARD_F2("standard_f2", 2),
    STANDARD_F4("standard_f4", 4),
    STANDARD_F8("standard_f8", 8),
    STANDARD_F16("standard_f16", 16),
    STANDARD_D11_V2("standard_d11_v2", 2),
    STANDARD_D12_V2("standard_d12_v2", 4),
    STANDARD_D13_V2("standard_d13_v2", 8),
    STANDARD_D14_V2("standard_d14_v2", 16),
    STANDARD_D15_V2("standard_d15_v2", 20),
    STANDARD_D11("standard_d11", 2),
    STANDARD_D12("standard_d12", 4),
    STANDARD_D13("standard_d13", 8),
    STANDARD_D14("standard_d14", 16),
    STANDARD_G1("standard_g1", 2),
    STANDARD_G2("standard_g2", 4),
    STANDARD_G3("standard_g3", 8),
    STANDARD_G4("standard_g4", 16),
    STANDARD_G5("standard_g5", 32),
    STANDARD_L4("standard_l4", 4),
    STANDARD_L8("standard_l8", 8),
    STANDARD_L16("standard_l16", 16),
    STANDARD_L32("standard_l32", 32),
    STANDARD_NC6("standard_nc6", 6),
    STANDARD_NC12("standard_nc12", 12),
    STANDARD_NC24("standard_nc24", 24),
    STANDARD_NC24R("standard_nc24r", 24),
    STANDARD_NV6("standard_nv6", 6),
    STANDARD_NV12("standard_nv12", 12),
    STANDARD_NV24("standard_nv24", 24),
    STANDARD_H8("standard_h8", 8),
    STANDARD_H16("standard_h16", 16),
    STANDARD_H8M("standard_h8m", 8),
    STANDARD_H16M("standard_h16m", 16),
    STANDARD_H16MR("standard_h16mr", 16),
    STANDARD_H16R("standard_h16r", 16),
    STANDARD_A8("standard_a8", 8),
    STANDARD_A9("standard_a9", 16),
    STANDARD_A10("standard_a10", 8),
    STANDARD_A11("standard_a11", 16);

    private String  value;
    private Integer executors;
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureScaleSetSizeEnum.class);

    private AzureScaleSetSizeEnum(String value, Integer executors) {
        this.value = value;
        this.executors = executors;
    }

    public String toString() {
        return this.value;
    }

    public Integer getExecutors() {
        return executors;
    }

    public static AzureScaleSetSizeEnum fromValue(String value) {
        AzureScaleSetSizeEnum retVal = null;
        for (AzureScaleSetSizeEnum vmSize : AzureScaleSetSizeEnum.values()) {
            if (vmSize.value.equals(value)) {
                retVal = vmSize;
                break;
            }
        }

        if (retVal == null) {
            LOGGER.error("Tried to create azure vm size enum for: " + value + ", but we don't support such type ");
        }

        return retVal;
    }

    public String getValue() {
        return value;
    }
}
