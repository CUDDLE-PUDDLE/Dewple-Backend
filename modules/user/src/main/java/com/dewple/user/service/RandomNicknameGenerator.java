package com.dewple.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
public class RandomNicknameGenerator {

    private static final List<String> SOURCES = List.of(
            "하늘", "구름", "바람", "비", "눈", "별", "달", "해", "안개", "노을",
            "무지개", "서리", "이슬", "폭풍", "천둥", "번개", "태양", "새벽", "황혼", "여명",
            "호랑이", "사자", "여우", "곰", "늑대", "토끼", "고양이", "강아지", "용", "매",
            "독수리", "부엉이", "까마귀", "돌고래", "고래", "펭귄", "다람쥐", "사슴", "두루미", "제비",
            "나무", "꽃", "풀", "숲", "잎", "장미", "대나무", "소나무", "버드나무", "민들레",
            "선인장", "연꽃", "벚꽃", "도토리", "씨앗", "이끼", "덩굴", "뿌리", "열매", "가시",
            "레몬", "사과", "딸기", "포도", "복숭아", "수박", "귤", "체리", "망고", "호두",
            "빵떡", "소금", "후추", "땅콩", "버터", "감자", "고구마", "오렌지", "자두",
            "거울", "칼", "방패", "망치", "열쇠", "나침반", "모자", "안경", "깃털", "종이",
            "가위", "바늘", "실", "북", "종", "촛불", "등불", "항아리", "부채", "주머니",
            "봄", "여름", "가을", "겨울", "자정", "정오", "오후", "밤", "아침", "저녁",
            "꿈", "기억", "희망", "지혜", "수수께끼", "비밀", "약속", "전설", "신화", "운명",
            "그림자", "수정", "진주", "산호", "호박", "다이아", "루비", "옥", "강철",
            "불꽃", "얼음", "파도", "소용돌이", "물결", "안식", "여행", "모험", "미로", "탑",
            "성", "우주", "혜성", "유성", "행성", "성운", "은하", "오로라", "마법", "요정",
            "유령", "산", "바다", "강", "호수", "섬", "언덕", "골짜기", "사막", "동굴",
            "절벽", "오솔길", "들판", "늪", "빙하", "화산", "봉우리", "벼랑", "계곡", "갯벌", "모래"
    );

    private static final String SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 4;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String word1 = SOURCES.get(random.nextInt(SOURCES.size()));
        String word2 = SOURCES.get(random.nextInt(SOURCES.size()));

        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
        }

        return word1 + word2 + suffix;
    }
}
