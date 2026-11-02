
package com.sirius.sirius.controller;

import com.sirius.sirius.dto.RateRequest;
import com.sirius.sirius.dto.RateResponse;
import com.sirius.sirius.service.billing.RateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
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
            @PathVariable Long id
    ) {
        return rateService.getById(id);
    }

    @PutMapping("/{id}")
    public RateResponse update(
            @PathVariable Long id,
            @Valid @RequestBody RateRequest request
    ) {
        return rateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        rateService.delete(id);
    }
}
