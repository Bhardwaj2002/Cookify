package com.cookify.support;

import com.cookify.common.exception.ConflictException;
import com.cookify.common.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that trigger each error path of GlobalExceptionHandler. */
@RestController
@RequestMapping("/api/test-errors")
public class ErrorTriggerController {

    public record SampleRequest(@NotBlank String name, @NotBlank @Email String email) {
    }

    @PostMapping("/validate")
    public SampleRequest validate(@Valid @RequestBody SampleRequest body) {
        return body;
    }

    @GetMapping("/param")
    public int param(@RequestParam @Min(1) int page) {
        return page;
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id) {
        throw new NotFoundException("RECIPE_NOT_FOUND", "Recipe " + id + " not found");
    }

    @GetMapping("/conflict")
    public String conflict() {
        throw new ConflictException("USERNAME_TAKEN", "This username is taken, try again");
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("secret internal detail: jdbc password=hunter2");
    }
}
