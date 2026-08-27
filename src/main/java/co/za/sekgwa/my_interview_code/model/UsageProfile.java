package co.za.sekgwa.my_interview_code.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class UsageProfile {

    private Integer averageMonthlyDataMb;
    private Integer averageMonthlyVoiceMinutes;
    private Integer preferredValidityDays;
    private BigDecimal getMaximumBudget;

    public UsageProfile() {}

}
