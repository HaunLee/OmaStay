package com.omakase.omastay.service;

import com.omakase.omastay.repository.PriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    @Autowired
    private PriceRepository priceRepository;


}
