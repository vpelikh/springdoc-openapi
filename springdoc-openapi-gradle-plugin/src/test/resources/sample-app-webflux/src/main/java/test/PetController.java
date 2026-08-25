package test;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PetController {

    @Operation(summary = "Get a pet by id")
    @GetMapping("/pets/{id}")
    public Mono<String> getPet(@PathVariable String id) {
        return Mono.just("pet-" + id);
    }

    @GetMapping("/pets")
    public Mono<String> listPets() {
        return Mono.just("[]");
    }
}