package com.project.rupayaBackend.service;

import com.project.rupayaBackend.dto.*;
import com.project.rupayaBackend.entity.Bill;
import com.project.rupayaBackend.entity.BillShare;
import com.project.rupayaBackend.entity.Group;
import com.project.rupayaBackend.entity.User;
import com.project.rupayaBackend.entity.enums.SplitType;
import com.project.rupayaBackend.exception.NotFoundException;
import com.project.rupayaBackend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillService {
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private BillShareRepository billShareRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private BillResponse buildBillResponse(Bill bill, List<BillShare> shares) {

        Group group = groupRepository.findById(bill.getGroupId())
                .orElse(null);

        // collect all user ids: payer + all share users
        Set<UUID> userIds = new HashSet<>();
        userIds.add(bill.getPaidBy());
        for (BillShare s : shares) userIds.add(s.getUserId());

        Map<UUID, User> userMap = userRepository.findAllByIdIn(new ArrayList<>(userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        User payerEntity = userMap.get(bill.getPaidBy());
        UserResponse payer = payerEntity == null ? null : toUserResponse(payerEntity);

        GroupMinimal groupMin = group == null ? null : GroupMinimal.builder()
                .id(group.getId())
                .name(group.getName())
                .build();

        List<BillShareResponse> shareResponses = shares.stream()
                .map(s -> {
                    User u = userMap.get(s.getUserId());
                    return BillShareResponse.builder()
                            .id(s.getId())
                            .userId(s.getUserId())
                            .amount(s.getAmount())
                            .paid(s.getPaid())
                            .user(u == null ? null : toUserResponse(u))
                            .build();
                })
                .toList();

        return BillResponse.builder()
                .id(bill.getId())
                .groupId(bill.getGroupId())
                .paidBy(bill.getPaidBy())
                .payer(payer)
                .group(groupMin)
                .createdBy(bill.getCreatedBy())
                .createdAt(bill.getCreatedAt())
                .splitType(bill.getSplitType().name())
                .description(bill.getDescription())
                .totalAmount(bill.getTotalAmount())
                .shares(shareResponses)
                .build();
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .build();
    }

    private record CalculatedShare(UUID userId, Double amount, boolean paid) {
    }

    private List<CalculatedShare> calculateShares(
            SplitType splitType,
            Double totalAmount,
            List<BillShareRequest> sharesInput,
            UUID paidBy
    ) {
        if (totalAmount == null || totalAmount <= 0) throw new IllegalArgumentException("total_amount must be > 0");
        if (sharesInput == null || sharesInput.isEmpty()) throw new IllegalArgumentException("shares is required");

        long distinct = sharesInput.stream().map(BillShareRequest::getUserId).distinct().count();
        if (distinct != sharesInput.size()) throw new IllegalArgumentException("Duplicate user_id in shares");

        if (splitType == SplitType.EQUAL) {
            List<BillShareRequest> involved = sharesInput.stream()
                    .filter(s -> s.getAmount() == null || s.getAmount() > 0)
                    .toList();
            if (involved.isEmpty()) throw new IllegalArgumentException("At least one share must be involved");

            double per = totalAmount / involved.size();
            return sharesInput.stream()
                    .map(s -> {
                        boolean isInvolved = (s.getAmount() == null || s.getAmount() > 0);
                        double amt = isInvolved ? per : 0L;
                        return new CalculatedShare(s.getUserId(), amt, s.getUserId().equals(paidBy));
                    })
                    .toList();
        }

        // EXACT
        double sum = sharesInput.stream().map(s -> s.getAmount() == null ? 0L : s.getAmount()).reduce((double) 0L, Double::sum);
        if (sum != totalAmount) throw new IllegalArgumentException("Sum of shares must equal total_amount");

        return sharesInput.stream()
                .map(s -> new CalculatedShare(
                        s.getUserId(),
                        s.getAmount() == null ? 0L : s.getAmount(),
                        s.getUserId().equals(paidBy)
                ))
                .toList();
    }

    public BillResponse addBill(BillCreateRequest data, UUID creatorId) {

        // 1) membership check
        if (!groupMemberRepository.existsActiveByGroupIdAndUserId(data.getGroupId(), creatorId)) {
            throw new SecurityException("Not a member of this group");
        }

        // 2) paidBy default
        UUID paidBy = (data.getPaidBy() != null) ? data.getPaidBy() : creatorId;

        // 3) split type
        SplitType splitType;
        try {
            splitType = SplitType.valueOf(String.valueOf(data.getSplitType()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid split_type. Allowed: EQUAL, EXACT");
        }

        // 4) calculate shares
        List<CalculatedShare> calculatedShares =
                calculateShares(splitType, data.getTotalAmount(), data.getShares(), paidBy);

        // 5) create Bill
        Bill bill = Bill.builder()
                .groupId(data.getGroupId())
                .paidBy(paidBy)
                .splitType(splitType)
                .description(data.getDescription())
                .totalAmount(data.getTotalAmount())
                .build();
        bill.setCreatedBy(creatorId);
        bill.setUpdatedBy(creatorId);

        Bill savedBill = billRepository.save(bill);

        // 6) create BillShares
        List<BillShare> savedShares = billShareRepository.saveAll(
                calculatedShares.stream().map(s -> {
                    BillShare bs = BillShare.builder()
                            .billId(savedBill.getId())
                            .userId(s.userId())
                            .amount(s.amount())
                            .paid(s.paid())
                            .build();
                    bs.setCreatedBy(creatorId);
                    bs.setUpdatedBy(creatorId);
                    return bs;
                }).toList()
        );

        // 7) build BillResponse (load users + group in 1-2 queries)
        return buildBillResponse(savedBill, savedShares);
    }

    public PaginatedResponse<BillResponse> getAllBills(UUID userId, int skip, int limit) {
        int page = skip / limit; // convert offset to page number
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 1) Query bills page (+ total)
        Page<Bill> billPage = billRepository.findAllForUser(userId, pageable);
        List<Bill> bills = billPage.getContent();

        if (bills.isEmpty()) {
            return PaginatedResponse.<BillResponse>builder()
                    .items(List.of())
                    .total(billPage.getTotalElements())
                    .skip(skip)
                    .limit(limit)
                    .hasMore(billPage.hasNext())
                    .build();
        }

        List<UUID> billIds = bills.stream().map(Bill::getId).toList();

        // 2) Query all shares for these bills (1 query)
        List<BillShare> shares = billShareRepository.findByBillIdIn(billIds);

        // 3) Bulk load groups (1 query)
        Set<UUID> groupIds = bills.stream().map(Bill::getGroupId).collect(Collectors.toSet());
        Map<UUID, Group> groupsById = groupRepository.findAllByIdIn(new ArrayList<>(groupIds))
                .stream().collect(Collectors.toMap(Group::getId, g -> g));

        // 4) Bulk load users (payer + share users) (1 query)
        Set<UUID> userIds = new HashSet<>();
        for (Bill b : bills) userIds.add(b.getPaidBy());
        for (BillShare s : shares) userIds.add(s.getUserId());

        Map<UUID, User> usersById = userRepository.findAllByIdIn(new ArrayList<>(userIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        // Group shares by billId (in-memory)
        Map<UUID, List<BillShare>> sharesByBillId =
                shares.stream().collect(Collectors.groupingBy(BillShare::getBillId));

        // Build BillResponse list
        List<BillResponse> items = bills.stream().map(b -> {

            User payerEntity = usersById.get(b.getPaidBy());
            UserResponse payer = payerEntity == null ? null : toUserResponse(payerEntity);

            Group g = groupsById.get(b.getGroupId());
            GroupMinimal groupMin = g == null ? null : GroupMinimal.builder()
                    .id(g.getId())
                    .name(g.getName())
                    .build();

            List<BillShareResponse> shareDtos =
                    sharesByBillId.getOrDefault(b.getId(), List.of())
                            .stream()
                            .map(s -> {
                                User u = usersById.get(s.getUserId());
                                return BillShareResponse.builder()
                                        .id(s.getId())
                                        .userId(s.getUserId())
                                        .amount(s.getAmount())
                                        .paid(s.getPaid())
                                        .user(u == null ? null : toUserResponse(u))
                                        .build();
                            })
                            .toList();

            return BillResponse.builder()
                    .id(b.getId())
                    .groupId(b.getGroupId())
                    .paidBy(b.getPaidBy())
                    .payer(payer)
                    .group(groupMin)
                    .createdBy(b.getCreatedBy())
                    .createdAt(b.getCreatedAt())
                    .splitType(b.getSplitType().name())
                    .description(b.getDescription())
                    .totalAmount(b.getTotalAmount())
                    .shares(shareDtos)
                    .build();

        }).toList();

        return PaginatedResponse.<BillResponse>builder()
                .items(items)
                .total(billPage.getTotalElements())
                .skip(skip)
                .limit(limit)
                .hasMore(billPage.hasNext())
                .build();
    }

    public PaginatedResponse<BillResponse> getBillsOfGroup(UUID userId, UUID groupId, int skip, int limit, String search) {
        int page = skip / limit;
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        Page<Bill> billPage = billRepository.findAllByGroupId(groupId, pageable,search);
        List<Bill> bills = billPage.getContent();

        if (bills.isEmpty()) {
            return PaginatedResponse.<BillResponse>builder().items(List.of()).total(billPage.getTotalElements()).skip(skip).limit(limit).hasMore(billPage.hasNext()).build();
        }
        List<UUID> billIds = bills.stream().map(Bill::getId).toList();

        //query all share with these billIds
        List<BillShare> shares = billShareRepository.findByBillIdIn(billIds);

        //load user(payer + share users)
        Set<UUID> userIds = new HashSet<>();
        for (Bill b : bills) {
            userIds.add(b.getPaidBy());
        }
        for (BillShare s : shares) userIds.add(s.getUserId());

        Map<UUID, User> usersById = userRepository.findAllByIdIn(new ArrayList<>(userIds)).stream().collect(Collectors.toMap(User::getId, u -> u));

        //group shares by billId
        Map<UUID, List<BillShare>> sharesByBillId =
                shares.stream().collect(Collectors.groupingBy(BillShare::getBillId));

        //build BillResponse List
        List<BillResponse> items = bills.stream().map(bill -> {
            User payerEntity = usersById.get(bill.getPaidBy());
            UserResponse payer = payerEntity == null ? null : toUserResponse(payerEntity);

            GroupMinimal groupMin = GroupMinimal.builder().id(groupId).name(group.getName()).build();

            List<BillShareResponse> shareDtos =
                    sharesByBillId.getOrDefault(bill.getId(), List.of())
                            .stream()
                            .map(s -> {
                                User u = usersById.get(s.getUserId());
                                return BillShareResponse.builder()
                                        .id(s.getId())
                                        .userId(s.getUserId())
                                        .amount(s.getAmount())
                                        .paid(s.getPaid())
                                        .user(u == null ? null : toUserResponse(u))
                                        .build();
                            })
                            .toList();
            return BillResponse.builder()
                    .id(bill.getId())
                    .groupId(bill.getGroupId())
                    .paidBy(bill.getPaidBy())
                    .payer(payer)
                    .group(groupMin)
                    .createdBy(bill.getCreatedBy())
                    .createdAt(bill.getCreatedAt())
                    .splitType(bill.getSplitType().name())
                    .description(bill.getDescription())
                    .totalAmount(bill.getTotalAmount())
                    .shares(shareDtos)
                    .build();
        }).toList();
        return PaginatedResponse.<BillResponse>builder().items(items).total(billPage.getTotalElements()).skip(skip).limit(limit).hasMore(billPage.hasNext()).build();
    }

    public BillResponse updateBill(UpdateBillRequest request, UUID billId, UUID userId) {
        // 1. Fetch bill and verify existence
        Bill bill = billRepository.findById(billId).orElse(null);
        if (bill == null) {
            throw new NotFoundException("Bill not found");
        }

        List<BillShare> currentShares = billShareRepository.findByBillId(billId);

        // 2. Check if user is a member of the group
        if (!groupMemberRepository.existsActiveByGroupIdAndUserId(bill.getGroupId(), userId)) {
            throw new SecurityException("Not a member of this group");
        }

        // 3. Resolve target values (request overrides bill)
        SplitType targetSplitType = request.getSplitType() != null ? request.getSplitType() : bill.getSplitType();
        Double targetTotalAmount = request.getTotalAmount() != null ? request.getTotalAmount() : bill.getTotalAmount();
        UUID targetPaidBy = request.getPaidBy() != null ? request.getPaidBy() : bill.getPaidBy();

        List<CalculatedShare> newSharesData = null;

        if (request.getShares() != null) {
            // Explicit shares provided: recalculate with target split/total/paidBy
            newSharesData = calculateShares(targetSplitType, targetTotalAmount, request.getShares(), targetPaidBy);
        } else if (request.getTotalAmount() != null || request.getSplitType() != null || request.getPaidBy() != null) {
            if (targetSplitType == SplitType.EQUAL) {
                // Auto-recalculate from existing share user IDs
                List<BillShareRequest> fakeSharesInput = currentShares.stream()
                        .map(s -> new BillShareRequest(s.getUserId(), s.getAmount()))
                        .toList();
                newSharesData = calculateShares(SplitType.EQUAL, targetTotalAmount, fakeSharesInput, targetPaidBy);
            } else if (targetSplitType == SplitType.EXACT && request.getTotalAmount() != null) {
                double currentSum = currentShares.stream().mapToDouble(BillShare::getAmount).sum();
                if (Math.abs(currentSum - targetTotalAmount) > 0.01) {
                    throw new IllegalArgumentException("Updating total amount on an EXACT split requires providing new shares.");
                }
            }
        }

        // 4. Surgical bill update (only fields present in request)
        if (request.getDescription() != null) {
            bill.setDescription(request.getDescription());
        }
        if (request.getTotalAmount() != null) {
            bill.setTotalAmount(request.getTotalAmount());
        }
        if (request.getSplitType() != null) {
            bill.setSplitType(request.getSplitType());
        }
        if (request.getPaidBy() != null) {
            bill.setPaidBy(request.getPaidBy());
        }
        bill.setUpdatedBy(userId);
        Bill savedBill = billRepository.save(bill);

        // 5. Surgical share updates: remove missing, update existing, add new
        if (newSharesData != null) {
            Map<UUID, BillShare> currentSharesByUserId = currentShares.stream()
                    .collect(Collectors.toMap(BillShare::getUserId, s -> s));
            Set<UUID> newUserIds = newSharesData.stream().map(CalculatedShare::userId).collect(Collectors.toSet());

            for (BillShare s : currentShares) {
                if (!newUserIds.contains(s.getUserId())) {
                    billShareRepository.delete(s);
                }
            }

            for (CalculatedShare shareData : newSharesData) {
                if (currentSharesByUserId.containsKey(shareData.userId())) {
                    BillShare existing = currentSharesByUserId.get(shareData.userId());
                    existing.setAmount(shareData.amount());
                    existing.setPaid(shareData.paid());
                    existing.setUpdatedBy(userId);
                    billShareRepository.save(existing);
                } else {
                    BillShare newShare = BillShare.builder()
                            .billId(billId)
                            .userId(shareData.userId())
                            .amount(shareData.amount())
                            .paid(shareData.paid())
                            .build();
                    newShare.setCreatedBy(userId);
                    newShare.setUpdatedBy(userId);
                    billShareRepository.save(newShare);
                }
            }
        }

        // 6. Return full bill details
        List<BillShare> finalShares = billShareRepository.findByBillId(savedBill.getId());
        return buildBillResponse(savedBill, finalShares);
    }
}
