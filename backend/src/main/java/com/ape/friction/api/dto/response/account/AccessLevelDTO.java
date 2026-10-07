package com.ape.friction.api.dto.response.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.ape.friction.api.dto.request.AddAccountRequestDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccessLevelDTO {

    @NotBlank
    @Size(min = 1, max = 32)
    @Pattern(regexp = "CLIENT|ADMIN|MODERATOR", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String levelName;

    private boolean active;
    //@NotNull doda sie po zmianie addUser
    private AddAccountRequestDTO account;

}



