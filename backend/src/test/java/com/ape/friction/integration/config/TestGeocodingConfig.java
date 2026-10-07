//package com.ape.friction.integration.config;
//
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//import  com.ape.friction.api.dto.internal.GeoPoint;
//
//
//import java.util.Optional;
//
//// Default no-op GeocodingService for the test profile so every integration
//// test ApplicationContext can satisfy KitchenSelectorServiceImplementation's
//// dependency. MOC17FindNearestKitchenTest provides a @Primary stub that
//// overrides this default for the cases that actually exercise MOC.17.
//@Component
//@Profile("test")
//public class TestGeocodingConfig {
//    public Optional<GeoPoint> geocode(String address) {
//        return Optional.empty();
//    }
//}
