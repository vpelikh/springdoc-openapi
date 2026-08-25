package it;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PetController {

    @GetMapping("/pets/{id}")
    public Pet getPet(Long id) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName("pet-" + id);
        return pet;
    }

    @GetMapping("/pets")
    public String listPets() {
        return "[]";
    }
}