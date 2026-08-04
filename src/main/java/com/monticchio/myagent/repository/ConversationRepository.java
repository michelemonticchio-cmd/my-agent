package com.monticchio.myagent.repository;

import com.monticchio.myagent.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
