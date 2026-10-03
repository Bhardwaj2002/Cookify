package com.cookify.admin;

import com.cookify.admin.AdminDtos.AdminUserResponse;
import com.cookify.admin.AdminDtos.BanRequest;
import com.cookify.admin.AdminDtos.UserStatus;
import com.cookify.admin.AdminDtos.WarningResponse;
import com.cookify.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Moderation tools (ROLE_ADMIN only, enforced in SecurityConfig). */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Search users; status ALL, WARNED or BANNED")
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "ALL") UserStatus status,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.users(q, status, page, size);
    }

    @Operation(summary = "A user's warnings, newest first")
    @GetMapping("/users/{id}/warnings")
    public List<WarningResponse> warnings(@PathVariable Long id) {
        return adminService.warnings(id);
    }

    @Operation(summary = "Ban a user for N days or permanently; removes their content")
    @PostMapping("/users/{id}/ban")
    public AdminUserResponse ban(@PathVariable Long id, @Valid @RequestBody BanRequest request) {
        return adminService.ban(id, request);
    }

    @Operation(summary = "Lift a ban and reset the user's warnings")
    @PostMapping("/users/{id}/unban")
    public AdminUserResponse unban(@PathVariable Long id) {
        return adminService.unban(id);
    }
}
