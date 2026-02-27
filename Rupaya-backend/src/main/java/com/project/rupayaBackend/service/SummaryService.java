package com.project.rupayaBackend.service;

import com.project.rupayaBackend.dto.SummaryResponse;
import com.project.rupayaBackend.dto.UserResponse;
import com.project.rupayaBackend.entity.User;
import com.project.rupayaBackend.exception.NotFoundException;
import com.project.rupayaBackend.repository.BillShareRepository;
import com.project.rupayaBackend.repository.GroupMemberRepository;
import com.project.rupayaBackend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class  SummaryService {

    @Autowired
    BillShareRepository billShareRepository;

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    public SummaryResponse getSummary(UUID loggedId, UUID groupId) {

        if(groupId != null) {
            if(!groupMemberRepository.existsActiveByGroupIdAndUserId(groupId,loggedId)) {
                throw new NotFoundException("User is not the member of this group");
            }
            Double totalOwed = billShareRepository.sumTotalOwedInGroup(loggedId, groupId);
            Double totalOwe = billShareRepository.sumTotalOweInGroup(loggedId, groupId);
            return SummaryResponse.builder().totalOwed(totalOwed).totalOwe(totalOwe).groupCount(1L).friends(List.of()).build();
        }else{
            Double totalOwed = billShareRepository.sumTotalOwedAll(loggedId);
            Double totalOwe = billShareRepository.sumTotalOweAll(loggedId);
            Long totalGroupCount = groupMemberRepository.countDistinctGroupsOfUser(loggedId);

            List<User> friends = userRepository.findFriendsForUser(loggedId, PageRequest.of(0, 10));
            List<UserResponse> userResponses= friends.stream()
                    .map(user ->UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                    .toList();
            return SummaryResponse.builder()
                    .totalOwed(totalOwed)
                    .totalOwe(totalOwe)
                    .groupCount(totalGroupCount)
                    .friends(userResponses)
                    .build();
        }
    }
}
