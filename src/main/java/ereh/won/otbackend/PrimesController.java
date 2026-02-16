package ereh.won.otbackend;

import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/primes")
public class PrimesController {

    private final PrimesService primesService;

    public PrimesController(PrimesService primesService) {
        this.primesService = primesService;
    }

    @GetMapping("/getPrime")
    public ResponseEntity<Integer> getPrime(@RequestParam int position) {
        val result = primesService.getPrime(position);
        return ResponseEntity.ok(result);
    }
}
