package com.ape.friction.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.ape.friction.api.dto.data.EditUserAccountDataDataDTO;
import com.ape.friction.api.entities.users.Account;

@Mapper(componentModel = "spring")
public interface EditUserAccountDataMapper {
    EditUserAccountDataDataDTO toDto(Account account);

    Account toAccount(EditUserAccountDataDataDTO dto);
}
