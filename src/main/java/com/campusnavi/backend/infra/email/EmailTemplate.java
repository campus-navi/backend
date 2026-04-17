package com.campusnavi.backend.infra.email;

public record EmailTemplate(String subject, String content) {
    public static EmailTemplate sendCode(String code) {
        String subject = "[CampusNavi] 이메일 인증 코드";
        String content = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>이메일 인증</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f6f8; margin:0; padding:24px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                                       style="max-width:600px; background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                
                                    <tr>
                                        <td style="background-color:#111827; padding:24px; text-align:center;">
                                            <h1 style="margin:0; font-size:22px; color:#ffffff; font-weight:700;">
                                                이메일 인증코드 안내
                                            </h1>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:40px 32px;">
                                            <p style="margin:0 0 16px; font-size:16px; line-height:1.6; color:#111827;">
                                                안녕하세요.
                                            </p>
                
                                            <p style="margin:0 0 16px; font-size:16px; line-height:1.6; color:#111827;">
                                                아래 인증코드를 입력하여 이메일 인증을 완료해주세요.
                                            </p>
                
                                            <div style="margin:32px 0; text-align:center;">
                                                <div style="display:inline-block; padding:16px 32px; background-color:#f3f4f6; border:1px solid #e5e7eb; border-radius:10px;">
                                                    <span style="font-size:32px; font-weight:700; letter-spacing:8px; color:#2563eb;">
                                                        %s
                                                    </span>
                                                </div>
                                            </div>
                
                                            <p style="margin:0 0 8px; font-size:14px; line-height:1.6; color:#4b5563;">
                                                인증코드는 <strong>5분간 유효</strong>합니다.
                                            </p>
                                            <p style="margin:0 0 24px; font-size:14px; line-height:1.6; color:#4b5563;">
                                                본인이 요청하지 않았다면 이 이메일을 무시해주세요.
                                            </p>
                
                                            <hr style="border:none; border-top:1px solid #e5e7eb; margin:24px 0;">
                
                                            <p style="margin:0; font-size:12px; line-height:1.6; color:#9ca3af;">
                                                본 메일은 발신전용입니다. 문의가 필요한 경우 서비스 고객센터를 이용해주세요.
                                            </p>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="background-color:#f9fafb; padding:20px 24px; text-align:center;">
                                            <p style="margin:0; font-size:12px; color:#9ca3af;">
                                                © CampusNavi. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(code);
        return new EmailTemplate(subject, content);
    }
}
