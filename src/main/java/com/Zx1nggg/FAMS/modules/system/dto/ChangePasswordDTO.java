package com.Zx1nggg.FAMS.modules.system.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ChangePasswordDTO {
    @NotBlank @lombok.ToString.Exclude private String oldPassword;
    @NotBlank @Size(min = 6, max = 72) @lombok.ToString.Exclude private String newPassword;
}
