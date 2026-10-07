package com.ape.friction.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.ape.friction.api.dto.response.account.AccountResponseDTO;
import com.ape.friction.api.entities.users.Account;

import java.util.List;

@Mapper(componentModel = "spring", uses = AccessLevelMapper.class)
public interface AccountListMapper {

    AccountResponseDTO toDTO(Account account);


    List<AccountResponseDTO> toDTOList(List<Account> accounts);
    /*@Mapping(target = "login", ignore = false)
    @Mapping(target = "accessLevels", ignore = false)
    @Mapping(target = "id", ignore = false)*/
    Account toAccount(AccountResponseDTO dto);
}
