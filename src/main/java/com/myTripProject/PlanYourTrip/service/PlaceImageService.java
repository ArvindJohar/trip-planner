
package com.myTripProject.PlanYourTrip.service;

import com.myTripProject.PlanYourTrip.entity.PlaceImage;
import com.myTripProject.PlanYourTrip.repository.PlaceImageRepository;
import org.springframework.stereotype.Service;

@Service
public class PlaceImageService {

    private final PlaceImageRepository placeImageRepository;

    public PlaceImageService(PlaceImageRepository placeImageRepository) {
        this.placeImageRepository = placeImageRepository;
    }

    public String getImageUrl(String city, String country) {
        if (city == null || city.isBlank()
                || country == null || country.isBlank()) {
            return null;
        }

        return placeImageRepository
                .findByCityAndCountry(city, country)
                .map(PlaceImage::getImageUrl)
                .orElse(null);
    }
}
