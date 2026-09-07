package com.Zx1nggg.FAMS.modules.regulator.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class InspectionRecordDTO {
    @NotNull @Positive private Long farmId;
    @Positive private Long pondId;
    @NotNull private LocalDate inspectionDate;
    @NotBlank @Pattern(regexp = "water_quality|drug_residue|seedling|feed|other") private String inspectionType;
    @Size(max = 200) private String inspectionItem;
    @NotBlank @Pattern(regexp = "qualified|unqualified") private String result;
    @Size(max = 50) private String inspectorName;
    @Size(max = 10000) private String description;
    @Size(max = 500) private String unqualifiedReason;
    @Size(max = 5000) private String attachmentUrls;
}
