package com.datn.engflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.internet.MimeMessage;

/**
 * Gửi mail qua Gmail SMTP.
 * <ul>
 *   <li>{@link #sendOtpEmail} là fail-soft cho UX (không gãy request khi SMTP lag).</li>
 *   <li>{@link #sendStreakReminder} / {@link #sendStreakComebackReminder} ném exception khi gửi lỗi
 *       để scheduler có thể giữ lại marker và retry cùng ngày.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Value("${engflow.frontend-url:http://localhost:5173}")
  private String frontendUrl;

  /**
   * Mail nhắc học cho user còn streak (học yesterday, chưa học hôm nay):
   * "đừng để mất chuỗi N ngày" với N = streak hiệu lực.
   * Ném RuntimeException khi gửi lỗi — caller quyết định retry.
   */
  public void sendStreakReminder(String toEmail, String fullName, int currentStreak) {
    String subject = "🔥 Đừng để mất chuỗi " + currentStreak + " ngày học liên tục!";
    String html = buildStreakEmailBody(fullName, "streak-intro", currentStreak);
    send(toEmail, subject, html, "streak reminder", true);
  }

  /**
   * Mail mời quay lại cho user đã bỏ học ≥ 2 ngày (streak đã gãy).
   * Ném RuntimeException khi gửi lỗi.
   */
  public void sendStreakComebackReminder(String toEmail, String fullName, int lastStreak) {
    String subject = "📚 Chuỗi học đã tạm dừng — quay lại EngFlow nhé!";
    String html = buildStreakEmailBody(fullName, "comeback-intro", lastStreak);
    send(toEmail, subject, html, "streak comeback reminder", true);
  }

  /**
   * Send a password-reset OTP email (6-digit code, valid 10 minutes).
   * Fail-soft: chỉ log lỗi để request không ném 5xx làm lộ trạng thái email.
   */
  public void sendOtpEmail(String toEmail, String fullName, String otp) {
    String subject = "🔐 Mã đặt lại mật khẩu EngFlow: " + otp;
    String resetLink = HtmlUtils.htmlEscape(frontendPath("/reset-password"));
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

            <a href="%s"
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
        .formatted(HtmlUtils.htmlEscape(fullName != null ? fullName : "bạn"),
            otp, resetLink);

    send(toEmail, subject, html, "password-reset OTP", false);
  }

  private String buildStreakEmailBody(String fullName, String variant, int streak) {
    boolean atRisk = "streak-intro".equals(variant);
    String intro;
    if (atRisk) {
      intro = "Hôm nay bạn chưa học. Streak <strong>" + streak
          + " ngày</strong> chỉ còn vài giờ nữa là mất — học một bài là giữ được!";
    } else if (streak > 0) {
      intro = "Chuỗi học đã tạm dừng (bạn từng đạt <strong>" + streak
          + " ngày</strong> liên tục). Mỗi ngày lại một chuỗi mới — bắt đầu lại hôm nay!";
    } else {
      intro = "Bạn đã tạo tài khoản nhưng chuỗi học vẫn chưa bắt đầu. Mỗi ngày đều là một chuỗi mới — học ngay hôm nay!";
    }
    String counterCaption = atRisk ? "ngày liên tục — đang bị đe doạ" : "ngày bạn từng đạt";
    String counterHtml = streak > 0
        ? "<div style='font-size:64px;font-weight:900;color:#D02020;'>" + streak
            + "</div><div style='font-size:14px;font-weight:700;text-transform:uppercase;letter-spacing:2px;color:#666;'>" + counterCaption + "</div>"
        : "<div style='font-size:24px;font-weight:900;color:#D02020;'>BẮT ĐẦU NGAY!</div>";

    return """
        <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;background:#fff;">
          <div style="background:#121212;padding:24px 32px;border-bottom:6px solid #D02020;">
            <div style="display:flex;align-items:center;gap:8px;">
              <div style="width:14px;height:14px;border-radius:50%%;background:#D02020;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#1a56db;border:2px solid #fff;"></div>
              <div style="width:14px;height:14px;background:#fceea7;border:2px solid #fff;"></div>
              <span style="color:#fff;font-weight:900;font-size:20px;text-transform:uppercase;letter-spacing:-0.5px;margin-left:8px;">EngFlow</span>
            </div>
          </div>
          <div style="padding:40px 32px;text-align:center;">
            <div style="font-size:48px;margin-bottom:16px;">🔥</div>
            <h1 style="font-size:22px;font-weight:900;text-transform:uppercase;letter-spacing:-0.5px;margin:0 0 8px 0;color:#121212;">
              Xin chào, %s!
            </h1>
            <p style="color:#666;font-size:15px;margin:0 0 32px 0;line-height:1.6;">
              %s
            </p>
            <div style="border:4px solid #121212;padding:24px;margin:0 auto 32px auto;max-width:200px;box-shadow:6px 6px 0 0 #121212;">
              %s
            </div>
            <a href="%s"
              style="display:inline-block;background:#D02020;color:#fff;font-weight:900;font-size:16px;text-transform:uppercase;letter-spacing:1px;padding:16px 48px;text-decoration:none;border:3px solid #121212;box-shadow:5px 5px 0 0 #121212;">
              VÀO HỌC NGAY →
            </a>
          </div>
          <div style="background:#f5f5f5;padding:20px 32px;border-top:3px solid #121212;text-align:center;">
            <p style="color:#999;font-size:11px;margin:0;font-weight:600;text-transform:uppercase;letter-spacing:1px;">
              EngFlow — Nền tảng học Tiếng Anh tương tác
            </p>
          </div>
        </div>
        """
        .formatted(HtmlUtils.htmlEscape(fullName != null ? fullName : "bạn"),
            intro, counterHtml, HtmlUtils.htmlEscape(studyLink()));
  }

  /** Link trang học, dùng cho CTA của mail nhắc streak. */
  private String studyLink() {
    return frontendPath("/lessons");
  }

  /**
   * Ghép một đường dẫn với {@code engflow.frontend-url} đã cấu hình.
   *
   * <p>URL được kiểm trước khi ghép: nếu cấu hình trỏ tới scheme lạ, có userinfo,
   * query hoặc fragment thì ném thay vì âm thầm ghép ra một link sai — mail là
   * đường một chiều, không có cách nào sửa sau khi đã gửi.</p>
   */
  private String frontendPath(String path) {
    java.net.URI base = java.net.URI.create(frontendUrl);
    if ((!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme()))
        || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null
        || base.getFragment() != null) {
      throw new IllegalStateException("engflow.frontend-url must be an absolute HTTP(S) application URL");
    }
    return frontendUrl.replaceAll("/+$", "") + path;
  }

  private void send(String toEmail, String subject, String html, String purpose, boolean throwOnFailure) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
      log.info("{} sent to: {}", purpose, toEmail);
    } catch (Exception e) {
      log.error("Failed to send {} to {}: {}", purpose, toEmail, e.getMessage());
      if (throwOnFailure) {
        throw new IllegalStateException("Failed to send " + purpose + " to " + toEmail, e);
      }
    }
  }
}
