package co.za.sekgwa.my_interview_code.repository;

import co.za.sekgwa.my_interview_code.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, String> {

    Optional<PurchaseEntity> findByIdempotencyKey(String idempotencyKey);
}
