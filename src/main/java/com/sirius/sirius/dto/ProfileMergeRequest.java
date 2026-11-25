package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProfileMergeRequest(@NotNull @Positive Long keepProfileId, @NotNull @Positive Long mergeProfileId) {}
