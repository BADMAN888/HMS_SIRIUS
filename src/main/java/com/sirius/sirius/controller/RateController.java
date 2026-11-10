
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.service.billing.RateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Validated
public class RateController {

    private final RateService rateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RateResponse create(
            @Valid @RequestBody RateRequest request
    ) {
        return rateService.create(request);
    }

    @GetMapping
    public List<RateResponse> getAll() {
        return rateService.getAll();
    }

    @GetMapping("/{id}")
    public RateResponse getById(
            @PathVariable @Positive Long id
    ) {
        return rateService.getById(id);
    }

    @PutMapping("/{id}")
    public RateResponse update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RateRequest request
    ) {
        return rateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Positive Long id
    ) {
        rateService.delete(id);
    }
}
