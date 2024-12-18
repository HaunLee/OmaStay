package com.omakase.omastay.dto;

import com.omakase.omastay.entity.Review;
import com.omakase.omastay.entity.enumurate.BooleanStatus;
import com.omakase.omastay.vo.FileImageNameVo;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class ReviewDTO {
    private int id;
    private int memIdx;
    private int resIdx;
    private int hIdx;
    private String revWriter;
    private String revContent;
    private FileImageNameVo revFileImageNameVo;
    private LocalDateTime revDate;
    private BooleanStatus revStatus;
    private Float revRating;
    private String revNone;

    

    public ReviewDTO(Review review) {
        this.id = review.getId();
        this.memIdx = review.getMember() != null ? review.getMember().getId() : null;
        this.resIdx = review.getReservation() != null ? review.getReservation().getId() : null;
        this.hIdx = review.getHostInfo() != null ? review.getHostInfo().getId() : null;
        this.revWriter = review.getRevWriter();
        this.revContent = review.getRevContent();
        this.revDate = review.getRevDate();
        this.revFileImageNameVo = review.getRevFileImageNameVo();
        this.revStatus = review.getRevStatus();
        this.revRating = review.getRevRating();
        this.revNone = review.getRevNone();
    }

    @QueryProjection
    public ReviewDTO(Integer id, Integer memberId, Integer reservationId, Integer hIdx,String revWriter, String revContent, FileImageNameVo revfileImageNameVo,LocalDateTime revDate,
                     BooleanStatus revStatus, Float revRating, String revNone) {
        this.id = id;
        this.memIdx = memberId;
        this.resIdx = reservationId;
        this.hIdx = hIdx;
        this.revWriter = revWriter;
        this.revContent = revContent;
        this.revFileImageNameVo = revfileImageNameVo;
        this.revDate = revDate;
        this.revStatus = revStatus;
        this.revRating = revRating;
        this.revNone = revNone;
    }

   
}