package com.pageon.backend.controller;

import com.pageon.backend.dto.request.BankAccountVerification;
import com.pageon.backend.dto.response.creator.settlement.BankAccount;
import com.pageon.backend.dto.response.creator.settlement.BankAccountList;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.creator.CreatorAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creators/settlements")
@RequiredArgsConstructor
public class CreatorAccountController {


}
