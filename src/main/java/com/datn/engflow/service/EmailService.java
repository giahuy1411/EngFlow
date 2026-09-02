package com.datn.engflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * class EmailService.
 */
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  /**
   * Send a streak reminder email to a user who hasn't studied today.
   */
  public void sendStreakReminder(String toEmail, String fullName, int currentStreak) {
    String subject = currentStreak > 0
        ? "🔥 Đừng để mất chuỗi " + currentStreak + " ngày học liên tục!"
        : "📚 Hôm nay bạn chưa học — quay lại EngFlow nhé!";

    String streakDisplay = currentStreak > 0
        ? "<div style='font-size:64px;font-weight:900;color:#D02020;'>" + currentStreak
            + "</div><div style='font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:2px;color:#666;'>ngày liên tục</div>"
        : "<div style='font-size:24px;font-weight:900;color:#D02020;'>BẮT ĐẦU NGAY!</div>";

    String html = """
        <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;background:#fff;">
          <!-- Header -->
          <div style="background:#121212;padding:24px 32px;border-bottom:6px solid #D02020;">
            <div style="display:flex;align-items:center;gap:8px;">
              <div style="width:14px;height:14px;border-radius:50%%;background:#D02020;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#1a56db;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#fceea7;border:2px solid #fff;"></div>
              <span style="color:#fff;font-weight:900;font-size:20px;text-transform:uppercase;letter-spacing:-0.5px;margin-left:8px;">EngFlow</span>
            </div>
          </div>

          <!-- Body -->
          <div style="padding:40px 32px;text-align:center;">
            <div style="font-size:48px;margin-bottom:16px;">🔥</div>
            <h1 style="font-size:22px;font-weight:900;text-transform:uppercase;letter-spacing:-0.5px;margin:0 0 8px 0;color:#121212;">
              Xin chào, %s!
            </h1>
            <p style="color:#666;font-size:15px;margin:0 0 32px 0;line-height:1.6;">
              Hôm nay bạn chưa hoàn thành bài học nào. Đừng để chuỗi streak bị gãy nhé!
            </p>

            <!-- Streak Counter -->
            <div style="border:4px solid #121212;padding:24px;margin:0 auto 32px auto;max-width:200px;box-shadow:6px 6px 0 0 #121212;">
              %s
            </div>

            <!-- CTA Button -->
            <a href="http://localhost:5173/lessons"
               style="display:inline-block;background:#D02020;color:#fff;font-weight:900;font-size:16px;text-transform:uppercase;letter-spacing:1px;padding:16px 48px;text-decoration:none;border:3px solid #121212;box-shadow:5px 5px 0 0 #121212;">
              VÀO HỌC NGAY →
            </a>
          </div>

          <!-- Footer -->
          <div style="background:#f5f5f5;padding:20px 32px;border-top:3px solid #121212;text-align:center;">
            <p style="color:#999;font-size:11px;margin:0;font-weight:600;text-transform:uppercase;letter-spacing:1px;">
              EngFlow — Nền tảng học Tiếng Anh tương tác
            </p>
          </div>
        </div>
        """
        .formatted(fullName != null ? fullName : "bạn", streakDisplay);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
      log.info("Streak reminder sent to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send streak reminder to {}: {}", toEmail, e.getMessage());
    }
  }

  /**
   * Send a password-reset OTP email (6-digit code, valid 10 minutes).
   */
  public void sendOtpEmail(String toEmail, String fullName, String otp) {
    String subject = "🔐 Mã đặt lại mật khẩu EngFlow: " + otp;

    String html = """
        <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;background:#fff;">
          <div style="background:#121212;padding:24px 32px;border-bottom:6px solid #8B5CF6;">
            <div style="display:flex;align-items:center;gap:8px;">
              <div style="width:14px;height:14px;border-radius:50%%;background:#8B5CF6;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#F472B6;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#FBBF24;border:2px solid #fff;"></div>
              <span style="color:#fff;font-weight:900;font-size:20px;text-transform:uppercase;letter-spacing:-0.5px;margin-left:8px;">EngFlow</span>
            </div>
          </div>

          <div style="padding:40px 32px;text-align:center;">
            <div style="font-size:48px;margin-bottom:16px;">🔐</div>
            <h1 style="font-size:22px;font-weight:900;text-transform:uppercase;letter-spacing:-0.5px;margin:0 0 8px 0;color:#121212;">
              Xin chào, %s!
            </h1>
            <p style="color:#666;font-size:15px;margin:0 0 32px 0;line-height:1.6;">
              Bạn đã yêu cầu đặt lại mật khẩu. Nhập mã OTP dưới đây vào trang EngFlow:
            </p>

            <div style="border:4px solid #121212;padding:24px;margin:0 auto 16px auto;max-width:240px;box-shadow:6px 6px 0 0 #8B5CF6;">
              <div style="font-size:40px;font-weight:900;letter-spacing:8px;color:#8B5CF6;">%s</div>
            </div>

            <p style="color:#999;font-size:13px;margin:0 0 32px 0;">
              Mã có hiệu lực <strong>10 phút</strong>. Không chia sẻ mã này với ai.
            </p>

            <a href="http://localhost:5173/reset-password"
               style="display:inline-block;background:#8B5CF6;color:#fff;font-weight:900;font-size:16px;text-transform:uppercase;letter-spacing:1px;padding:16px 48px;text-decoration:none;border:3px solid #121212;box-shadow:5px 5px 0 0 #121212;">
              NHẬP MÃ NGAY →
</a>
          </div>

          <div style="background:#f5f5f5;padding:20px 32px;border-top:3px solid #121212;text-align:center;">
            <p style="color:#999;font-size:11px;margin:0;font-weight:600;text-transform:uppercase;letter-spacing:1px;">
              EngFlow — Nền tảng học Tiếng Anh tương tác
            </p>
          </div>
        </div>
        """
        .formatted(fullName != null ? fullName : "bạn", otp);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
      log.info("Password-reset OTP sent to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send OTP to {}: {}", toEmail, e.getMessage());
    }
  }
}