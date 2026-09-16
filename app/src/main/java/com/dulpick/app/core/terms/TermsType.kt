package com.dulpick.app.core.terms

// 온보딩 약관 3종. service·privacy 는 필수, marketing 은 선택 (iOS TermsType 대응)
enum class TermsType(
    val title: String,
    // 약관 시트에 표시하는 문구. TermsType.title 과 달라 따로 둔다
    val agreementTitle: String,
    val isRequired: Boolean,
    val url: String,
) {
    SERVICE(
        title = "이용약관",
        agreementTitle = "서비스 이용약관(필수)",
        isRequired = true,
        url = "https://dulpick.omong.kr/terms",
    ),
    PRIVACY(
        title = "개인정보수집 및 이용",
        agreementTitle = "개인정보수집 및 이용(필수)",
        isRequired = true,
        url = "https://dulpick.omong.kr/privacy",
    ),
    MARKETING(
        title = "마케팅 수신 동의",
        agreementTitle = "마케팅 수신 동의(선택)",
        isRequired = false,
        url = "https://dulpick.omong.kr/marketing",
    ),
}
