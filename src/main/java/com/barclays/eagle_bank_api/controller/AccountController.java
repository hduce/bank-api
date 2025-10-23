package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.AccountApi;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.model.ListBankAccountsResponse;
import com.barclays.eagle_bank_api.model.UpdateBankAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController implements AccountApi {

    @Override
    public ResponseEntity<BankAccountResponse> createAccount(CreateBankAccountRequest createBankAccountRequest) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }

    @Override
    public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }

    @Override
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }

    @Override
    public ResponseEntity<ListBankAccountsResponse> listAccounts() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }

    @Override
    public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
    }
}