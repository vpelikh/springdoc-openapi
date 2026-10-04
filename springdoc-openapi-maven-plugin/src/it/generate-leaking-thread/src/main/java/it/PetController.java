package it;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PetController {

    @GetMapping("/pets/{id}")
    public Mono<String> getPet(String id) {
        return Mono.just("pet-" + id);
    }

    @GetMapping("/pets")
    public Mono<String> listPets() {
        return Mono.just("[]");
    }
}