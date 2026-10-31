package com.cinema.repository;

import com.cinema.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUserUsername(String username);

    Optional<Member> findByMemberCode(String memberCode);

    boolean existsByMemberCode(String memberCode);
}
