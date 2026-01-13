package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.OptionDto;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Question> getRandomQuestions(List<Domain> domains, int count) {
        List<Question> allQuestions = questionRepository.findByDomainIn(domains);

        if (allQuestions.size() <= count) {
            return allQuestions;
        }

        // Shuffle and return requested count
        Collections.shuffle(allQuestions);
        return allQuestions.subList(0, count);
    }

    @Transactional(readOnly = true)
    public List<Question> getRandomQuestionsWithDistribution(List<Domain> domains, int totalCount, Map<Domain, Integer> domainPercentages) {
        List<Question> selectedQuestions = new ArrayList<>();
        Map<Domain, List<Question>> questionsByDomain = new HashMap<>();

        // Group questions by domain
        for (Domain domain : domains) {
            List<Question> domainQuestions = questionRepository.findByDomain(domain);
            Collections.shuffle(domainQuestions);
            questionsByDomain.put(domain, domainQuestions);
        }

        // Calculate questions per domain based on percentages
        Map<Domain, Integer> questionsPerDomain = new HashMap<>();
        int allocated = 0;

        for (Domain domain : domains) {
            int percentage = domainPercentages.getOrDefault(domain, 0);
            int count = (int) Math.round((totalCount * percentage) / 100.0);
            questionsPerDomain.put(domain, count);
            allocated += count;
        }

        // Adjust for rounding errors
        if (allocated < totalCount) {
            Domain firstDomain = domains.get(0);
            questionsPerDomain.put(firstDomain, questionsPerDomain.get(firstDomain) + (totalCount - allocated));
        }

        // Select questions from each domain
        for (Domain domain : domains) {
            int needed = questionsPerDomain.getOrDefault(domain, 0);
            List<Question> domainQuestions = questionsByDomain.get(domain);

            if (domainQuestions != null && !domainQuestions.isEmpty()) {
                int available = Math.min(needed, domainQuestions.size());
                selectedQuestions.addAll(domainQuestions.subList(0, available));
            }
        }

        // Shuffle final list
        Collections.shuffle(selectedQuestions);
        return selectedQuestions;
    }

    @Transactional(readOnly = true)
    public QuestionDto convertToDto(Question question, boolean includeCorrectAnswers) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setDomain(question.getDomain());
        dto.setDifficulty(question.getDifficulty());
        dto.setQtype(question.getQtype());
        dto.setStem(question.getStem());
        dto.setExplanation(question.getExplanation());

        // Parse tags
        if (question.getTagsJson() != null) {
            try {
                List<String> tags = objectMapper.readValue(question.getTagsJson(), new TypeReference<List<String>>() {});
                dto.setTags(tags);
            } catch (Exception e) {
                log.warn("Failed to parse tags for question {}", question.getId(), e);
                dto.setTags(new ArrayList<>());
            }
        }

        // Convert options
        List<OptionDto> optionDtos = question.getOptions().stream()
                .map(option -> {
                    OptionDto optionDto = new OptionDto();
                    optionDto.setId(option.getId());
                    optionDto.setLabel(option.getLabel());
                    optionDto.setText(option.getText());
                    if (includeCorrectAnswers) {
                        optionDto.setIsCorrect(option.getIsCorrect());
                    }
                    return optionDto;
                })
                .collect(Collectors.toList());
        dto.setOptions(optionDtos);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Long> getCorrectOptionIds(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        return question.getOptions().stream()
                .filter(OptionItem::getIsCorrect)
                .map(OptionItem::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getTotalQuestionCount() {
        return questionRepository.countAll();
    }

    @Transactional(readOnly = true)
    public Map<Domain, Long> getQuestionCountByDomain() {
        Map<Domain, Long> counts = new HashMap<>();
        for (Domain domain : Domain.values()) {
            counts.put(domain, questionRepository.countByDomain(domain));
        }
        return counts;
    }
}
