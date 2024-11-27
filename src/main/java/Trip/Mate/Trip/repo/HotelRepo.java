package Trip.Mate.Trip.repo;

import Trip.Mate.Trip.model.Hotel;
import Trip.Mate.Trip.model.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HotelRepo extends JpaRepository<Hotel,Integer> {

    List<Hotel> findByCityContainingIgnoreCaseOrHotelNameContainingIgnoreCase(
            String city,
            String hotelName
    );
    Hotel findByHotelName(String hotel);
    List<Hotel> findTop3ByOrderByIdAsc();

}
