package com.monticchio.myagent.repository;

import com.monticchio.myagent.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {}
