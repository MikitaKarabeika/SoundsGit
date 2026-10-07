package com.ape.friction.api.mappers;

import org.mapstruct.*;
import com.ape.friction.api.dto.data.PreferencesDataDTO;
import com.ape.friction.api.dto.request.AddAccountRequestDTO;
import com.ape.friction.api.dto.request.RegisterRequestDTO;
import com.ape.friction.api.entities.users.Account;

@Mapper(componentModel = "spring", uses = AccessLevelMapper.class)
public interface AccountMapper {


    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accessLevels", source = "accessLevels")
    Account toAccount(AddAccountRequestDTO dto);

    // Powiązuje access levels z kontem i aktywuje je
    @AfterMapping
    default void linkAccessLevels(AddAccountRequestDTO dto, @MappingTarget Account account) {
        if (account.getAccessLevels() != null) {
            account.getAccessLevels().forEach(al -> {
                al.setAccount(account);
                al.setActive(true);
            });
        }
    }


    //Rejestracja MOK.1
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "accessLevels", ignore = true)
    @Mapping(target = "id", ignore = true)
    Account toAccount(RegisterRequestDTO request);



    //Aktualizacja preferencji

    Account toAccount(PreferencesDataDTO dto);



    //ukradzione z warsztatu
    //troche zmienione
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "name"),
            @Mapping(target = "surname"),
            @Mapping(target = "language"),
            @Mapping(target = "theme"),
    })
    void updateExistingAccount(Account accountUpdate, @MappingTarget Account account);




}
