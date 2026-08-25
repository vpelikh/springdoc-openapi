package test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PetController {

    @GetMapping("/pets/{id}")
    public String getPet(@PathVariable String id) {
        return "pet-" + id;
    }

    @GetMapping("/pets")
    public String listPets() {
        return "[]";
    }
}