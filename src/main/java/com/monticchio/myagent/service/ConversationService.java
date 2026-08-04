package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.ConversationDtos.ConversationSummary;
import com.monticchio.myagent.dto.ConversationDtos.MessageDto;
import com.monticchio.myagent.entity.Conversation;
import com.monticchio.myagent.entity.User;
import com.monticchio.myagent.exception.ForbiddenException;
import com.monticchio.myagent.exception.LlmException;
import com.monticchio.myagent.repository.ConversationRepository;
import com.monticchio.myagent.repository.MessageRepository;
import com.monticchio.myagent.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // Creates a new untitled conversation when conversationId is null, otherwise verifies ownership.
    public Conversation resolveOwnedConversation(String username, Long conversationId) {
        User user = getUser(username);
        if (conversationId == null) {
            Conversation conversation = new Conversation();
            conversation.setOwner(user);
            return conversationRepository.save(conversation);
        }
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new LlmException("Conversation not found"));
        if (!conversation.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to this conversation");
        }
        return conversation;
    }

    public List<ConversationSummary> listNamed(String username) {
        User user = getUser(username);
        return conversationRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(c -> c.getTitle() != null)
                .map(this::toSummary)
                .toList();
    }

    public ConversationSummary getOrCreateGeneral(String username) {
        User user = getUser(username);
        Conversation general = conversationRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(c -> c.getTitle() == null)
                .findFirst()
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setOwner(user);
                    return conversationRepository.save(conversation);
                });
        return toSummary(general);
    }

    public ConversationSummary create(String username, String title, String plantationLabel) {
        User user = getUser(username);
        Conversation conversation = new Conversation();
        conversation.setOwner(user);
        conversation.setTitle(title);
        conversation.setPlantationLabel(plantationLabel);
        conversationRepository.save(conversation);
        return toSummary(conversation);
    }

    public List<MessageDto> getMessages(String username, Long conversationId) {
        resolveOwnedConversation(username, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> new MessageDto(m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new LlmException("Authenticated user not found: " + username));
    }

    private ConversationSummary toSummary(Conversation conversation) {
        return new ConversationSummary(
                conversation.getId(), conversation.getTitle(), conversation.getPlantationLabel(), conversation.getCreatedAt());
    }
}
