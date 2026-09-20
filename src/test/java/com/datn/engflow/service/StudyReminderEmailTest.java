package com.datn.engflow.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StudyReminderEmailTest {
    @Test
    void reminderEscapesNameAndUsesConfiguredFrontend() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "fromEmail", "sender@example.test");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://study.example.test/");

        service.sendStreakReminder("learner@example.test", "<b>Learner</b>", 3);

        message.saveChanges();
        String html = html(message);
        assertThat(html).contains("&lt;b&gt;Learner&lt;/b&gt;")
                .contains("href=\"https://study.example.test/lessons\"")
                .doesNotContain("localhost", "<b>Learner</b>");
        assertThat(message.getSubject()).contains("3");
        verify(sender).send(message);
    }

    /**
     * OTP mail từng hardcode {@code http://localhost:5173/reset-password} trong khi
     * mail nhắc học đã đọc {@code engflow.frontend-url} — nghĩa là link đặt lại mật
     * khẩu chết với mọi người nhận ngoài máy dev, và không cách nào cấu hình được.
     */
    @Test
    void otpEmailUsesConfiguredFrontendToo() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "fromEmail", "sender@example.test");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://study.example.test/");

        service.sendOtpEmail("learner@example.test", "Learner", "123456");

        message.saveChanges();
        String html = html(message);
        assertThat(html).contains("href=\"https://study.example.test/reset-password\"")
                .doesNotContain("localhost");
        verify(sender).send(message);
    }

    private String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String result = html(multipart.getBodyPart(index));
                if (!result.isEmpty()) return result;
            }
        }
        return "";
    }
}
