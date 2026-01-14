package co.singularit.az104simulator.controller;

import co.singularit.az104simulator.domain.ExamMode;
import co.singularit.az104simulator.dto.AttemptHistoryDto;
import co.singularit.az104simulator.dto.QuestionReviewDto;
import co.singularit.az104simulator.service.HistoryService;
import co.singularit.az104simulator.service.StudentIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;
    private final StudentIdentityService studentIdentityService;

    /**
     * Show attempt history list
     *
     * GET /history?mode=EXAM&lang=en
     */
    @GetMapping
    public String showHistory(
            @RequestParam(required = false) ExamMode mode,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String studentId = studentIdentityService.getOrCreateStudentId(request, response);
        log.info("Showing history for studentId={}, mode={}", studentId, mode);

        // Get history (limit to 50 most recent)
        List<AttemptHistoryDto> attempts = historyService.getAttemptHistory(studentId, mode, 50);

        model.addAttribute("attempts", attempts);
        model.addAttribute("selectedMode", mode != null ? mode.name() : "ALL");

        return "history";
    }

    /**
     * Show attempt detail for review
     *
     * GET /history/{attemptId}?lang=en
     */
    @GetMapping("/{attemptId}")
    public String showAttemptDetail(
            @PathVariable String attemptId,
            HttpServletRequest request,
            Model model) {

        String studentId = studentIdentityService.getStudentId(request);
        if (studentId == null) {
            log.warn("No studentId found for attempt detail request");
            return "redirect:/history";
        }

        Locale locale = LocaleContextHolder.getLocale();
        String lang = locale.getLanguage();

        try {
            // Get attempt summary
            AttemptHistoryDto summary = historyService.getAttemptSummary(attemptId, studentId);

            // Get all questions for review
            Map<Integer, QuestionReviewDto> questions = historyService.getAttemptDetail(attemptId, studentId, lang);

            model.addAttribute("summary", summary);
            model.addAttribute("questions", questions);
            model.addAttribute("attemptId", attemptId);
            model.addAttribute("currentLang", lang);

            return "attempt-detail";

        } catch (IllegalArgumentException e) {
            log.error("Error loading attempt detail: {}", e.getMessage());
            return "redirect:/history";
        }
    }
}
