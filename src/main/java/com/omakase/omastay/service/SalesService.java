package com.omakase.omastay.service;

import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.Sales;
import com.omakase.omastay.repository.ReservationRepository;
import com.omakase.omastay.repository.SalesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalesService {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    /********** 예약이 사용완료가 되면 체크아웃 다음날 매출 테이블로 들어감 **********/
    @Transactional 
    @Scheduled(fixedRate = 1800000) // 30분 = 1800000 milliseconds
    public void insertSales() {

        // 현재 시간에서 하루 전 날짜를 계산
        LocalDateTime yesterday = LocalDateTime.now().minus(1, ChronoUnit.DAYS);

        // 하루가 지난 Reservation의 ID를 가져옴
        List<Reservation> expiredReservation = reservationRepository.findExpiredReservationsNotInSale(yesterday);

        // 필요한 로직을 처리 (예: Sale 테이블에 추가)
        for (Reservation reservation : expiredReservation) {
            Sales sales = new Sales();
            sales.setReservation(reservation);  // Sales 테이블에 예약 ID 설정
            sales.setSalDate(LocalDate.now());  // 현재 날짜 설정
            sales.setHostInfo(reservation.getRoomInfo().getHostInfo());
            salesRepository.save(sales); // Sales 테이블에 삽입
        }

        System.out.println("매출 테이블 추가");
    }
}
