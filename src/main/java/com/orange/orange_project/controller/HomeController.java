package com.orange.orange_project.controller;

import com.orange.orange_project.model.ReservationForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/reserve")
    public String reserve(ReservationForm form, Model model) {
        String sql = """
                    INSERT INTO reservations
                    (owner_name, phone, email, cat_name, stay_days, room_type, note_text)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                form.getOwnerName(),
                form.getPhone(),
                form.getEmail(),
                form.getCatName(),
                form.getStayDays(),
                form.getRoomType(),
                form.getNote()
        );

        model.addAttribute("message", "予約内容を保存しました。ありがとうございます。");
        return "result";
    }
}