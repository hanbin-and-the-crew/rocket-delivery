package org.sparta.hub.application.boot;

import lombok.RequiredArgsConstructor;
import org.sparta.hub.domain.entity.Hub;
import org.sparta.hub.domain.repository.HubRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class HubDataSeeder implements CommandLineRunner {

    private final HubRepository hubRepository;

    @Override
    public void run(String... args) {
        if (hubRepository.count() > 0) return;

        hubRepository.save(Hub.create("서울특별시 센터", "서울특별시 송파구 송파대로 55", 37.5130, 127.1010));      // 잠실 인근
        hubRepository.save(Hub.create("경기 북부 센터", "경기도 고양시 덕양구 권율대로 570", 37.6525, 126.8350)); // 고양 덕양
        hubRepository.save(Hub.create("경기 남부 센터", "경기도 이천시 덕평로 257-21", 37.2720, 127.4350));     // 이천
        hubRepository.save(Hub.create("부산광역시 센터", "부산 동구 중앙대로 206", 35.1796, 129.0756));
        hubRepository.save(Hub.create("대구광역시 센터", "대구 북구 태평로 161", 35.8714, 128.6014));
        hubRepository.save(Hub.create("인천광역시 센터", "인천 남동구 정각로 29", 37.4563, 126.7052));
        hubRepository.save(Hub.create("광주광역시 센터", "광주 서구 내방로 111", 35.1595, 126.8526));
        hubRepository.save(Hub.create("대전광역시 센터", "대전 서구 둔산로 100", 36.3504, 127.3845));
        hubRepository.save(Hub.create("울산광역시 센터", "울산 남구 중앙로 201", 35.5384, 129.3114));
        hubRepository.save(Hub.create("세종특별자치시 센터", "세종특별자치시 한누리대로 2130", 36.4800, 127.2890));
        hubRepository.save(Hub.create("강원특별자치도 센터", "강원특별자치도 춘천시 중앙로 1", 37.8813, 127.7298)); // 춘천
        hubRepository.save(Hub.create("충청북도 센터", "충북 청주시 상당구 상당로 82", 36.6424, 127.4890));       // 청주
        hubRepository.save(Hub.create("충청남도 센터", "충남 홍성군 홍북읍 충남대로 21", 36.6009, 126.6650));     // 홍성
        hubRepository.save(Hub.create("전북특별자치도 센터", "전북특별자치도 전주시 완산구 효자로 225", 35.8242, 127.1480)); // 전주
        hubRepository.save(Hub.create("전라남도 센터", "전남 무안군 삼향읍 오룡길 1", 34.9904, 126.4817));       // 무안
        hubRepository.save(Hub.create("경상북도 센터", "경북 안동시 풍천면 도청대로 455", 36.5684, 128.7294));   // 안동
        hubRepository.save(Hub.create("경상남도 센터", "경남 창원시 의창구 중앙대로 300", 35.2285, 128.6811));  // 창원
    }
}
