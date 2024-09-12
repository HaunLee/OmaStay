package com.omakase.omastay.service;

import com.omakase.omastay.dto.AccountDTO;
import com.omakase.omastay.dto.AdminMemberDTO;
import com.omakase.omastay.dto.FacilitiesDTO;
import com.omakase.omastay.dto.HostFacilitiesDTO;
import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.custom.HostInfoCustomDTO;
import com.omakase.omastay.dto.custom.HostMypageDTO;
import com.omakase.omastay.entity.Account;
import com.omakase.omastay.entity.AdminMember;
import com.omakase.omastay.entity.Facilities;
import com.omakase.omastay.entity.HostFacilities;
import com.omakase.omastay.entity.HostInfo;
import com.omakase.omastay.entity.Image;
import com.omakase.omastay.entity.enumurate.HStep;
import com.omakase.omastay.mapper.AccountMapper;
import com.omakase.omastay.mapper.AdminMemberMapper;
import com.omakase.omastay.mapper.FacilitiesMapper;
import com.omakase.omastay.mapper.HostInfoMapper;
import com.omakase.omastay.repository.AccountRepository;
import com.omakase.omastay.repository.HostFacilitiesRepository;
import com.omakase.omastay.repository.HostInfoRepository;
import com.omakase.omastay.repository.ImageRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HostInfoService {

    @Autowired
    private HostInfoRepository hostInfoRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private HostFacilitiesRepository hostFacilitiesRepository;

    @Autowired
    private ImageRepository imageRepository;

    public void saveHostMypage(HostMypageDTO hostMypageDTO, AdminMemberDTO adminMemberDTO) {
        System.out.println(hostMypageDTO);

        System.out.println(adminMemberDTO);
        
        AdminMember adminMember = AdminMemberMapper.INSTANCE.toAdminMember(adminMemberDTO);
        
        HostInfo hostInfo = hostInfoRepository.findByAdminMemberId(adminMember.getId());
        if (hostInfo == null) {
            hostInfo = new HostInfo();
            hostInfo.setAdminMember(adminMember); // AdminMember 설정
            hostInfo.setHStep(HStep.MYPAGE); // hStep을 0으로 설정
        }
        hostInfo.setHostContactInfo(hostMypageDTO.getHostInfo().getHostContactInfo());
        hostInfo.setHurl(hostMypageDTO.getHostInfo().getHurl());
        hostInfo.setHname(hostMypageDTO.getHostInfo().getHname());
        hostInfo.setHphone(hostMypageDTO.getHostInfo().getHphone());
        hostInfoRepository.save(hostInfo);

        Account account = accountRepository.findByHostInfoId(hostInfo.getId());
        if(account == null) {
            account = new Account();
            account.setHostInfo(hostInfo);
        }
        account.setAcBank(hostMypageDTO.getAccount().getAcBank());
        account.setAcName(hostMypageDTO.getAccount().getAcName());
        account.setAcNum(hostMypageDTO.getAccount().getAcNum());
        accountRepository.save(account); 
    }

    public HostMypageDTO findHostMypageByAdminMember(AdminMemberDTO adminMember) {
        System.out.println("마이페이지 서비스 왔다");
        HostInfo hostInfo = hostInfoRepository.findByAdminMemberId(adminMember.getId());
        System.out.println(hostInfo);
        Account account = null;
        if (hostInfo != null) {
            account = accountRepository.findByHostInfoId(hostInfo.getId());
            System.out.println(account);
            System.out.println(account.getHostInfo());
        }

        AccountDTO accountDTO = null;
        HostInfoDTO hostInfoDTO = null;

        if (account != null) {
            try {
                accountDTO = AccountMapper.INSTANCE.toAccountDTO(account);
                System.out.println(accountDTO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (hostInfo != null) {
            try {
                hostInfoDTO = HostInfoMapper.INSTANCE.toHostInfoDTO(hostInfo);
                System.out.println(hostInfoDTO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return new HostMypageDTO(accountDTO, hostInfoDTO);
    }

    public void saveHostInfo(HostInfoCustomDTO hostInfoCustomDTO, AdminMemberDTO adminMemberDTO) {
        System.out.println(hostInfoCustomDTO);
        System.out.println(adminMemberDTO);
        
        AdminMember adminMember = AdminMemberMapper.INSTANCE.toAdminMember(adminMemberDTO);
        
        HostInfo hostInfo = hostInfoRepository.findByAdminMemberId(adminMember.getId());

        hostInfo.setHStep(HStep.INFO); // hStep을 1로 설정
        hostInfo.setHCate(hostInfoCustomDTO.getHostInfo().getHCate());;
        hostInfo.setHostAddress(hostInfoCustomDTO.getHostInfo().getAddressVo());;
        hostInfo.setHostOwnerInfo(hostInfoCustomDTO.getHostInfo().getHostOwnerInfo());
        hostInfo.setDirections(hostInfoCustomDTO.getHostInfo().getDirections());
        hostInfoRepository.save(hostInfo);

        List<HostFacilities> hostFacilities = hostFacilitiesRepository.findByHostInfoId(hostInfo.getId());
        
        List<FacilitiesDTO> newFacilities = hostInfoCustomDTO.getFacilities();;

        if (hostFacilities != null) {
            // 기존 Facilities 목록에서 새로운 목록에 없는 항목 삭제
            List<HostFacilities> facilitiesToRemove = hostFacilities.stream()
                    .filter(hostFacility -> !newFacilities.contains(hostFacility.getFacilities()))
                    .collect(Collectors.toList());
            hostFacilitiesRepository.deleteAll(facilitiesToRemove);
        }
        
        // 새로운 Facilities 목록 추가
        for (FacilitiesDTO facility : newFacilities) {
            boolean exists = hostFacilities != null && hostFacilities.stream()
            .anyMatch(hostFacility -> hostFacility.getFacilities().equals(facility));
            if (!exists) {
                HostFacilities newHostFacility = new HostFacilities();
                newHostFacility.setHostInfo(hostInfo);
                newHostFacility.setFacilities(FacilitiesMapper.INSTANCE.toFacilities(facility));
                hostFacilitiesRepository.save(newHostFacility);
            }
        }

        List<Image> images = imageRepository.findByHostInfoId(hostInfo.getId());

        boolean exists = images.stream()
                .anyMatch(image -> image.getImgName().equals(hostInfoCustomDTO.getImage().getImgName()));
        if (!exists) {
            Image newImage = new Image();
            newImage.setHostInfo(hostInfo);
            newImage.setImgName(hostInfoCustomDTO.getImage().getImgName());
            imageRepository.save(newImage);
        } 
    }
}
