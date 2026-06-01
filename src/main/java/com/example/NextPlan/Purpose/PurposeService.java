package com.example.NextPlan.Purpose;

import com.example.NextPlan.Entity.Purpose;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.PurposeRepository;
import com.example.NextPlan.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurposeService {

    private final PurposeRepository purposeRepository;
    private final UserRepository userRepository;

    @Transactional
    public PurposeResponse createPurpose(UUID userId, PurposeRequest request) {
        validateUser(userId);

        Purpose purpose = Purpose.builder()
                .userId(userId)
                .name(request.name())
                .targetDate(request.date())
                .link(request.link())
                .purposeType(request.type())
                .goal(request.goal())
                .createdAt(OffsetDateTime.now())
                .build();

        return PurposeResponse.from(purposeRepository.save(purpose));
    }

    @Transactional(readOnly = true)
    public PurposeListResponse getPurposes(UUID userId) {
        validateUser(userId);

        List<PurposeResponse> items = purposeRepository.findByUserIdOrderByTargetDateAscPurposeIdDesc(userId)
                .stream()
                .map(PurposeResponse::from)
                .toList();

        return new PurposeListResponse(items.size(), items);
    }

    @Transactional
    public void deletePurpose(UUID userId, Integer purposeId) {
        validateUser(userId);

        Purpose purpose = purposeRepository.findById(purposeId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        if (!purpose.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        purposeRepository.delete(purpose);
    }

    private void validateUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    public record PurposeRequest(
            String name,
            LocalDate date,
            String link,
            String type,
            String goal
    ) {
    }

    public record PurposeResponse(
            Integer purposeId,
            String name,
            LocalDate date,
            String link,
            String type,
            String goal,
            OffsetDateTime createdAt
    ) {
        public static PurposeResponse from(Purpose purpose) {
            return new PurposeResponse(
                    purpose.getPurposeId(),
                    purpose.getName(),
                    purpose.getTargetDate(),
                    purpose.getLink(),
                    purpose.getPurposeType(),
                    purpose.getGoal(),
                    purpose.getCreatedAt()
            );
        }
    }

    public record PurposeListResponse(
            int count,
            List<PurposeResponse> items
    ) {
    }
}
