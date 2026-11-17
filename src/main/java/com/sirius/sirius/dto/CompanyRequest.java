
package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompanyRequest(

        @NotNull @Positive Long legalEntityId

) {
}
