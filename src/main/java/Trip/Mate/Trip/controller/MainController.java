package Trip.Mate.Trip.controller;

import Trip.Mate.Trip.dto.HotelDto;
import Trip.Mate.Trip.dto.PackageDto;
import Trip.Mate.Trip.model.*;
import Trip.Mate.Trip.model.Package;
import Trip.Mate.Trip.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequiredArgsConstructor
public class MainController {

    private final  UserService userService;
    private final HotelService hotelService;
    private final PackService packService;
    private final PackBookingService bookingService;
    private final MemoryService memoryService;

    @GetMapping("/")
    public String home(@CookieValue(value = "email",required = false)String email,Model model) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectMapper hotelmapper = new ObjectMapper();
        String topHotelJson = hotelmapper.writeValueAsString(hotelService.topHotels());
        model.addAttribute("hotels",topHotelJson);
        if(email !=null){
            User user = userService.getUserByEmail(email);
            model.addAttribute("user",user);

            String apiUrl = "http://localhost:5000/api/recommendations?user_id="+user.getId();
            RestTemplate restTemplate = new RestTemplate();
            try {
                Map response = restTemplate.getForObject(apiUrl,Map.class);
                if (response != null && "success".equals(response.get("status"))) {
                    List<Map<String, Object>> recommendations = (List<Map<String, Object>>) response.get("recommendations");
                    // Extract package IDs from the recommendations

                    System.out.println(recommendations);
                    List<Long> packageIds = recommendations.stream()
                            .map(rec -> Long.valueOf(rec.get("id").toString()))
                            .toList();
                    if(packageIds.isEmpty()){
                        String topPackagesJson = mapper.writeValueAsString(packService.toppackages());
                        model.addAttribute("packages",topPackagesJson);
                    }else{
                        List<Package> recommendedPackages = packService.findAllById(packageIds);
                        String recommendedPackagesJson = mapper.writeValueAsString(recommendedPackages);
                        model.addAttribute("packages", recommendedPackagesJson);
                    }

                }
                System.out.println(response);
                System.out.println("Success");
            }catch (Exception e){
                System.out.println(e);
                System.out.println("ERROR IN TRAINING");
                String topPackagesJson = mapper.writeValueAsString(packService.toppackages());
                model.addAttribute("packages",topPackagesJson);
                return "home";
            }
            return "home";
        }else{
            String topPackagesJson = mapper.writeValueAsString(packService.toppackages());
            model.addAttribute("packages",topPackagesJson);
        }
        return "home";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @PostMapping("/login")
    public String UserLogin(@RequestParam("username") String email, @RequestParam("password") String pass, HttpServletResponse response,Model model){
        if(email.equals("admin") && pass.equals("admin@123")){
            userService.admin(response);
            return "redirect:admin-home";
        }
        User user = userService.verifyUser(email, pass,response);
        if(user != null) return "redirect:/pack";
        model.addAttribute("error","Check your credentials");
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model){
        model.addAttribute("user",new User());
        return "register";
    }

    @PostMapping("/register")
    public String userRegister(@ModelAttribute User user){
        userService.userRegister(user);
        return "redirect:login";
    }

    @GetMapping("/addHotel")
    public String hotelPage(Model model){
        model.addAttribute("hotelDto",new HotelDto());
        return "add-hotel";
    }

    @PostMapping("/addHotel")
    public String addHotel(@ModelAttribute HotelDto hotelDto, @RequestParam("image")MultipartFile image,@CookieValue(value = "role",required = true) String role) throws IOException {
        if (role.equals("admin")) {
            hotelService.addHotel(hotelDto);
            return "redirect:admin-home";
        }else return "redirect:login";
    }

    @PostMapping("/updateHotel/{id}")
    public String updateHotel(@ModelAttribute HotelDto hotelDto, @RequestParam("image")MultipartFile image,@PathVariable("id") int id) throws IOException {
        hotelService.updateHotel(id,hotelDto);
        return "hotels";
    }

    @PostMapping("/updatePackage/{id}")
    public String updatePack(@ModelAttribute PackageDto packDto, @RequestParam("image")MultipartFile image,@PathVariable("id") int id) throws IOException {
        packService.updatePackage(id,packDto);
        return "redirect:/packageList";
    }

    @GetMapping("/addpackage")
    public String pack(@CookieValue(value = "role",required = true) String role,Model model){
        if (role.equals("admin")) {
            model.addAttribute("pack", new PackageDto());
            return "add-package";
        }else return "redirect:login";
    }

    @PostMapping("/addpackage")
    public String addPackage(@ModelAttribute PackageDto packageDto,@RequestParam("image") MultipartFile image) throws IOException {
        packService.addPackImage(packageDto);
        return "redirect:admin-home";
    }

    @GetMapping("/updatePackage/{id}")
    public String updatePackage(@PathVariable("id") long id,Model model) throws IOException {
        model.addAttribute("pack",packService.packByIds(id));
        model.addAttribute("packId",packService.findById(id).getId());
        model.addAttribute("isEdit",true);
        return "add-package";
    }


