package com.ape.friction.api.mappers;

import org.mapstruct.Mapper;
import com.ape.friction.api.dto.data.EditOwnAccountDataDTO;
import com.ape.friction.api.entities.users.Account;

@Mapper(componentModel = "spring")
public interface EditOwnAccountDataMapper {


    EditOwnAccountDataDTO toDto(Account account);


    Account toAccount(EditOwnAccountDataDTO dto);

}
