package com.campusnavi.backend.official.post.recommend.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RecommendCandidateReader {

    Map<Long, Set<Long>> findCandidatesByMemberIdsAndDate(Collection<Long> memberIds, LocalDate date);
}