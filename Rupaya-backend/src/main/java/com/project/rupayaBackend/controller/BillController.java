package com.project.rupayaBackend.controller;

import com.project.rupayaBackend.dto.*;
import com.project.rupayaBackend.security.CustomUserDetails;
import com.project.rupayaBackend.service.BillService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/bills/")
public class BillController {

    @Autowired
    private BillService billService;

    @PostMapping
    public ResponseEntity<BillResponse> addBill(@RequestBody BillCreateRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        BillResponse response = billService.addBill(request, principal.getId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<BillResponse>> getAllBills(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(defaultValue = "0") int skip,
                                                                       @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(billService.getAllBills(principal.getId(), skip, limit));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<PaginatedResponse<BillResponse>> getBillsOfGroup(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID groupId, @RequestParam(defaultValue = "0") int skip, @RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(billService.getBillsOfGroup(principal.getId(), groupId, skip, limit,search));
    }

    @PatchMapping("{billId}")
    public ResponseEntity<BillResponse> updateBill(@RequestBody UpdateBillRequest request, @PathVariable UUID billId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(billService.updateBill(request,billId,principal.getId()));
    }
}
