package com.campusnavi.backend.official.ai;

import com.campusnavi.backend.global.common.ProcessingStatus;
import com.campusnavi.backend.official.entity.OfficialPostAiMeta;
import com.campusnavi.backend.official.repository.OfficialPostAiMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AiMetaRetryScheduler {

    private final OfficialPostAiMetaRepository metaRepository;
    private final AiMetaProcessor processor;

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void retryFailed() {
        List<OfficialPostAiMeta> targets = metaRepository
                .findAllByStatusAndRetryCountLessThan(ProcessingStatus.FAILED, 3);

        for (OfficialPostAiMeta meta : targets) {
            processor.process(meta.getOfficialPost());
        }
    }
}
