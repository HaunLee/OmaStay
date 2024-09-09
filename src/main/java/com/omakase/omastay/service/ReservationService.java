package com.omakase.omastay.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.omakase.omastay.dto.PaymentDTO;
import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.entity.Member;
import com.omakase.omastay.entity.Payment;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.RoomFacilities;
import com.omakase.omastay.entity.enumurate.PayStatus;
import com.omakase.omastay.entity.enumurate.ResStatus;
import com.omakase.omastay.mapper.PaymentMapper;
import com.omakase.omastay.mapper.ReservationMapper;
import com.omakase.omastay.repository.PaymentRepository;
import com.omakase.omastay.repository.ReservationRepository;
import com.omakase.omastay.vo.StartEndVo;

import jakarta.transaction.Transactional;

@Service
public class ReservationService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Transactional
    public PaymentDTO insertPaymentInfo(PaymentDTO payment) {
        System.out.println("얍"+ payment.getNsalePrice());

        Payment res = PaymentMapper.INSTANCE.toPayment(payment);

        res.setIssuedCoupon(null);
        res.setPoint(null);

        if( payment.getNsalePrice() == null && payment.getNsalePrice().equals("")){
            res.setNsalePrice(null);
        }

        res.setPayStatus(PayStatus.PAY);
        res.setPayDate(LocalDateTime.now());

        Payment pay = paymentRepository.save(res);

        PaymentDTO dto = PaymentMapper.INSTANCE.toPaymentDTO(pay);
        return dto;
    }

    public ReservationDTO insertReservationInfo(ReservationDTO reservationDTO, PaymentDTO paymentDTO) {

        Reservation res = ReservationMapper.INSTANCE.toReservation(reservationDTO);

        res.setResPrice(500000);
        Member member = new Member();
        member.setId(1);
        res.setMember(member);
        res.setResPerson(2);
        RoomFacilities roomFacilities = new RoomFacilities();
        roomFacilities.setId(1);
        res.setRoomFacility(roomFacilities);
        StartEndVo startEndVo = new StartEndVo();
        startEndVo.setStart(LocalDateTime.now());
        startEndVo.setEnd(LocalDateTime.now().plusDays(1));
        res.setStartEndVo(startEndVo);
        res.setResStatus(ResStatus.COMPLETED);
        res.setNonMember(null);

        Reservation result = reservationRepository.save(res);
        ReservationDTO dto = ReservationMapper.INSTANCE.toReservationDTO(result);
        return dto;
    }


}
