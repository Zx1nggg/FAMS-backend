package com.Zx1nggg.FAMS.modules.regulator.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class InspectionRectifyDTO {
    @NotBlank @Pattern(regexp = "pending|rectifying|rectified|accepted") private String rectifyStatus;
    private LocalDate rectifyDeadline;
    @NotBlank @Size(max = 500) private String rectifyRemark;
}
