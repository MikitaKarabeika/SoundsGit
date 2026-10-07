package com.ape.friction.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.ape.friction.api.dto.response.account.AccessLevelDTO;
import com.ape.friction.api.dto.response.account.AdministratorDTO;
import com.ape.friction.api.dto.response.account.ClientDTO;
import com.ape.friction.api.dto.response.account.ModeratorDTO;
import com.ape.friction.api.entities.users.*;
import com.ape.friction.api.exceptions.accessLevel.AccessLevelDoesntExistsException;

import java.util.Map;
import java.util.function.Supplier;


@Mapper(componentModel = "spring")
public interface AccessLevelMapper {


    @Mapping(target = "account", ignore = true)
    Client toClient(ClientDTO dto);

    @Mapping(target = "account", ignore = true)
    ClientDTO toClientDTO(Client clients);

    @Mapping(target = "account", ignore = true)
    Administrator toAdministrator(AdministratorDTO dto);

    @Mapping(target = "account", ignore = true)
    AdministratorDTO toAdministratorDTO(Administrator administrators);

    @Mapping(target = "account", ignore = true)
    Moderator toModerator(ModeratorDTO dto);

    @Mapping(target = "account", ignore = true)
    ModeratorDTO toModeratorDTO(Moderator moderator);





    default AccessLevel toAccessLevel(AccessLevelDTO dto) {

        if (dto instanceof ClientDTO c) return toClient(c);

        if (dto instanceof AdministratorDTO a) return toAdministrator(a);

        if (dto instanceof ModeratorDTO m) return toModerator(m);

        if (dto != null && dto.getLevelName() != null) {
            Supplier<AccessLevel> supplier = levelFactory.get(dto.getLevelName().toUpperCase());

            if (supplier != null) {
                return supplier.get();
            }
        }

        throw new AccessLevelDoesntExistsException();
    }



    default AccessLevelDTO toAccessLevelDTO(AccessLevel accesslevel) {

        if (accesslevel instanceof Client c) return toClientDTO(c);

        if (accesslevel instanceof Administrator a) return toAdministratorDTO(a);

        if (accesslevel instanceof Moderator m) return toModeratorDTO(m);


        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown AccLevel subclass: " + accesslevel.getClass());

    }


   public  Map<String, Supplier<AccessLevel>> levelFactory = Map.of(
            "ADMIN", Administrator::new,
            "CLIENT", Client::new,
            "MODERATOR", Moderator::new
    );



}
