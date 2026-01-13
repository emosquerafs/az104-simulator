package co.singularit.az104simulator.controller;

import co.singularit.az104simulator.domain.Attempt;
import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.ExamMode;
import co.singularit.az104simulator.dto.*;
import co.singularit.az104simulator.service.AttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/attempt")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final AttemptService attemptService;

    @PostMapping("/start")
    public String startAttempt(@ModelAttribute ExamConfigDto config) {
        log.info("Starting attempt with config: mode={}, questions={}", config.getMode(), config.getNumberOfQuestions());

        // Validate and set defaults
        if (config.getSelectedDomains() == null || config.getSelectedDomains().isEmpty()) {
            config.setSelectedDomains(List.of(Domain.values()));
        }

        if (config.getMode() == null) {
            config.setMode(ExamMode.PRACTICE);
        }

        Attempt attempt = attemptService.createAttempt(config);
        return "redirect:/attempt/" + attempt.getId() + "/question/0";
    }

    @GetMapping("/{attemptId}/question/{index}")
    public String showQuestion(
            @PathVariable String attemptId,
            @PathVariable int index,
            Model model) {

        Attempt attempt = attemptService.getAttempt(attemptId);
        ExamConfigDto config = attemptService.getAttemptConfig(attemptId);

        if (index < 0 || index >= attempt.getTotalQuestions()) {
            return "redirect:/attempt/" + attemptId + "/question/0";
        }

        QuestionDto question = attemptService.getQuestionForAttempt(attemptId, index, attempt.getMode());
        Map<String, Object> status = attemptService.getAttemptStatus(attemptId);

        model.addAttribute("attempt", attempt);
        model.addAttribute("question", question);
        model.addAttribute("currentIndex", index);
        model.addAttribute("status", status);
        model.addAttribute("config", config);

        return "exam";
    }

    @PostMapping("/{attemptId}/answer")
    @ResponseBody
    public Map<String, Object> submitAnswer(
            @PathVariable String attemptId,
            @RequestBody AnswerSubmissionDto submission) {

        attemptService.submitAnswer(attemptId, submission);

        return Map.of(
                "success", true,
                "message", "Answer saved"
        );
    }

    @PostMapping("/{attemptId}/navigate")
    public String navigate(
            @PathVariable String attemptId,
            @RequestParam int index) {

        attemptService.updateCurrentIndex(attemptId, index);
        return "redirect:/attempt/" + attemptId + "/question/" + index;
    }

    @GetMapping("/{attemptId}/review")
    public String reviewAttempt(@PathVariable String attemptId, Model model) {
        Attempt attempt = attemptService.getAttempt(attemptId);
        Map<String, Object> status = attemptService.getAttemptStatus(attemptId);
        List<Long> questionIds = attemptService.getQuestionIds(attemptId);

        model.addAttribute("attempt", attempt);
        model.addAttribute("status", status);
        model.addAttribute("questionIds", questionIds);

        return "review";
    }

    @PostMapping("/{attemptId}/submit")
    public String submitAttempt(@PathVariable String attemptId) {
        attemptService.completeAttempt(attemptId);
        return "redirect:/attempt/" + attemptId + "/results";
    }

    @GetMapping("/{attemptId}/results")
    public String showResults(@PathVariable String attemptId, Model model) {
        ResultDto results = attemptService.getResults(attemptId);
        Attempt attempt = attemptService.getAttempt(attemptId);

        model.addAttribute("results", results);
        model.addAttribute("attempt", attempt);

        return "results";
    }
}