    @GetMapping("/admin-home")
    public String adminPage(@CookieValue(value = "role",required = false) String role,Model model){
        if (role != null){
            List<Package> packages = packService.allPack();
            List<Hotel> hotels = hotelService.getAllHotels();
            List<User> users = userService.allDetails();

            model.addAttribute("totalPackages", packages.size());
            model.addAttribute("totalHotels", hotels.size());
            model.addAttribute("totalCustomers", users.size());
            return "admin-home";
        } else
            return "redirect:login";
    }

    @GetMapping("/book/{id}")
    public String bookingPage(@CookieValue(value = "email",required = false)String email,@PathVariable("id") int id, Model model) throws IOException {
        if(email!=null){
            Package pack = packService.packById(id);
            model.addAttribute("package",pack);
            model.addAttribute("memories",memoryService.getMemoriesByPackId(id));
            model.addAttribute("hotels",hotelService.searchHotels(pack.getCity(),pack.getPackName()));
            if(!pack.getCountry().equals("india")){
                Double currency = packService.getCurrency(pack.getCountry());
                model.addAttribute("currency",currency);
                return "booking";
            }
            return "booking";
        }else return "redirect:/login";
    }

    @PostMapping("/packBook")
    public String packBooking(@RequestParam("package_id") int pId,
                              @RequestParam("totalPerson") int count,
                              @RequestParam("travelDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
                              @RequestParam("customerName") String customerName,
                              @RequestParam("hotelName") String hotel,
                              @CookieValue(value = "email", required = false) String email) {

        User user = userService.getUserByEmail(email);
        if (user == null) {
            return "redirect:/login";
        }

        Package packageToBook = packService.packById(pId);
        if (packageToBook == null) {
            return "redirect:/packages";
        }

        PackBooking packBooking = new PackBooking();
        packBooking.setPack(packageToBook);
        packBooking.setUser(user);
        packBooking.setUserCount(count);
        packBooking.setAmount((packageToBook.getPricePerPerson().multiply(BigDecimal.valueOf(count))));
        packBooking.setHotel(hotelService.findHotelById(Integer.parseInt(hotel)));
        packBooking.setBookingDate(date);

        bookingService.savePack(packBooking);
        try {
            Thread.sleep(5000); // 5000 milliseconds = 5 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }


    @GetMapping("/packageList")
    public String packageList(Model model){
        model.addAttribute("pack",packService.allPack());
        return "packages";
    }

    @GetMapping("/hotelsList")
    private String hotelsList(Model model){
        model.addAttribute("hotel",hotelService.getAllHotels());
        return "hotels";
    }

    @GetMapping("/allUsers")
    public String users(Model model){
        model.addAttribute("users",userService.allDetails());
        return "allUsers";
    }

    @GetMapping("/bookingDetails/{id}")
    public String bookingDetails(@PathVariable("id") int id,
                                 @CookieValue(value = "email",required = false)String email,Model model){
        model.addAttribute("user",userService.getUserByEmail(email));
        model.addAttribute("userBookings",bookingService.bookingByUserId(id));
        model.addAttribute("packages",packService.allPack());
        return "bookingDetails";
    }

    @GetMapping("/addMemories")
    public String addMemoriesPage(@RequestParam("bookingId") int id,Model model){
        model.addAttribute("bookingId",id);
        return "addMemories";
    }

    @PostMapping("/addMemories")
    public String addMemories(@RequestParam("images") MultipartFile[] images,
                              @RequestParam("title") String title,
                              @RequestParam("description") String description,
                              @RequestParam("bookingId") int id,
                              @CookieValue(value = "email",required = false)String email,
                              Model model) throws IOException {

        PackBooking booking = bookingService.bookingById(id);

        memoryService.save(
                images,
                title,
                description,
                userService.getUserByEmail(email),
                packService.packById((int) booking.getPack().getId()),
                bookingService.bookingById(id)
        );
        return "redirect:/";
    }

    @GetMapping("hotels/{id}")
    public String getHotelById(@PathVariable int id , Model model) {
        HotelDto hotelDto = hotelService.getHotelByIds(id);
        model.addAttribute("hotelDto",hotelDto);
        model.addAttribute("isEdit", true);
        model.addAttribute("hotelId",hotelService.findHotelById(id).getId());
        return "add-hotel";
    }

    @GetMapping("memories")
    public String memories(Model model){
        model.addAttribute("memories",memoryService.getMemories());
        return "memories";
    }

    @GetMapping("pack")
    public String packs(Model model){
        model.addAttribute("packages",packService.allPack());
        return "all-pack";
    }

    @GetMapping("hotel")
    public String hotels(Model model){
        model.addAttribute("hotels",hotelService.getAllHotels());
        return "all-hotels";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response){
        Cookie logoutCookie = new Cookie("email", null);
        logoutCookie.setMaxAge(0);
        logoutCookie.setPath("/");
        response.addCookie(logoutCookie);
        return "redirect:/";
    }

}
