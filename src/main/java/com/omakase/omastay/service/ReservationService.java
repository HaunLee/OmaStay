package com.omakase.omastay.service;

import java.time.LocalDateTime;

import com.omakase.omastay.entity.enumurate.PayStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import com.omakase.omastay.dto.PaymentDTO;
import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.dto.RoomInfoDTO;
import com.omakase.omastay.dto.custom.HostReservationDTO;
import com.omakase.omastay.dto.custom.MemberCustomDTO;
import com.omakase.omastay.entity.Coupon;

import com.omakase.omastay.entity.Member;
import com.omakase.omastay.entity.Payment;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.RoomInfo;
import com.omakase.omastay.entity.enumurate.ResStatus;
import com.omakase.omastay.mapper.HostInfoMapper;
import com.omakase.omastay.mapper.PaymentMapper;
import com.omakase.omastay.mapper.ReservationMapper;
import com.omakase.omastay.mapper.RoomInfoMapper;
import com.omakase.omastay.repository.PaymentRepository;
import com.omakase.omastay.repository.ReservationRepository;
import com.omakase.omastay.vo.StartEndVo;


import java.util.List;
import java.util.ArrayList;

import jakarta.transaction.Transactional;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    public ReservationDTO checkReservation(ReservationDTO reservation) {

        Reservation res = ReservationMapper.INSTANCE.toReservation(reservation);

        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setId(2);
        res.setRoomInfo(roomInfo);
        StartEndVo startEndVo = new StartEndVo();
        startEndVo.setStart(LocalDateTime.now());
        startEndVo.setEnd(LocalDateTime.now().plusDays(1));
        res.setStartEndVo(startEndVo);

         // 방 중복 체크 시 Pessimistic Lock 적용
         Optional<Reservation> checkRoom = reservationRepository.findConflictingReservationWithLock(
            res.getRoomInfo().getId(), 
            res.getStartEndVo().getStart(), 
            res.getStartEndVo().getEnd()
        );

        if( checkRoom.isPresent() ) {
            return ReservationMapper.INSTANCE.toReservationDTO(checkRoom.get());
        }

        // 방 중복이 없는 경우
        return null;
    }

    @Transactional
    public PaymentDTO insertPaymentInfo(PaymentDTO payment) {
        System.out.println("얍"+ payment);

        // 결제 ID를 기준으로 비관적 락을 걸고 기존 결제 정보 조회
        Payment checkPay = paymentRepository.findByPaymentKeyWithLock(payment.getPaymentKey());
        System.out.println("checkPay"+ checkPay);

        // 이미 결제가 완료된 경우 중복 처리 방지
        if (checkPay != null) {
            throw new IllegalStateException("이미 결제가 완료된 상태입니다.");
        } else {
            Payment res = PaymentMapper.INSTANCE.toPayment(payment);
            System.out.println("여기뭐가나와요?"+ res);
            res.setIssuedCoupon(null);
            res.setPoint(null);
            
            if( payment.getNsalePrice() == null ){
                res.setNsalePrice("0");
            }
            
            res.setPayStatus(PayStatus.PAY);
            res.setPayDate(LocalDateTime.now());
            
            
            Payment pay = paymentRepository.save(res);
            
            PaymentDTO dto = PaymentMapper.INSTANCE.toPaymentDTO(pay);
            return dto;
        }
    }

    @Transactional
    public ReservationDTO insertReservationInfo(ReservationDTO reservationDTO, PaymentDTO paymentDTO) {

        Reservation res = ReservationMapper.INSTANCE.toReservation(reservationDTO);
        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setId(2);
        res.setRoomInfo(roomInfo);

        StartEndVo startEndVo = new StartEndVo();
        startEndVo.setStart(LocalDateTime.now());
        startEndVo.setEnd(LocalDateTime.now().plusDays(1));
        res.setStartEndVo(startEndVo);

        res.setResPrice(Integer.parseInt(paymentDTO.getAmount()));
        res.setResPerson(2);
        res.setRoomInfo(roomInfo);
        res.setStartEndVo(startEndVo);
        res.setResStatus(ResStatus.PENDING);
        res.setNonMember(null);

        Reservation result = reservationRepository.save(res);
        ReservationDTO dto = ReservationMapper.INSTANCE.toReservationDTO(result);
        return dto;
    }

    public ReservationDTO getReservation(int resIdx ) {
        Reservation res = reservationRepository.findById(resIdx).get();
        res.setResStatus(ResStatus.CANCELLED);
        Reservation result = reservationRepository.save(res);
        ReservationDTO dto = ReservationMapper.INSTANCE.toReservationDTO(result);
        return dto;
    }

    public List<HostReservationDTO> getAllRes(List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

    for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
        RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
        List<Reservation> reservations = reservationRepository.findByRoomInfo(roomInfo);
        for (Reservation reservation : reservations) {
            HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
            hostReservationAll.add(hostReservationDTO);
        }
    }
       return hostReservationAll;
    }

    public List<HostReservationDTO> searchRes(String resStatus, String dateValue, List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

        if (dateValue != null && !dateValue.trim().isEmpty()) {
            String[] date = dateValue.split(" ~ ");
            if (date.length < 2 || date[0].trim().isEmpty() || date[1].trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid date range format");
            }
            String startDate = date[0];
            String endDate = date[1];
            for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
                RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
                List<Reservation> reservations = reservationRepository.searchRes(resStatus, startDate, endDate, roomInfo);
                for (Reservation reservation : reservations) {
                    HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                    hostReservationAll.add(hostReservationDTO);
                }
            }
        } else {
            for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
                RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
                List<Reservation> reservations = reservationRepository.searchRes(resStatus, null, null, roomInfo);
                for (Reservation reservation : reservations) {
                    HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                    hostReservationAll.add(hostReservationDTO);
                }
            }
        }

        return hostReservationAll;
    }

    // 예약 확정
    public int confirmRes(List<Integer> ids) {
        int[] idArray = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            idArray[i] = ids.get(i);
        }

        int cnt = reservationRepository.confirmById(idArray);
        return cnt;
    }

    // 예약 취소
    public int rejectRes(List<Integer> ids) {
        int[] idArray = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            idArray[i] = ids.get(i);
        }

        int cnt = reservationRepository.rejectById(idArray);
        return cnt;
    }

    //호스트 예약 상세
    public HostReservationDTO getHostRes(int id) {
        Reservation res = reservationRepository.findById(id).get();
        HostReservationDTO hostReservationDTO = new HostReservationDTO(res);
        return hostReservationDTO;
    }


    /********** 체크 아웃 시점 이후 [확정]->[사용 완료] 예약 상태 변경 **********/
    // 30분마다 실행
    @Transactional
    @Scheduled(fixedRate = 1800000) // 30분 = 1800000 milliseconds
    public void checkAndUpdateExpiredStatus() {
        System.out.println("예약 상태 업데이트");
        reservationRepository.updateExpiredStatuses();
    }


    //admin의 회원조회에서 회원의 최근 예약 기록을 가져옴
    public Map<String, Object> member_reservation(Integer memId){

        Map<String, Object> map = new HashMap<>();

        LocalDateTime month3 = LocalDateTime.now().minusMonths(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
        //최근 3개월 내 예약 건수를 가져옴
        Integer monthCount = reservationRepository.get3MonthCount(memId, month3);
        System.out.println("monthCount: "+monthCount);
        map.put("monthCount", monthCount);

        //전체 예약 건수를 가져옴
        Integer totalCount = reservationRepository.getTotalCount(memId);
        System.out.println("totalCount: "+totalCount);
        map.put("totalCount", totalCount);

        //최근 예약 내역 5건 가져옴
        List<Reservation> list = reservationRepository.get5List(memId);

        List<MemberCustomDTO> recentList = new ArrayList<>();

        for(Reservation item : list){
            MemberCustomDTO dto = new MemberCustomDTO();
            dto.setReservation(ReservationMapper.INSTANCE.toReservationDTO(item));
            dto.setHostInfo(HostInfoMapper.INSTANCE.toHostInfoDTO(item.getRoomInfo().getHostInfo()));
            recentList.add(dto);
        }
        System.out.println("list: "+recentList);
        map.put("recentList", recentList);

        return map;

    }
}
