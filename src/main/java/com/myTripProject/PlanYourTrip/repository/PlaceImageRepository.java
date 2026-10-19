
package com.myTripProject.PlanYourTrip.repository;

import com.myTripProject.PlanYourTrip.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    @Query("""
            SELECT p FROM PlaceImage p
            WHERE LOWER(TRIM(p.country)) = LOWER(TRIM(:country))
              AND LOWER(TRIM(p.city)) = LOWER(TRIM(:city))
            """)
    Optional<PlaceImage> findByCityAndCountry(
            @Param("city") String city,
            @Param("country") String country
    );
}
