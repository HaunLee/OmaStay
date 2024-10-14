package com.omakase.omastay.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;


import com.omakase.omastay.dto.NonMemberDTO;
import com.omakase.omastay.dto.PaymentDTO;
import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.dto.RoomInfoDTO;
import com.omakase.omastay.dto.custom.HostReservationDTO;
import com.omakase.omastay.dto.custom.MemberCustomDTO;
import com.omakase.omastay.dto.custom.HostReservationEmailDTO;
import com.omakase.omastay.entity.NonMember;
import com.omakase.omastay.entity.Payment;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.RoomInfo;
import com.omakase.omastay.entity.enumurate.PayStatus;
import com.omakase.omastay.entity.enumurate.ResStatus;
import com.omakase.omastay.mapper.HostInfoMapper;
import com.omakase.omastay.mapper.PaymentMapper;
import com.omakase.omastay.mapper.ReservationMapper;
import com.omakase.omastay.mapper.RoomInfoMapper;
import com.omakase.omastay.repository.PaymentRepository;
import com.omakase.omastay.repository.ReservationRepository;

import jakarta.transaction.Transactional;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    public ReservationDTO checkReservation(ReservationDTO reservation) {
        System.out.println("그대의 방은 왜 null?"+reservation);

        Reservation res = ReservationMapper.INSTANCE.toReservation(reservation);

         // 방 중복 체크 시 Pessimistic Lock 적용
         List<Reservation> checkRoom = reservationRepository.checkSameRoom(
            res.getRoomInfo().getId(), 
            res.getStartEndVo().getStart(), 
            res.getStartEndVo().getEnd()
        );

        if (checkRoom != null && checkRoom.size() > 0 ){
            return ReservationMapper.INSTANCE.toReservationDTOList(checkRoom).get(0);
        } else {
            return null;
        }

    }

    @Transactional
    public PaymentDTO insertPaymentInfo(PaymentDTO payment) {
        System.out.println("얍"+ payment);

        // 이미 결제가 완료된 경우 중복 처리 방지
        Payment res = PaymentMapper.INSTANCE.toPayment(payment);
        System.out.println("여기뭐가나와요?"+ res);
        if( payment.getIcIdx() == null){
            res.setIssuedCoupon(null);
        }
        if( payment.getPIdx() == null){
            res.setPoint(null);
        }
        
        if( payment.getNsalePrice() == null ){
            res.setNsalePrice("0");
        }
        
        res.setPayStatus(PayStatus.PAY);
        res.setPayDate(LocalDateTime.now());
        
        
        Payment pay = paymentRepository.save(res);
        
        PaymentDTO dto = PaymentMapper.INSTANCE.toPaymentDTO(pay);
        return dto;
    }

    @Transactional
    public ReservationDTO insertReservationInfo(ReservationDTO reservationDTO, PaymentDTO paymentDTO) {
        Reservation res = ReservationMapper.INSTANCE.toReservation(reservationDTO);
        res.setNonMember(null);
        res.setResPrice(Integer.parseInt(paymentDTO.getAmount()));
        res.setResPerson(2);
        res.setResStatus(ResStatus.PENDING);
        
        Reservation result = reservationRepository.save(res);
        ReservationDTO dto = ReservationMapper.INSTANCE.toReservationDTO(result);
        return dto;
    }


    @Transactional
    public ReservationDTO insertNonMemberReservationInfo(ReservationDTO reservationDTO, PaymentDTO paymentDTO, NonMemberDTO noMember) {
        Reservation res = ReservationMapper.INSTANCE.toReservation(reservationDTO);
        res.setMember(null);
        System.out.println("비회원정보" + noMember);
        
        NonMember nonMember = new NonMember();
        nonMember.setId(noMember.getId());
        res.setNonMember(nonMember);
        res.setResName(noMember.getNonName());
        res.setResEmail(noMember.getNonEmail());

        
        res.setResPrice(Integer.parseInt(paymentDTO.getAmount()));
        res.setResPerson(2);
        res.setResStatus(ResStatus.PENDING);
        Payment payment = new Payment();
        payment.setId(paymentDTO.getId());
        res.setPayment(payment);
        
        Reservation result = reservationRepository.save(res);
        ReservationDTO dto = ReservationMapper.INSTANCE.toReservationDTO(result);
        return dto;
    }


    @Transactional
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


    //admin의 회원 상세 조회에서 회원의 최근 예약 기록을 가져옴
    public Map<String, Object> member_reservation(Integer memId){

        Map<String, Object> map = new HashMap<>();

        //3개월 날짜 범위 구하기
        LocalDateTime month3 = LocalDateTime.now().minusMonths(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
        //최근 3개월 내 예약 확정&사용완료 건수를 가져옴
        Integer monthCount = reservationRepository.get3MonthCount(memId, month3);
        map.put("monthCount", monthCount);

        //전체 예약 확정&사용완료 건수를 가져옴
        Integer totalCount = reservationRepository.getTotalCount(memId);
        map.put("totalCount", totalCount);

        //최근 예약 내역 5건 가져옴
        List<Reservation> list = reservationRepository.get5List(memId);

        List<MemberCustomDTO> recentList = new ArrayList<>();

        for(Reservation item : list){
            MemberCustomDTO dto = new MemberCustomDTO();
            dto.setReservation(ReservationMapper.INSTANCE.toReservationDTO(item));
            dto.setHostInfo(HostInfoMapper.INSTANCE.toHostInfoDTO(item.getRoomInfo().getHostInfo()));
            dto.setPayment(PaymentMapper.INSTANCE.toPaymentDTO(item.getPayment()));
            recentList.add(dto);
        }
        System.out.println("list: "+recentList);
        map.put("recentList", recentList);

        return map;

    }
    @Transactional
    public List<HostReservationDTO> getReservationsDay(List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

        LocalDateTime date = LocalDate.now().atStartOfDay();

        for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
            RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
            List<Reservation> reservations = reservationRepository.findReservationsByDate(date, roomInfo);

            for (Reservation reservation : reservations) {
                HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                hostReservationAll.add(hostReservationDTO);
            }
        }
           return hostReservationAll;
    
    }

    @Transactional
    public List<HostReservationDTO> getReservationsWeek(List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX);

        for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
            RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
            List<Reservation> reservations = reservationRepository.findReservationsByWeek(startOfWeek, endOfWeek, roomInfo);
            for (Reservation reservation : reservations) {
                HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                hostReservationAll.add(hostReservationDTO);
            }
        }
           return hostReservationAll;
    
    }

    //이번달 예약정보
    @Transactional
    public List<HostReservationDTO> getReservationsMonth(List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
            RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
            List<Reservation> reservations = reservationRepository.findReservationsByMonth(startOfMonth, endOfMonth, roomInfo);
            for (Reservation reservation : reservations) {
                HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                hostReservationAll.add(hostReservationDTO);
            }
        }
           return hostReservationAll;
    
    }

    //입실예정정보
    @Transactional
    public List<HostReservationDTO> findReservationsByCheckIn(List<RoomInfoDTO> roomInfoDTOList) {
        List<HostReservationDTO> hostReservationAll = new ArrayList<>();

        LocalDateTime nowDate = LocalDate.now().atStartOfDay();

        for (RoomInfoDTO roomInfoDTO : roomInfoDTOList) {
            RoomInfo roomInfo = RoomInfoMapper.INSTANCE.toRoomInfo(roomInfoDTO);
            List<Reservation> reservations = reservationRepository.findReservationsByCheckIn(nowDate, roomInfo);
            for (Reservation reservation : reservations) {
                HostReservationDTO hostReservationDTO = new HostReservationDTO(reservation);
                hostReservationAll.add(hostReservationDTO);
            }
        }
           return hostReservationAll;
    
    }

    //예약대기정보
    @Transactional
    public List<HostReservationEmailDTO> findReservationsByPending() {
        List<HostReservationEmailDTO> hostReservationAll = new ArrayList<>();
        List<Reservation> pendingReservations = reservationRepository.findReservationsByPending();
        
        for (Reservation reservation : pendingReservations) {
            hostReservationAll.add(new HostReservationEmailDTO(reservation));
        }

        return hostReservationAll;
    }

    //예약확정,취소메일 발송을 위한 예약 정보 조회
    @Transactional
    public HostReservationEmailDTO getRes(Integer id) {
         Reservation res = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reservation Id:" + id));
        return new HostReservationEmailDTO(res);
    }
  
    public ReservationDTO getNoReservation(String resNum, String nonEmail) {
        return ReservationMapper.INSTANCE.toReservationDTO(reservationRepository.findByResNumAndNonEmail(resNum, nonEmail));
    }

    public List<ReservationDTO> getMemIdxListByHIdx(Integer memIdx, Integer hIdx) {
        return reservationRepository.findSingleByMemIdxAndHIdx(hIdx,memIdx);
    }

   
  
}
