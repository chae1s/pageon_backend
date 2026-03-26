package com.pageon.backend.service;

import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final CreatorRepository creatorRepository;

    public Creator findCreatorByUser(User user) {
        Creator creator = creatorRepository.findByUser(user).orElseThrow(
                () -> new CustomException(ErrorCode.CREATER_NOT_FOUND)
        );

        return creator;
    }

}
