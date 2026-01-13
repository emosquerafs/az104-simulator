package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.OptionDto;
import co.singularit.az104simulator.dto.QuestionResultDto;
import co.singularit.az104simulator.dto.ResultDto;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    public ResultDto calculateResults(Attempt attempt, List<AttemptAnswer> answers) {
        ResultDto result = new ResultDto();
        result.setAttemptId(attempt.getId());
        result.setTotalQuestions(attempt.getTotalQuestions());

        if (attempt.getEndedAt() != null && attempt.getStartedAt() != null) {
            long seconds = Duration.between(attempt.getStartedAt(), attempt.getEndedAt()).getSeconds();
            result.setDurationSeconds((int) seconds);
            if (attempt.getTotalQuestions() > 0) {
                result.setAverageTimePerQuestion((double) seconds / attempt.getTotalQuestions());
            }
        }

        Map<Long, Question> questionMap = new HashMap<>();
        Map<Long, List<Long>> selectedAnswersMap = new HashMap<>();

        // Build map of selected answers
        for (AttemptAnswer answer : answers) {
            if (answer.getSelectedOptionIdsJson() != null && !answer.getSelectedOptionIdsJson().isEmpty()) {
                try {
                    List<Long> selectedIds = objectMapper.readValue(
                            answer.getSelectedOptionIdsJson(),
                            new TypeReference<List<Long>>() {}
                    );
                    selectedAnswersMap.put(answer.getQuestionId(), selectedIds);
                } catch (Exception e) {
                    log.error("Failed to parse selected options for question {}", answer.getQuestionId(), e);
                    selectedAnswersMap.put(answer.getQuestionId(), new ArrayList<>());
                }
            } else {
                selectedAnswersMap.put(answer.getQuestionId(), new ArrayList<>());
            }
        }

        // Load all questions
        Set<Long> questionIds = answers.stream()
                .map(AttemptAnswer::getQuestionId)
                .collect(Collectors.toSet());

        List<Question> questions = questionRepository.findAllById(questionIds);
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        // Score each question
        int correctCount = 0;
        Map<Domain, ResultDto.DomainBreakdown> domainBreakdowns = new HashMap<>();

        for (Domain domain : Domain.values()) {
            ResultDto.DomainBreakdown breakdown = new ResultDto.DomainBreakdown();
            breakdown.setDomain(domain);
            breakdown.setCorrect(0);
            breakdown.setTotal(0);
            breakdown.setPercentage(0.0);
            domainBreakdowns.put(domain, breakdown);
        }

        List<QuestionResultDto> questionResults = new ArrayList<>();

        for (AttemptAnswer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }

            List<Long> correctIds = question.getOptions().stream()
                    .filter(OptionItem::getIsCorrect)
                    .map(OptionItem::getId)
                    .sorted()
                    .collect(Collectors.toList());

            List<Long> selectedIds = selectedAnswersMap.getOrDefault(answer.getQuestionId(), new ArrayList<>())
                    .stream()
                    .sorted()
                    .collect(Collectors.toList());

            boolean isCorrect = correctIds.equals(selectedIds);

            if (isCorrect) {
                correctCount++;
                ResultDto.DomainBreakdown breakdown = domainBreakdowns.get(question.getDomain());
                breakdown.setCorrect(breakdown.getCorrect() + 1);
            }

            ResultDto.DomainBreakdown breakdown = domainBreakdowns.get(question.getDomain());
            breakdown.setTotal(breakdown.getTotal() + 1);

            // Build question result
            QuestionResultDto qResult = new QuestionResultDto();
            qResult.setQuestionId(question.getId());
            qResult.setStem(question.getStem());
            qResult.setExplanation(question.getExplanation());
            qResult.setCorrectOptionIds(correctIds);
            qResult.setSelectedOptionIds(selectedIds);
            qResult.setIsCorrect(isCorrect);

            List<OptionDto> optionDtos = question.getOptions().stream()
                    .map(opt -> {
                        OptionDto dto = new OptionDto();
                        dto.setId(opt.getId());
                        dto.setLabel(opt.getLabel());
                        dto.setText(opt.getText());
                        dto.setIsCorrect(opt.getIsCorrect());
                        return dto;
                    })
                    .collect(Collectors.toList());
            qResult.setOptions(optionDtos);

            questionResults.add(qResult);
        }

        result.setCorrectAnswers(correctCount);
        result.setIncorrectAnswers(attempt.getTotalQuestions() - correctCount);

        if (attempt.getTotalQuestions() > 0) {
            result.setScore((correctCount * 100.0) / attempt.getTotalQuestions());
        } else {
            result.setScore(0.0);
        }

        // Calculate domain percentages
        for (ResultDto.DomainBreakdown breakdown : domainBreakdowns.values()) {
            if (breakdown.getTotal() > 0) {
                breakdown.setPercentage((breakdown.getCorrect() * 100.0) / breakdown.getTotal());
            }
        }

        result.setDomainBreakdowns(domainBreakdowns);
        result.setQuestionResults(questionResults);

        return result;
    }

    private String getLocalizedText(String textEs, String textEn, String fallback, String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return (textEn != null && !textEn.isEmpty()) ? textEn : fallback;
        } else {
            return (textEs != null && !textEs.isEmpty()) ? textEs : fallback;
        }
    }
}
