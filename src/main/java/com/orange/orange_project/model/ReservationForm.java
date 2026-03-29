package com.orange.orange_project.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReservationForm {

    @NotBlank(message = "飼い主さまのお名前は必須です。")
    @Size(max = 100, message = "飼い主さまのお名前は100文字以内で入力してください。")
    private String ownerName;

    @NotBlank(message = "お電話番号は必須です。")
    @Size(max = 30, message = "お電話番号は30文字以内で入力してください。")
    private String phone;

    @NotBlank(message = "メールアドレスは必須です。")
    @Email(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 100, message = "メールアドレスは100文字以内で入力してください。")
    private String email;

    @NotBlank(message = "猫ちゃんのお名前は必須です。")
    @Size(max = 100, message = "猫ちゃんのお名前は100文字以内で入力してください。")
    private String catName;

    @NotBlank(message = "ご宿泊日数を選択してください。")
    private String stayDays;

    @NotBlank(message = "ご希望のお部屋を選択してください。")
    private String roomType;

    @Size(max = 1000, message = "備考・ご要望は1000文字以内で入力してください。")
    private String note;

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public String getStayDays() {
        return stayDays;
    }

    public void setStayDays(String stayDays) {
        this.stayDays = stayDays;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}