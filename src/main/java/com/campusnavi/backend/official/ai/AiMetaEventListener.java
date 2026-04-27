package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.official.ai.event.OfficialPostSavedEvent;
import com.campusnavi.backend.official.repository.OfficialPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AiMetaEventListener {

    private final OfficialPostRepository postRepository;
    private final AiMetaProcessor processor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiMetaExecutor")
    public void onPostSaved(OfficialPostSavedEvent event) {
        postRepository.findById(event.postId()).ifPresent(processor::process);
    }
}
