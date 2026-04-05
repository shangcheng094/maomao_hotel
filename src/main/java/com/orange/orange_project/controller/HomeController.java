package com.orange.orange_project.controller;

import com.orange.orange_project.model.AdminLoginForm;
import com.orange.orange_project.model.Reservation;
import com.orange.orange_project.model.ReservationForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, Integer> ROOM_CAPACITY = Map.of(
            "スタンダードルーム", 3,
            "サンシャイン・ラグジュアリールーム", 3,
            "ファミリー2匹ルーム", 3
    );

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

        String countSql = """
                SELECT COUNT(*)
                FROM reservations
                WHERE room_type = ?
                  AND check_in_date < ?
                  AND check_out_date > ?
                """;

        Integer reservedCount = jdbcTemplate.queryForObject(
                countSql,
                Integer.class,
                reservationForm.getRoomType(),
                reservationForm.getCheckOutDate(),
                reservationForm.getCheckInDate()
        );

        int capacity = ROOM_CAPACITY.getOrDefault(reservationForm.getRoomType(), 0);

        if (reservedCount == null || reservedCount >= capacity) {
            redirectAttributes.addFlashAttribute("saveError", "この日程では選択したお部屋は満室です。別のお部屋をお選びください。");
            redirectAttributes.addFlashAttribute("reservationForm", reservationForm);
            return "redirect:/#booking";
        }

        String reservationCode = "RES-" + System.currentTimeMillis();

        String insertSql = """
                INSERT INTO reservations
                (reservation_code, owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try {
            jdbcTemplate.update(
                    insertSql,
                    reservationCode,
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

        redirectAttributes.addFlashAttribute("reservationCode", reservationCode);
        redirectAttributes.addFlashAttribute("completedOwnerName", reservationForm.getOwnerName());
        redirectAttributes.addFlashAttribute("completedEmail", reservationForm.getEmail());
        redirectAttributes.addFlashAttribute("completedCatName", reservationForm.getCatName());
        redirectAttributes.addFlashAttribute("completedCheckInDate", reservationForm.getCheckInDate());
        redirectAttributes.addFlashAttribute("completedCheckOutDate", reservationForm.getCheckOutDate());
        redirectAttributes.addFlashAttribute("completedRoomType", reservationForm.getRoomType());

        return "redirect:/reservation/complete";
    }

    @GetMapping("/reservation/complete")
    public String reservationComplete() {
        return "reservation-complete";
    }

    @GetMapping("/inquiry")
    public String inquiryPage(
            @RequestParam(required = false) String reservationCode,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String ownerName,
            Model model
    ) {
        if (reservationCode != null) {
            model.addAttribute("reservationCode", reservationCode);
        }
        if (email != null) {
            model.addAttribute("email", email);
        }
        if (ownerName != null) {
            model.addAttribute("ownerName", ownerName);
        }
        return "inquiry";
    }

    @PostMapping("/inquiry")
    public String inquirySearch(
            @RequestParam String reservationCode,
            @RequestParam String email,
            @RequestParam String ownerName,
            Model model
    ) {
        String verifySql = """
                SELECT COUNT(*)
                FROM reservations
                WHERE reservation_code = ?
                  AND email = ?
                  AND owner_name = ?
                """;

        Integer matched = jdbcTemplate.queryForObject(
                verifySql,
                Integer.class,
                reservationCode,
                email,
                ownerName
        );

        if (matched == null || matched == 0) {
            model.addAttribute("inquiryError", "入力した予約番号・メールアドレス・お名前に一致する予約が見つかりませんでした。");
            model.addAttribute("reservationCode", reservationCode);
            model.addAttribute("email", email);
            model.addAttribute("ownerName", ownerName);
            return "inquiry";
        }

        String sql = """
                SELECT id, reservation_code, owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text, created_at
                FROM reservations
                WHERE email = ?
                ORDER BY created_at DESC
                """;

        List<Reservation> reservations = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Reservation reservation = new Reservation();
            reservation.setId(rs.getLong("id"));
            reservation.setReservationCode(rs.getString("reservation_code"));
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
        }, email);

        model.addAttribute("searchedReservationCode", reservationCode);
        model.addAttribute("searchedEmail", email);
        model.addAttribute("searchedOwnerName", ownerName);
        model.addAttribute("reservations", reservations);

        return "inquiry-result";
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

    @GetMapping("/admin/reservations")
    public String reservationList(Model model, HttpSession session) {
        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");
        if (adminLoggedIn == null || !adminLoggedIn) {
            return "redirect:/admin/login";
        }

        String sql = """
                SELECT id, reservation_code, owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text, created_at
                FROM reservations
                ORDER BY id DESC
                """;

        List<Reservation> reservations = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Reservation reservation = new Reservation();
            reservation.setId(rs.getLong("id"));
            reservation.setReservationCode(rs.getString("reservation_code"));
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

    @GetMapping("/api/available-rooms")
    public ResponseEntity<?> getAvailableRooms(
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate
    ) {
        if (checkInDate == null || checkInDate.isBlank()
                || checkOutDate == null || checkOutDate.isBlank()) {
            return ResponseEntity.badRequest().body("チェックイン日とチェックアウト日を入力してください。");
        }

        if (checkInDate.compareTo(checkOutDate) >= 0) {
            return ResponseEntity.badRequest().body("チェックアウト日はチェックイン日より後の日付を選択してください。");
        }

        String sql = """
                SELECT room_type, COUNT(*) AS reserved_count
                FROM reservations
                WHERE check_in_date < ?
                  AND check_out_date > ?
                GROUP BY room_type
                """;

        Map<String, Integer> reservedCountMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            reservedCountMap.put(
                    rs.getString("room_type"),
                    rs.getInt("reserved_count")
            );
        }, checkOutDate, checkInDate);

        ArrayList<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ROOM_CAPACITY.entrySet()) {
            String roomType = entry.getKey();
            int capacity = entry.getValue();
            int reserved = reservedCountMap.getOrDefault(roomType, 0);
            int remaining = capacity - reserved;

            if (remaining > 0) {
                Map<String, Object> room = new HashMap<>();
                room.put("roomType", roomType);
                room.put("remaining", remaining);
                result.add(room);
            }
        }

        return ResponseEntity.ok(result);
    }
    @PostMapping("/admin/reservations/delete")
    public String deleteReservationByAdmin(
            @RequestParam Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");
        if (adminLoggedIn == null || !adminLoggedIn) {
            return "redirect:/admin/login";
        }

        String sql = "DELETE FROM reservations WHERE id = ?";
        jdbcTemplate.update(sql, id);

        redirectAttributes.addFlashAttribute("adminMessage", "予約を削除しました。");
        return "redirect:/admin/reservations";
    }
    @PostMapping("/inquiry/delete")
    public String deleteReservationByCustomer(
            @RequestParam Long id,
            @RequestParam String reservationCode,
            @RequestParam String email,
            @RequestParam String ownerName,
            Model model
    ) {
        String verifySql = """
                SELECT COUNT(*)
                FROM reservations
                WHERE reservation_code = ?
                AND email = ?
                AND owner_name = ?
                """;

        Integer matched = jdbcTemplate.queryForObject(
                verifySql,
                Integer.class,
                reservationCode,
                email,
                ownerName
        );

        if (matched == null || matched == 0) {
            model.addAttribute("inquiryError", "入力情報が一致しないため、予約を削除できませんでした。");
            model.addAttribute("reservationCode", reservationCode);
            model.addAttribute("email", email);
            model.addAttribute("ownerName", ownerName);
            return "inquiry";
        }

        String deleteSql = """
                DELETE FROM reservations
                WHERE id = ?
                AND email = ?
                """;

        jdbcTemplate.update(deleteSql, id, email);

        String sql = """
                SELECT id, reservation_code, owner_name, phone, email, cat_name, check_in_date, check_out_date, room_type, note_text, created_at
                FROM reservations
                WHERE email = ?
                ORDER BY created_at DESC
                """;

        List<Reservation> reservations = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Reservation reservation = new Reservation();
            reservation.setId(rs.getLong("id"));
            reservation.setReservationCode(rs.getString("reservation_code"));
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
        }, email);

        model.addAttribute("inquirySuccess", "予約を取り消しました。");
        model.addAttribute("searchedReservationCode", reservationCode);
        model.addAttribute("searchedEmail", email);
        model.addAttribute("searchedOwnerName", ownerName);
        model.addAttribute("reservations", reservations);

        return "inquiry-result";
    }
}