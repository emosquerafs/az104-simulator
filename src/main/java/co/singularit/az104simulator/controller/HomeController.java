package co.singularit.az104simulator.controller;

import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.ExamMode;
import co.singularit.az104simulator.dto.ExamConfigDto;
import co.singularit.az104simulator.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final QuestionService questionService;

    @GetMapping("/")
    public String home(Model model) {
        long totalQuestions = questionService.getTotalQuestionCount();
        Map<Domain, Long> countsByDomain = questionService.getQuestionCountByDomain();

        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("countsByDomain", countsByDomain);

        return "home";
    }

    @GetMapping("/config")
    public String showConfig(@RequestParam(required = false) ExamMode mode, Model model) {
        ExamConfigDto config = new ExamConfigDto();

        if (mode != null) {
            config.setMode(mode);
        } else {
            config.setMode(ExamMode.PRACTICE);
        }

        // Set defaults based on mode
        if (mode == ExamMode.EXAM) {
            config.setNumberOfQuestions(50);
            config.setTimeLimitMinutes(100);
            config.setShowExplanationsImmediately(false);
        } else {
            config.setNumberOfQuestions(20);
            config.setTimeLimitMinutes(null);
            config.setShowExplanationsImmediately(true);
        }

        model.addAttribute("config", config);
        model.addAttribute("domains", Arrays.asList(Domain.values()));
        model.addAttribute("modes", Arrays.asList(ExamMode.values()));

        return "config";
    }
}
