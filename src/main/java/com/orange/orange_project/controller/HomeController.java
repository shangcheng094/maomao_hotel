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
import com.orange.orange_project.model.Reservation;
import java.util.List;
import com.orange.orange_project.model.AdminLoginForm;
import jakarta.servlet.http.HttpSession;

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
        if (reservationForm.getCheckInDate() != null
        && reservationForm.getCheckOutDate() != null
        && !reservationForm.getCheckInDate().isBlank()
        && !reservationForm.getCheckOutDate().isBlank()
        && reservationForm.getCheckInDate().compareTo(reservationForm.getCheckOutDate()) >= 0) {

            bindingResult.rejectValue("checkOutDate", "invalid", "チェックアウト日はチェックイン日より後の日付を選択してください。");
        }

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
                (owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
                jdbcTemplate.update(
                    sql,
                    reservationForm.getOwnerName(),
                    reservationForm.getPhone(),
                    reservationForm.getEmail(),
                    reservationForm.getCatName(),
                    reservationForm.getCheckInDate(),
                    reservationForm.getCheckOutDate(),
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
    @GetMapping("/admin/reservations")
    public String reservationList(Model model, HttpSession session) {
        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");
        if (adminLoggedIn == null || !adminLoggedIn) {
            return "redirect:/admin/login";
        }

        String sql = """
                SELECT id, owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text, created_at
                FROM reservations
                ORDER BY id DESC
                """;

        List<Reservation> reservations = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Reservation reservation = new Reservation();
            reservation.setId(rs.getLong("id"));
            reservation.setOwnerName(rs.getString("owner_name"));
            reservation.setPhone(rs.getString("phone"));
            reservation.setEmail(rs.getString("email"));
            reservation.setCatName(rs.getString("cat_name"));
            reservation.setCheckInDate(rs.getDate("check_in_date").toLocalDate());
            reservation.setCheckOutDate(rs.getDate("check_out_date").toLocalDate());
            reservation.setRoomType(rs.getString("room_type"));
            reservation.setNoteText(rs.getString("note_text"));
            if (rs.getTimestamp("created_at") != null) {
                reservation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return reservation;
        });

        model.addAttribute("reservations", reservations);
        return "reservations";
    }
    @GetMapping("/admin/login")
    public String adminLogin(Model model) {
        if (!model.containsAttribute("adminLoginForm")) {
            model.addAttribute("adminLoginForm", new AdminLoginForm());
        }
        return "login";
        }
    @PostMapping("/admin/login")
    public String doAdminLogin(
            @Valid AdminLoginForm adminLoginForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("adminLoginForm", adminLoginForm);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.adminLoginForm",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("loginError", "入力内容をご確認ください。");
            return "redirect:/admin/login";
        }

        String adminUsername = "admin";
        String adminPassword = "cat1234";

        if (!adminUsername.equals(adminLoginForm.getUsername())
                || !adminPassword.equals(adminLoginForm.getPassword())) {
            redirectAttributes.addFlashAttribute("adminLoginForm", adminLoginForm);
            redirectAttributes.addFlashAttribute("loginError", "ユーザー名またはパスワードが正しくありません。");
            return "redirect:/admin/login";
        }

        session.setAttribute("adminLoggedIn", true);
        return "redirect:/admin/reservations";
    }
    @PostMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

}