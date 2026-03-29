package com.orange.orange_project.controller;

import com.orange.orange_project.model.ReservationForm;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String home(Model model) {
        if (!model.containsAttribute("reservationForm")) {
            model.addAttribute("reservationForm", new ReservationForm());
        }
        return "index";
    }

    @PostMapping("/reserve")
    public String reserve(
            @Valid ReservationForm reservationForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("saveError", "入力内容をご確認ください。");
            redirectAttributes.addFlashAttribute("reservationForm", reservationForm);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.reservationForm",
                    bindingResult
            );
            return "redirect:/#booking";
        }

        String sql = """
                INSERT INTO reservations
                (owner_name, phone, email, cat_name, stay_days, room_type, note_text)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            jdbcTemplate.update(
                    sql,
                    reservationForm.getOwnerName(),
                    reservationForm.getPhone(),
                    reservationForm.getEmail(),
                    reservationForm.getCatName(),
                    reservationForm.getStayDays(),
                    reservationForm.getRoomType(),
                    reservationForm.getNote()
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("saveError", "予約の保存に失敗しました。時間をおいて再度お試しください。");
            redirectAttributes.addFlashAttribute("reservationForm", reservationForm);
            return "redirect:/#booking";
        }

        redirectAttributes.addFlashAttribute("successMessage", "予約内容を保存しました。ありがとうございます。");
        return "redirect:/#booking";
    }
}