package com.omakase.omastay.service;

import com.google.api.client.util.Value;
import com.omakase.omastay.dto.ImageDTO;
import com.omakase.omastay.entity.Image;
import com.omakase.omastay.entity.Recommendation;
import com.omakase.omastay.entity.enumurate.ImgCate;
import com.omakase.omastay.mapper.ImageMapper;
import com.omakase.omastay.repository.ImageRepository;
import com.omakase.omastay.repository.RecommendationRepository;

import java.util.List;
import com.omakase.omastay.dto.custom.RecommendationCustomDTO;
import com.omakase.omastay.entity.enumurate.HCate;
import com.omakase.omastay.mapper.HostInfoMapper;
import com.omakase.omastay.mapper.RecommendationMapper;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ImageRepository imageRepository;


    public List<Recommendation> getRecommHost(){
        return recommendationRepository.findTotal();
    }

    public ImageDTO getImage(Integer hIdx) {
        System.out.println(ImgCate.HOST);
        Image image = imageRepository.findByHostInfoAndImgCate(hIdx, ImgCate.HOST).get(0);
        System.out.println("<추천숙소 이미지> " + image);
    return ImageMapper.INSTANCE.toImageDTO(image);
    }

    public List<ImageDTO> getAllHostImage(Integer hIdx) {
        List<Image> image = imageRepository.findByHostInfoAndImage(hIdx, ImgCate.HOST);
    return ImageMapper.INSTANCE.toImageDTOList(image);
    }

    public List<ImageDTO> getAllRoomImage(Integer hIdx) {
        List<Image> image = imageRepository.findByRoomInfoAndImage(hIdx, ImgCate.ROOM);
    return ImageMapper.INSTANCE.toImageDTOList(image);
    }

 
    // 매주 월요일 00시에 추천 목록을 업데이트 (지난 주 일~토까지 체크인의 매출을 기준으로 추천 목록을 업데이트)
    // @Transactional 
    // @Scheduled(fixedRate = 1800000) // 30분 = 1800000 milliseconds
    // public void updateRecommendation() {
    //     int cnt =0;

    //     // 현재 시간에서 하루 전 날짜를 계산
    //     LocalDateTime yesterday = LocalDateTime.now().minus(1, ChronoUnit.DAYS);

    //     // 하루가 지난 Reservation의 ID를 가져옴
    //     List<Reservation> expiredReservation = reservationRepository.findExpiredReservationsNotInSale(yesterday);

    //     // 필요한 로직을 처리 (예: Sale 테이블에 추가)
    //     for (Reservation reservation : expiredReservation) {
    //         Sales sales = new Sales();
    //         sales.setReservation(reservation);  // Sales 테이블에 예약 ID 설정
    //         sales.setSalDate(LocalDate.now());  // 현재 날짜 설정
    //         sales.setHostInfo(reservation.getRoomInfo().getHostInfo());
    //         salesRepository.save(sales); // Sales 테이블에 삽입
    //         cnt ++;
    //     }

    //     if(cnt > 0){
    //         System.out.println("매출 테이블에 "+cnt+"건 추가");
    //     }
    // }

    //추천 숙소 - 매주 화요일 00시에 추천 목록을 업데이트 (지난 주 일~토까지 체크인의 매출을 기준으로 추천 목록을 업데이트)
    //@Scheduled(cron = "0 0 0 * * TUE") // 매주 화요일 00시
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void updateRecommendation() {
        int cnt =0;

        LocalDate lastWeek = LocalDate.now().minus(7, ChronoUnit.DAYS);
        LocalDate starDate = LocalDate.of(lastWeek.getYear(), lastWeek.getMonth(), lastWeek.getDayOfMonth());
        LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
        LocalDate endDate = LocalDate.of(yesterday.getYear(), yesterday.getMonth(), yesterday.getDayOfMonth());

        for(HCate hCate : HCate.values()){
            List<Recommendation> a = recommendationRepository.getRecommendationsWeeklyByHCate(hCate, starDate, endDate);
            System.out.println(a);
            for(Recommendation item : a){
                recommendationRepository.save(item);
                 cnt++;
            }
        }
        
        if(cnt > 0){
            System.out.println("추천 테이블에 "+cnt+"건 추가");
        }
    }

    //관리자 추천 숙소 - 전체 추천 숙소 가져옴
    public List<RecommendationCustomDTO> findTotal(){
        List<Recommendation> list = recommendationRepository.findTotal();

        List<RecommendationCustomDTO> value = new ArrayList<>();

        for(Recommendation temp : list){
            RecommendationCustomDTO dto = new RecommendationCustomDTO();
            dto.setRecommendationDTO(RecommendationMapper.INSTANCE.toRecommendationDTO(temp));
            dto.setHostInfoDTO(HostInfoMapper.INSTANCE.toHostInfoDTO(temp.getHostInfo()));
            value.add(dto);
        }

        return value;

    }

    //관리자 추천 숙소 - 숙소 유형별 추천 숙소 가져옴
    public List<RecommendationCustomDTO> getRecommendationByHCate(HCate hCate){
        List<Recommendation> list = recommendationRepository.findByHCate(hCate);

        List<RecommendationCustomDTO> value = new ArrayList<>();

        for(Recommendation temp : list){
            RecommendationCustomDTO dto = new RecommendationCustomDTO();
            dto.setRecommendationDTO(RecommendationMapper.INSTANCE.toRecommendationDTO(temp));
            dto.setHostInfoDTO(HostInfoMapper.INSTANCE.toHostInfoDTO(temp.getHostInfo()));
            value.add(dto);
        }

        return value;
    }

}
